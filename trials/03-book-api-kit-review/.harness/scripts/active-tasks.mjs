#!/usr/bin/env node
// active-tasks.mjs — manage the set of tasks you are juggling in one workspace.
//
// Why this exists: the harness gates (skill-permission guard, Stop completion
// gate, advisor proof) key off ONE "active task". Working on several tasks in a
// single session used to mean clobbering that single pointer — abandoning the
// previous task's evidence and tripping gates on the wrong task. This keeps a
// small registry of OPEN tasks plus one FOCUSED task, and mirrors the focused
// id back into the legacy `.harness/state/active-task.txt` so every existing
// single-task reader automatically operates on the focused task. Opening a
// second task no longer clobbers the first; switching is cheap.
//
// State: .harness/state/active-tasks.json = { schemaVersion, focused, open[] }
// Mirror: .harness/state/active-task.txt  = the focused task id (or empty)
//
// Usage:
//   node .harness/scripts/active-tasks.mjs list [--json]
//   node .harness/scripts/active-tasks.mjs open  <id>   # add to open set + focus
//   node .harness/scripts/active-tasks.mjs focus <id>   # focus (alias: switch); auto-opens
//   node .harness/scripts/active-tasks.mjs close <id>   # stop juggling it (refocus next)
//   node .harness/scripts/active-tasks.mjs focused      # print the focused id
//   node .harness/scripts/active-tasks.mjs clear         # forget every open task
//
// `close` only stops tracking a task as in-flight; it does NOT mark it complete.
// Completion is still the evidence-gated feature_list.json `passes: true` flow.

import { existsSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { resolve } from "node:path";
import { spawnSync } from "node:child_process";

function repoRoot() {
  if (process.env.CLAUDE_PROJECT_DIR) return resolve(process.env.CLAUDE_PROJECT_DIR);
  const r = spawnSync("git", ["rev-parse", "--show-toplevel"], { encoding: "utf8" });
  if (r.status === 0 && r.stdout.trim()) return r.stdout.trim();
  return process.cwd();
}

const ROOT = repoRoot();
const STATE_DIR = resolve(ROOT, ".harness/state");
const REGISTRY = resolve(STATE_DIR, "active-tasks.json");
const MIRROR = resolve(STATE_DIR, "active-task.txt");
const FEATURE_LIST = resolve(ROOT, ".harness/feature_list.json");
const STABLE_ID = /^[a-z0-9][a-z0-9._-]*$/;

function normalize({ focused, open }) {
  const seen = new Set();
  const dedup = [];
  for (const id of open) {
    if (typeof id === "string" && id && !seen.has(id)) {
      seen.add(id);
      dedup.push(id);
    }
  }
  // Focused must be one of the open tasks; otherwise fall back to the most
  // recently opened one, or null when nothing is open.
  const f = focused && seen.has(focused) ? focused : (dedup[dedup.length - 1] || null);
  return { focused: f, open: dedup };
}

function readRegistry() {
  if (existsSync(REGISTRY)) {
    try {
      const doc = JSON.parse(readFileSync(REGISTRY, "utf8"));
      const open = Array.isArray(doc.open) ? doc.open : [];
      const focused = typeof doc.focused === "string" && doc.focused ? doc.focused : null;
      return normalize({ focused, open });
    } catch {
      // Corrupt registry — fall through to the legacy single-task pointer.
    }
  }
  // Backward-compat: a project that only has the single-task pointer is treated
  // as exactly one open + focused task.
  const legacy = existsSync(MIRROR) ? readFileSync(MIRROR, "utf8").split(/\r?\n/)[0].trim() : "";
  return legacy ? { focused: legacy, open: [legacy] } : { focused: null, open: [] };
}

function writeRegistry(reg) {
  const norm = normalize(reg);
  mkdirSync(STATE_DIR, { recursive: true });
  writeFileSync(
    REGISTRY,
    `${JSON.stringify({ schemaVersion: 1, focused: norm.focused, open: norm.open }, null, 2)}\n`,
  );
  // Mirror focused → active-task.txt so the permission guard, Stop completion
  // gate, and advisor proof all act on the focused task with no code changes.
  writeFileSync(MIRROR, norm.focused ? `${norm.focused}\n` : "");
  return norm;
}

function featureIndex() {
  if (!existsSync(FEATURE_LIST)) return new Map();
  try {
    const doc = JSON.parse(readFileSync(FEATURE_LIST, "utf8"));
    const features = Array.isArray(doc) ? doc : (Array.isArray(doc?.features) ? doc.features : []);
    return new Map(features.map((f) => [String(f?.id || ""), f]));
  } catch {
    return new Map();
  }
}

function validId(id, warnings) {
  if (!STABLE_ID.test(id)) {
    fail(`"${id}" is not a stable lowercase task id (expected ${STABLE_ID}).`);
  }
  const feats = featureIndex();
  if (feats.size > 0 && !feats.has(id)) {
    warnings.push(`note: "${id}" is not in .harness/feature_list.json yet — add it there so the gates can find its contract/evidence.`);
  }
}

function fail(message) {
  process.stderr.write(`active-tasks: ${message}\n`);
  process.exit(1);
}

function renderList(reg, asJson) {
  if (asJson) {
    process.stdout.write(`${JSON.stringify(reg)}\n`);
    return;
  }
  if (reg.open.length === 0) {
    process.stdout.write("active-tasks: no open tasks.\n");
    return;
  }
  const feats = featureIndex();
  process.stdout.write(`active-tasks: ${reg.open.length} open (focused → ${reg.focused || "none"})\n`);
  for (const id of reg.open) {
    const f = feats.get(id);
    const marker = id === reg.focused ? "*" : " ";
    const state = f ? (f.passes === true ? "passes" : "open  ") : "no-feature";
    const title = f?.title ? ` — ${f.title}` : "";
    process.stdout.write(`  ${marker} [${state}] ${id}${title}\n`);
  }
}

function main() {
  const [cmd, ...rest] = process.argv.slice(2);
  const asJson = rest.includes("--json");
  const id = rest.find((a) => !a.startsWith("--")) || "";
  const reg = readRegistry();
  const warnings = [];

  switch (cmd) {
    case undefined:
    case "list":
      renderList(reg, asJson);
      return;
    case "focused":
      process.stdout.write(reg.focused ? `${reg.focused}\n` : "");
      return;
    case "open": {
      if (!id) fail("usage: active-tasks.mjs open <id>");
      validId(id, warnings);
      const open = reg.open.includes(id) ? reg.open : [...reg.open, id];
      const next = writeRegistry({ focused: id, open });
      for (const w of warnings) process.stderr.write(`${w}\n`);
      process.stdout.write(`active-tasks: opened + focused "${id}" (${next.open.length} open).\n`);
      return;
    }
    case "focus":
    case "switch": {
      if (!id) fail(`usage: active-tasks.mjs ${cmd} <id>`);
      validId(id, warnings);
      // focus auto-opens so "switch to X" works even if X was never opened.
      const open = reg.open.includes(id) ? reg.open : [...reg.open, id];
      writeRegistry({ focused: id, open });
      for (const w of warnings) process.stderr.write(`${w}\n`);
      process.stdout.write(`active-tasks: focused "${id}".\n`);
      return;
    }
    case "close": {
      if (!id) fail("usage: active-tasks.mjs close <id>");
      if (!reg.open.includes(id)) {
        process.stdout.write(`active-tasks: "${id}" was not open; nothing to close.\n`);
        return;
      }
      const open = reg.open.filter((x) => x !== id);
      // If we closed the focused task, refocus the most recently opened survivor.
      const focused = reg.focused === id ? (open[open.length - 1] || null) : reg.focused;
      const next = writeRegistry({ focused, open });
      process.stdout.write(`active-tasks: closed "${id}" (focused → ${next.focused || "none"}, ${next.open.length} open).\n`);
      return;
    }
    case "clear": {
      writeRegistry({ focused: null, open: [] });
      process.stdout.write("active-tasks: cleared all open tasks.\n");
      return;
    }
    default:
      fail(`unknown command "${cmd}". Use: list | open | focus | close | focused | clear`);
  }
}

main();
