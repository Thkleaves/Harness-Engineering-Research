#!/usr/bin/env bash
# SubagentStop hook — fires when a subagent finishes its turn (Task tool).
# Triggers the same structural-test that PostToolUse(Edit) runs, because a
# subagent can edit files in batches that individually pass but jointly drift
# off-layer. Running the check at subagent boundary catches that drift early.
#
# Contract:
#   - Never blocks (exit 0 even on failure — the parent Stop hook handles the
#     final gate). We only emit a stderr summary that Claude reads.
#   - Telemetry append to .harness/telemetry.jsonl as {event:"subagent_stop"}.
#   - For the mandatory advisor, writes a runtime proof to
#     .harness/state/advisor-runs/<taskId>.jsonl. The stopping subagent is
#     identified by its agent type (Claude Code provides agent_type; Codex/Kiro
#     use subagent_type/subagent), with a transcript-signature fallback. Reading
#     the wrong identity field previously deadlocked the advisor gate (issue #32),
#     so this must keep reading agent_type. The JS heredoc below is deliberately
#     comment-free: stray quotes/parens/backticks break the bash $(...) parser.
#   - Skipped when .harness/config.json#structuralTest.engine === "none" (the
#     "structural test not yet wired" escape hatch used by polyglot scaffolds).
set -eo pipefail

INPUT=$(cat)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
_LIB_DIR="$SCRIPT_DIR/_lib"
. "$_LIB_DIR/jp.sh"
. "$_LIB_DIR/telemetry.sh"

TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
SHA=$(git rev-parse --short HEAD 2>/dev/null || echo 'no-git')
SUBAGENT="(unknown)"
LINE=""

if command -v node >/dev/null 2>&1; then
  META=$(AHK_SUBAGENT_TS="$TS" AHK_SUBAGENT_SHA="$SHA" node - "$INPUT" <<'NODE' 2>/dev/null || true
const fs = require("node:fs");
const crypto = require("node:crypto");
const path = require("node:path");

const input = process.argv[2] || "";
let payload = {};
try {
  payload = JSON.parse(input);
} catch {
  payload = {};
}

  function firstString(values, fallback = "") {
    for (const value of values) {
      if (typeof value === "string" && value.trim()) return value.trim();
    }
    return fallback;
  }

  function advisorRoleValue(value) {
    return String(value || "").trim().toLowerCase() === "advisor";
  }

function activeTaskFromState() {
  try {
    return fs.readFileSync(".harness/state/active-task.txt", "utf8").split(/\r?\n/)[0].trim();
  } catch {
    return "";
  }
}

function stableTaskId(value) {
  return /^[A-Za-z0-9._-]+$/.test(String(value || ""));
}

function transcriptMentionsAdvisor(transcriptPath) {
  try {
    if (!transcriptPath) return false;
    const raw = fs.readFileSync(transcriptPath, "utf8").replace(/\s+/g, "");
    return raw.includes('"agent_type":"advisor"')
      || raw.includes('"subagent_type":"advisor"')
      || raw.includes('"name":"advisor"');
  } catch {
    return false;
  }
}

  const advisorCandidates = [
    payload.agent_type,
    payload.agentType,
    payload.subagent_type,
    payload.subagentType,
    payload.agent_name,
    payload.agentName,
    typeof payload.agent === "string" ? payload.agent : "",
    typeof payload.subagent === "string" ? payload.subagent : "",
    payload.agent?.type,
    payload.agent?.name,
    payload.agent?.role,
    payload.agent?.agent_type,
    payload.agent?.agentType,
    payload.subagent?.type,
    payload.subagent?.name,
    payload.subagent?.role,
    payload.subagent?.agent_type,
    payload.subagent?.agentType,
    payload.tool_input?.agent_type,
    payload.tool_input?.agentType,
    payload.tool_input?.subagent_type,
    payload.tool_input?.subagentType,
    payload.tool_input?.agent_name,
    payload.tool_input?.agentName,
    payload.tool_input?.agent?.type,
    payload.tool_input?.agent?.name,
    payload.tool_input?.agent?.role,
    payload.metadata?.agent_type,
    payload.metadata?.agentType,
    payload.metadata?.subagent_type,
    payload.metadata?.subagentType,
    payload.metadata?.agent_name,
    payload.metadata?.agentName,
    payload.metadata?.name,
    payload.metadata?.role,
    payload.agent_metadata?.agent_type,
    payload.agent_metadata?.agentType,
    payload.agent_metadata?.name,
    payload.agent_metadata?.role,
    payload.subagent_metadata?.subagent_type,
    payload.subagent_metadata?.subagentType,
    payload.subagent_metadata?.name,
    payload.subagent_metadata?.role,
  ];
  const advisorRole = firstString(advisorCandidates.filter(advisorRoleValue));
  const subagent = firstString([
    advisorRole,
    typeof payload.subagent === "string" ? payload.subagent : "",
    typeof payload.agent === "string" ? payload.agent : "",
    payload.agent?.id,
    payload.agent?.agent_id,
    payload.agent?.agentId,
    payload.subagent?.id,
    payload.subagent?.agent_id,
    payload.subagent?.agentId,
    payload.agent_id,
    payload.agentId,
    payload.session_id,
    payload.sessionId,
  ], "unknown");
  const transcriptPath = firstString([payload.transcript_path, payload.transcriptPath]);
  const isAdvisor = Boolean(advisorRole) || transcriptMentionsAdvisor(transcriptPath);
const taskId = firstString([
  process.env.AHK_ACTIVE_TASK,
  payload.taskId,
  payload.task_id,
  payload.activeTask,
  payload.active_task,
  payload.tool_input?.taskId,
  payload.tool_input?.activeTask,
  activeTaskFromState(),
]);
const sessionId = firstString([payload.session_id, payload.sessionId]);
const inputHash = `sha256:${crypto.createHash("sha256").update(input).digest("hex")}`;
const eventId = `${Date.now().toString(36)}-${inputHash.slice(7, 19)}`;
const base = {
  schemaVersion: 1,
  ts: process.env.AHK_SUBAGENT_TS || new Date().toISOString(),
  source: "SubagentStop",
  subagent,
  taskId: taskId || undefined,
  session_id: sessionId || undefined,
  eventId,
  inputHash,
  sha: process.env.AHK_SUBAGENT_SHA || "no-git",
};
for (const key of Object.keys(base)) {
  if (base[key] === undefined || base[key] === "") delete base[key];
}

let proofPath = "";
let advisorUnattributed = false;
if (isAdvisor && stableTaskId(taskId)) {
  const relPath = `.harness/state/advisor-runs/${taskId}.jsonl`;
  proofPath = relPath;
  fs.mkdirSync(path.dirname(relPath), { recursive: true });
  fs.appendFileSync(
    relPath,
    `${JSON.stringify({ ...base, subagent: "advisor", event: "advisor_subagent_stop", proofPath: relPath })}\n`,
  );
} else if (isAdvisor) {
  advisorUnattributed = true;
}

process.stdout.write(JSON.stringify({
  subagent,
  taskId,
  eventId,
  inputHash,
  proofPath,
  advisorUnattributed,
  telemetryLine: JSON.stringify({ ...base, event: "subagent_stop" }),
}));
NODE
)
  if [ -n "$META" ] && have_jp; then
    SUBAGENT=$(printf '%s' "$META" | jp '.subagent // "unknown"' 2>/dev/null || echo "unknown")
    LINE=$(printf '%s' "$META" | jp '.telemetryLine // empty' 2>/dev/null || true)
    ADVISOR_UNATTRIBUTED=$(printf '%s' "$META" | jp '.advisorUnattributed // false' 2>/dev/null || echo "false")
  fi
elif have_jp; then
  SUBAGENT=$(echo "$INPUT" | jp '.agent_type // .subagent_type // .subagent // .session_id // "unknown"' 2>/dev/null || echo "unknown")
fi

# Telemetry first so we record every subagent boundary, even if the
# structural-test bails below. telemetry_append handles rotation.
if [ -z "$LINE" ]; then
  LINE=$(printf '{"schemaVersion":1,"ts":"%s","event":"subagent_stop","source":"SubagentStop","subagent":"%s","sha":"%s"}' \
    "$TS" "$SUBAGENT" "$SHA")
fi
telemetry_append "$LINE"

# Fail loudly when an advisor subagent stopped without a stable active task id:
# the mandatory-advisor proof could not be written, so the parent Stop gate will
# block on a proof it can never find. Surface it instead of a silent deadlock
# (issue #32).
if [ "${ADVISOR_UNATTRIBUTED:-false}" = "true" ]; then
  echo "[ahk] subagent_stop: advisor finished but no stable active task id was set (.harness/state/active-task.txt is empty and AHK_ACTIVE_TASK is unset). The advisor runtime proof was NOT written, so the mandatory-advisor gate will block. Set the active task before spawning the advisor subagent." >&2
  telemetry_append "$(printf '{"schemaVersion":1,"ts":"%s","event":"advisor_subagent_stop_unattributed","source":"SubagentStop","sha":"%s"}' "$TS" "$SHA")"
fi

# Skip if structural test disabled.
if [ -f .harness/config.json ] \
   && grep -qE '"engine"[[:space:]]*:[[:space:]]*"none"' .harness/config.json; then
  exit 0
fi

# AHK_HOOK_MODE=warn → log only, don't run.
if [ "${AHK_HOOK_MODE:-}" = "warn" ]; then
  exit 0
fi

# Run structural test workspace-wide. Subagents typically touch multiple
# files; per-file scoping would miss the cross-file drift case. Cap output
# to 30 lines on stderr so the parent agent sees the summary without flood.
RAN=0
if [ -f .harness/runners/structural-check.mjs ] && command -v node >/dev/null 2>&1; then
  RAN=1
  if ! node .harness/runners/structural-check.mjs 2>&1 | tail -30 >&2; then
    echo "[ahk] subagent_stop: structural-test reported violations (see above). Continuing — parent Stop hook will gate." >&2
  fi
elif [ -f .harness/runners/structural_check.go ] && command -v go >/dev/null 2>&1; then
  RAN=1
  if ! go run .harness/runners/structural_check.go 2>&1 | tail -30 >&2; then
    echo "[ahk] subagent_stop: structural-test reported violations (see above). Continuing — parent Stop hook will gate." >&2
  fi
elif [ -f .harness/runners/structural_test.py ] && command -v python >/dev/null 2>&1; then
  RAN=1
  if ! python .harness/runners/structural_test.py 2>&1 | tail -30 >&2; then
    echo "[ahk] subagent_stop: structural-test reported violations (see above). Continuing — parent Stop hook will gate." >&2
  fi
elif command -v npm >/dev/null 2>&1 && [ -f package.json ] \
     && grep -q '"harness:check"' package.json 2>/dev/null; then
  RAN=1
  if ! npm run --silent harness:check 2>&1 | tail -30 >&2; then
    echo "[ahk] subagent_stop: structural-test reported violations (see above). Continuing — parent Stop hook will gate." >&2
  fi
fi
if [ "$RAN" = "0" ]; then
  # No structural-test entry point. Skip silently — already logged in telemetry.
  exit 0
fi
exit 0
