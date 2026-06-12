# 03-book-api-kit-review — Agent Working Notes

03-book-api-kit-review — solo-dev project on the agent-harness-kit harness. generic/generic project. Single-developer hobby
project. This file is intentionally short — it is a **table of contents**, not
an encyclopedia.

## Build & Run

- Install:    `npm install`
- Dev:        `npm run dev`
- Test:       `npm test`
- Lint:       `npm run lint`
- Structural: `npm run harness:check` (must pass before any PR)
- Architecture fitness: `node .harness/scripts/check-architecture-fitness.mjs --strict` (domain invariant plugin gate)
- Readiness: `node .harness/scripts/harness-readiness.mjs --strict` (release/pre-merge gate)

## Architecture (brief)

Layer order, enforced mechanically:

**types → config → repo → service → runtime → ui** — code may only depend forward. Cross-cutting concerns
enter via `providers/`.

Full diagram and rationale: `.harness/docs/architecture.md`.

## Golden principles (must hold)

1. Prefer shared utilities in `src/shared/` over new helpers.
2. Validate at boundaries; never probe data shape "YOLO-style".
3. Each test is end-to-end through one feature in `.harness/feature_list.json`.

Full list: `.harness/docs/golden-principles.md`.

## Where to look (read on demand)

The lines below use Claude Code 2.1+ `@`-imports — Claude loads the file
into context only when this section is referenced, keeping the working
CLAUDE.md tiny.

- @.harness/docs/architecture.md      — when adding a new module or moving code.
- @.harness/docs/adr/                 — when changing public APIs.
- @.harness/docs/golden-principles.md — before any refactor.
- @.harness/docs/context-rules.md      — when choosing what to read for a task phase/risk lane.
- @.harness/docs/operational-state.md  — before recording/querying intake, stories, backlog, or traces.
- @.harness/docs/trace-quality.md      — before final trace/friction recording.
- @.harness/feature_list.json         — before claiming a feature is done.
- @.harness/project/state.json        — before changing phase, MVP scope, risks, or checklists.
- `.harness/memory/current-summary.md` — compact shared project memory injected by SessionStart.
- `.harness/PROGRESS.md`     — read at session start; append at session end (kit-managed, not @-imported).

## Skills you should use

- `/inspect-module <path>`            when you need to understand existing code.
- `/context-query <question>`         before editing unfamiliar code or when the right files are not obvious; fallback command: `node .harness/scripts/context-query.mjs "<question>" --scope . --lane normal --json`.
- `/add-feature <description>`        when adding new capability — never freestyle.
- `/structural-test-author <layer>`   when adding a new structural rule.
- Add `.harness/fitness/rules/<rule-id>.json` when a domain invariant should be enforced mechanically.
- `/garbage-collection`               every Friday or before tagging a release.
- `/eval-runner`                      before merging any change to a skill or agent file.
- `/deliver-html`                     when user wants an analysis / audit / plan / decision doc / next-actions report — HTML for humans, MD stays for agent files (principle #11).
- `/remember-project`                 when a decision, risk, scope change, or handoff note must survive future sessions.
- `/project-status`                   when the user needs a phase/MVP/checklist/risk/status dashboard.

## Subagents you should delegate to (do NOT inline these reviews)

- `architecture-reviewer` — for any cross-layer change.
- `security-reviewer`     — for any auth, input handling, or secret-touching change.
- `reliability-reviewer`  — for any new error path, retry loop, or async boundary.

## Mandatory Advisor Protocol

An advisor agent exists in this project. It uses a higher-capability model
and MUST be consulted before:

1. Claiming any feature is done (before setting `passes: true`)
2. Any mutation touching auth, secrets, or trust boundaries
3. Any cross-layer architectural change
4. Any new public API surface

You CANNOT skip the advisor. The precompletion hook will block completion
if no advisor decision artifact exists for the active task, or if that artifact
  is not backed by an advisor runtime proof.

The advisor returns a structured JSON decision matching
`.harness/schemas/review-decision.schema.json`. If `decision != "pass"`,
you must address its findings before proceeding.

Invoke the advisor through a real runtime path. Native subagent runtimes should
use an actual advisor subagent and copy the matching `SubagentStop` proof row
from `.harness/state/advisor-runs/<taskId>.jsonl` into
`provenance.runtimeProof`. Codex runtimes may use the harness-owned
`.harness/scripts/codex-advisor-run.mjs` bridge, which persists both the advisor
decision and the matching runtime proof. Do not inline the advisor review in the
main agent.

## Workflow contract

1. Start session: run `/inspect-module .`, read `.harness/PROGRESS.md`, and keep `.harness/project/state.json` aligned with the active work.
2. Focus a feature from `.harness/feature_list.json` whose `passes: false`. Juggling several at once is fine — track the open set with `.harness/scripts/active-tasks.mjs` (`open`/`focus`/`close`/`list`); gates always apply to the focused task, and opening another never drops the first.
3. Implement. Run the structural test. If it fails, FIX before continuing.
4. Self-verify with the matching reviewer subagent(s); required reviewers must leave structured decisions under `.harness/reviews/<feature_id>/`.
5. Write `.harness/evidence/<feature_id>.json` with structural/test/smoke proof and reviewer decision links.
6. Ensure passing reviewer decisions include `checkedInvariants`, `diffCoverage`, `confidence`, and no `unreviewedRiskAreas`.
7. Record operational trace/friction with `.harness/scripts/harness-state.mjs trace` when the task changes code, docs, or harness state.
8. Update `.harness/feature_list.json` (`passes: true`) **only after** end-to-end proof and evidence pass. Append `.harness/PROGRESS.md`; stop commit-ready unless the user explicitly asks you to commit.

## What NOT to do

- Don't add a new layer without an ADR.
- Don't npm install packages with native bindings without an ADR.
- Don't disable the structural test to make a PR pass.
- Don't write code that the structural test cannot reason about (no dynamic
  imports across layers).
- Don't update CLAUDE.md without proposing a harness improvement
  (`/propose-harness-improvement`).
- Don't grow CLAUDE.md past 200 instructions — Stop hook blocks the stop on
  overflow (HumanLayer measurement). Excess belongs in `.harness/docs/` or @-imports.
