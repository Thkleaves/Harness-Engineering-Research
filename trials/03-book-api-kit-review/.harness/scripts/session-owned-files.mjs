#!/usr/bin/env node
import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";

const ROOT = process.env.CLAUDE_PROJECT_DIR || process.env.CODEX_PROJECT_DIR || process.cwd();
const STATE_PATH = resolve(ROOT, ".harness/state/session-owned-files.json");
const MUTATING_TOOLS = new Set(["Edit", "Write", "MultiEdit", "apply_patch"]);

function readStdin() {
  return new Promise((resolveRead) => {
    let input = "";
    process.stdin.setEncoding("utf8");
    process.stdin.on("data", (chunk) => {
      input += chunk;
    });
    process.stdin.on("end", () => resolveRead(input));
  });
}

function parseJson(text, fallback = null) {
  try {
    return JSON.parse(text);
  } catch {
    return fallback;
  }
}

function insideRoot(path) {
  const root = ROOT.replace(/\\/g, "/");
  const target = String(path || "").replace(/\\/g, "/");
  return target === root || target.startsWith(`${root}/`);
}

function relPath(path) {
  const raw = String(path || "").replace(/\\/g, "/");
  const root = ROOT.replace(/\\/g, "/");
  if (raw === root) return "";
  if (raw.startsWith(`${root}/`)) return raw.slice(root.length + 1);
  return raw.replace(/^\.\//, "");
}

function hasUrlScheme(value) {
  return /^[a-z][a-z0-9+.-]*:/i.test(String(value || ""));
}

function normalizedProjectPath(value) {
  const text = String(value || "").trim();
  if (!text || hasUrlScheme(text)) return "";
  const abs = resolve(ROOT, text);
  return insideRoot(abs) ? relPath(abs) : "";
}

function patchText(payload) {
  return (
    payload.tool_input?.command ||
    payload.tool_input?.patch ||
    payload.input?.command ||
    payload.input?.patch ||
    payload.command ||
    payload.patch ||
    ""
  );
}

function patchMutationTargets(payload) {
  return [
    ...String(patchText(payload)).matchAll(/^\*\*\* (?:Add|Update|Delete) File: (.+)$/gm),
    ...String(patchText(payload)).matchAll(/^\*\*\* Move to: (.+)$/gm),
  ]
    .map((match) => match[1]?.trim())
    .filter(Boolean);
}

function mutationTargets(payload) {
  if (!MUTATING_TOOLS.has(payload.tool_name || "")) return [];
  const direct = (
    payload.tool_input?.file_path ||
    payload.tool_input?.path ||
    payload.tool_input?.file ||
    payload.input?.file_path ||
    payload.input?.path ||
    payload.input?.file ||
    payload.file_path ||
    payload.path ||
    ""
  );
  if (direct) return [direct];
  if ((payload.tool_name || "") === "apply_patch") return patchMutationTargets(payload);
  return [];
}

function activeTaskFromState() {
  try {
    return readFileSync(resolve(ROOT, ".harness/state/active-task.txt"), "utf8").trim();
  } catch {
    return "";
  }
}

function activeTask(payload) {
  return (
    process.env.AHK_ACTIVE_TASK ||
    process.env.AHK_ACTIVE_TASK_ID ||
    payload.active_task ||
    payload.activeTask ||
    payload.task_id ||
    payload.taskId ||
    payload.tool_input?.active_task ||
    payload.tool_input?.activeTask ||
    payload.tool_input?.task_id ||
    payload.tool_input?.taskId ||
    activeTaskFromState() ||
    ""
  );
}

function readState() {
  if (!existsSync(STATE_PATH)) {
    return {
      schemaVersion: 1,
      files: [],
      entries: [],
    };
  }
  const parsed = parseJson(readFileSync(STATE_PATH, "utf8"));
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return {
      schemaVersion: 1,
      files: [],
      entries: [],
    };
  }
  return {
    schemaVersion: 1,
    ...parsed,
    files: Array.isArray(parsed.files) ? parsed.files : [],
    entries: Array.isArray(parsed.entries) ? parsed.entries : [],
  };
}

function writeState(state) {
  mkdirSync(dirname(STATE_PATH), { recursive: true });
  writeFileSync(STATE_PATH, JSON.stringify(state, null, 2) + "\n");
}

const input = await readStdin();
const payload = parseJson(input, {});
const targets = mutationTargets(payload)
  .map(normalizedProjectPath)
  .filter(Boolean);

if (targets.length === 0) process.exit(0);

const taskId = activeTask(payload);
const sessionId = payload.session_id || payload.sessionId || "";
const ts = new Date().toISOString();
const state = readState();
const files = new Set(state.files);
const entriesByKey = new Map();

for (const entry of state.entries) {
  const file = normalizedProjectPath(entry?.file || entry?.path);
  if (!file) continue;
  const key = `${entry.taskId || ""}\0${file}`;
  entriesByKey.set(key, { ...entry, file });
}

for (const file of targets) {
  files.add(file);
  const key = `${taskId || ""}\0${file}`;
  entriesByKey.set(key, {
    file,
    taskId,
    sessionId,
    tool: payload.tool_name || "",
    updatedAt: ts,
  });
}

writeState({
  schemaVersion: 1,
  sessionId: sessionId || state.sessionId || "",
  taskId: taskId || state.taskId || "",
  updatedAt: ts,
  files: [...files].sort(),
  entries: [...entriesByKey.values()].sort((a, b) => {
    const left = `${a.taskId || ""}/${a.file || ""}`;
    const right = `${b.taskId || ""}/${b.file || ""}`;
    return left.localeCompare(right);
  }),
});
