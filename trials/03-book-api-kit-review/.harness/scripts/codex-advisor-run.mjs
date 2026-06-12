#!/usr/bin/env node
import { createHash } from "node:crypto";
import { existsSync } from "node:fs";
import { mkdir, readFile, writeFile, appendFile } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import { spawnSync } from "node:child_process";

const DEFAULT_TIMEOUT_MS = 240_000;

function parseArgs(argv) {
  const opts = {
    cwd: process.cwd(),
    taskId: "",
    codex: process.env.AHK_CODEX_BINARY || "codex",
    timeoutMs: Number.parseInt(process.env.AHK_CODEX_ADVISOR_TIMEOUT_MS || "", 10) || DEFAULT_TIMEOUT_MS,
    json: false,
  };
  for (let idx = 0; idx < argv.length; idx += 1) {
    const arg = argv[idx];
    if (arg === "--task") opts.taskId = String(argv[++idx] || "");
    else if (arg.startsWith("--task=")) opts.taskId = arg.slice("--task=".length);
    else if (arg === "--cwd") opts.cwd = resolve(String(argv[++idx] || process.cwd()));
    else if (arg.startsWith("--cwd=")) opts.cwd = resolve(arg.slice("--cwd=".length));
    else if (arg === "--codex") opts.codex = String(argv[++idx] || opts.codex);
    else if (arg.startsWith("--codex=")) opts.codex = arg.slice("--codex=".length);
    else if (arg === "--timeout-ms") opts.timeoutMs = Number.parseInt(String(argv[++idx] || ""), 10);
    else if (arg.startsWith("--timeout-ms=")) opts.timeoutMs = Number.parseInt(arg.slice("--timeout-ms=".length), 10);
    else if (arg === "--json") opts.json = true;
  }
  return opts;
}

function sha256(value) {
  return `sha256:${createHash("sha256").update(String(value || "")).digest("hex")}`;
}

async function firstLine(path) {
  try {
    return (await readFile(path, "utf8")).split(/\r?\n/)[0].trim();
  } catch {
    return "";
  }
}

function stableTaskId(value) {
  return /^[A-Za-z0-9._-]+$/.test(String(value || ""));
}

function extractJsonDecision(text) {
  const raw = String(text || "");
  const fenced = raw.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const candidates = [];
  if (fenced) candidates.push(fenced[1]);
  const first = raw.indexOf("{");
  const last = raw.lastIndexOf("}");
  if (first >= 0 && last > first) candidates.push(raw.slice(first, last + 1));
  for (const candidate of candidates) {
    try {
      const parsed = JSON.parse(candidate.trim());
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) return parsed;
    } catch {
      // Try the next candidate.
    }
  }
  return null;
}

function fail(message, detail = "") {
  const output = { status: "fail", error: message, detail };
  console.error(JSON.stringify(output, null, 2));
  process.exit(1);
}

function gitSha(cwd) {
  const result = spawnSync("git", ["rev-parse", "--short", "HEAD"], { cwd, encoding: "utf8" });
  return result.status === 0 ? result.stdout.trim() : "no-git";
}

function buildPrompt({ taskId, advisorText }) {
  return [
    "Run the mandatory advisor review for this repository.",
    "Read and follow the Codex advisor definition below. Inspect only repository files and read-only git state.",
    `Active task: ${taskId}`,
    "",
    "Return exactly one fenced json block matching .harness/schemas/review-decision.schema.json.",
    "Do not create or modify files. The harness runner will persist the decision and runtime proof.",
    "",
    "Advisor definition:",
    advisorText,
  ].join("\n");
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  if (!Number.isInteger(opts.timeoutMs) || opts.timeoutMs < 1) fail("--timeout-ms must be a positive integer");

  const root = resolve(opts.cwd);
  const taskId = opts.taskId || await firstLine(join(root, ".harness/state/active-task.txt"));
  if (!stableTaskId(taskId)) {
    fail("Stable active task id is required", "Pass --task <taskId> or set .harness/state/active-task.txt.");
  }

  const advisorPath = ".codex/agents/advisor.toml";
  const advisorAbs = join(root, advisorPath);
  if (!existsSync(advisorAbs)) fail("Codex advisor definition is missing", advisorPath);

  const stateDir = join(root, ".harness/state");
  await mkdir(stateDir, { recursive: true });
  await writeFile(join(stateDir, "active-task.txt"), `${taskId}\n`);

  const advisorText = await readFile(advisorAbs, "utf8");
  const prompt = buildPrompt({ taskId, advisorText });
  const outFile = join(stateDir, `codex-advisor-${taskId}.out.txt`);
  const codexArgs = [
    "exec",
    "--ephemeral",
    "--sandbox",
    "read-only",
    "--config",
    'approval_policy="never"',
    "-C",
    root,
    "-o",
    outFile,
    prompt,
  ];
  const run = spawnSync(opts.codex, codexArgs, {
    cwd: root,
    encoding: "utf8",
    timeout: opts.timeoutMs,
  });
  if (run.status !== 0) {
    fail(
      `codex advisor run exited ${Number.isInteger(run.status) ? run.status : 1}`,
      `signal=${run.signal || ""}\nstderr=${(run.stderr || "").slice(-2000)}\nstdout=${(run.stdout || "").slice(-2000)}`,
    );
  }

  const outText = existsSync(outFile) ? await readFile(outFile, "utf8") : "";
  const combined = `${outText}\n${run.stdout || ""}`;
  const decision = extractJsonDecision(combined);
  if (!decision) fail("Codex advisor output did not contain a JSON decision", combined.slice(-2000));
  if (decision.reviewer !== "advisor") fail("Codex advisor decision reviewer must be advisor");
  if (decision.decision === "pass" && decision.taskId !== taskId) {
    fail("Passing advisor decision taskId must match the active task", `decision.taskId=${decision.taskId || ""} taskId=${taskId}`);
  }

  const ts = new Date().toISOString();
  const proofPath = `.harness/state/advisor-runs/${taskId}.jsonl`;
  const decisionPath = `.harness/reviews/${taskId}/advisor-decision.json`;
  const proofInput = JSON.stringify({
    taskId,
    advisorPath,
    promptHash: sha256(prompt),
    stdoutHash: sha256(run.stdout || ""),
    outputHash: sha256(outText),
    decision,
  });
  const inputHash = sha256(proofInput);
  const eventId = `codex-${Date.now().toString(36)}-${inputHash.slice(7, 19)}`;
  const runtimeProof = {
    type: "codex-advisor-run",
    eventId,
    inputHash,
    path: proofPath,
  };
  const boundDecision = {
    ...decision,
    schemaVersion: decision.schemaVersion ?? 1,
    taskId: decision.taskId || taskId,
    featureId: decision.featureId || decision.taskId || taskId,
    createdAt: decision.createdAt || ts,
    provenance: {
      ...(decision.provenance || {}),
      source: "advisor-agent",
      trigger: decision.provenance?.trigger || "claim-done",
      createdBy: "advisor",
      runtimeProof,
    },
  };
  const proofRecord = {
    schemaVersion: 1,
    ts,
    event: "codex_advisor_run",
    source: "CodexAdvisorRun",
    subagent: "advisor",
    taskId,
    eventId,
    inputHash,
    proofPath,
    decisionPath,
    advisorPath,
    sha: gitSha(root),
  };

  await mkdir(dirname(join(root, proofPath)), { recursive: true });
  await appendFile(join(root, proofPath), `${JSON.stringify(proofRecord)}\n`);
  await mkdir(dirname(join(root, decisionPath)), { recursive: true });
  await writeFile(join(root, decisionPath), `${JSON.stringify(boundDecision, null, 2)}\n`);

  const payload = { status: "pass", taskId, decisionPath, proofPath, eventId, inputHash };
  if (opts.json) console.log(JSON.stringify(payload, null, 2));
  else {
    console.log(`codex advisor run: ${boundDecision.decision}`);
    console.log(`decision: ${decisionPath}`);
    console.log(`runtime proof: ${proofPath} eventId=${eventId}`);
  }
}

main().catch((error) => fail(error.message || String(error), error.stack || ""));
