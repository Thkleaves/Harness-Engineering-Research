# Working on multiple tasks at once

The harness lets you keep several tasks **open in one workspace** at the same
time, with exactly one **focused** task. This removes the old friction where
starting a second task silently clobbered the first one's active pointer and
tripped the completion/permission gates on the wrong task.

## Model

- **Registry** — `.harness/state/active-tasks.json`:
  ```json
  { "schemaVersion": 1, "focused": "feature-b", "open": ["feature-a", "feature-b"] }
  ```
- **Focused task** — the one all single-task gates act on. The registry mirrors
  the focused id into the legacy `.harness/state/active-task.txt`, so the
  skill/task permission guard, the Stop completion gate, and the advisor
  runtime-proof all keep working unchanged — they just follow the focused task.
- **Open set** — every task you are currently juggling. Opening another task
  **adds** to this set; it never drops the task you were on.

Each task remains a first-class citizen keyed by id: its contract
(`.harness/task-contracts/<id>.json`), evidence (`.harness/evidence/<id>.json`),
and reviews (`.harness/reviews/<id>/`) are independent. The registry only tracks
*which* tasks are in flight and *which* one is focused.

## CLI

```bash
node .harness/scripts/active-tasks.mjs list            # show open tasks; * marks focused
node .harness/scripts/active-tasks.mjs open  <id>      # add to the open set + focus it
node .harness/scripts/active-tasks.mjs focus <id>      # focus a task (alias: switch); auto-opens
node .harness/scripts/active-tasks.mjs close <id>      # stop juggling it (refocus the next survivor)
node .harness/scripts/active-tasks.mjs focused         # print the focused id (for scripts)
node .harness/scripts/active-tasks.mjs clear           # forget every open task
```

`close` only stops *tracking* a task as in-flight — it does **not** mark it
complete. Completion is still the evidence-gated `passes: true` flow in
`.harness/feature_list.json`.

## Typical flow

```bash
# Start task A, do some work…
node .harness/scripts/active-tasks.mjs open feature-a

# A blocker comes in — open B without losing A
node .harness/scripts/active-tasks.mjs open feature-b
# …work on B, finish it through the normal evidence flow…
node .harness/scripts/active-tasks.mjs close feature-b

# Back to A (still exactly where you left it)
node .harness/scripts/active-tasks.mjs switch feature-a
```

`SessionStart` prints the open-task landscape so a resumed session sees the
whole set, not just one suggestion.

## Notes

- **Backward compatible.** A project that only has the old
  `.harness/state/active-task.txt` is treated as one open + focused task; the
  registry materializes the first time you run a command.
- **Gates are per-task and follow the focus.** Claiming "done" gates the focused
  task only; other open tasks are never required to be complete for the Stop
  hook to pass.
- **Permissions.** Each task contract's `permissions.allow` still applies to the
  focused task. (With the guard in its default warn mode, out-of-policy calls
  are logged to `.harness/bypass.log` rather than blocked — see
  `permission-model.md`.)
- **True parallelism.** For literally simultaneous work across multiple agents,
  give each task its own git worktree via
  `.harness/scripts/prepare-session-worktree.mjs --task=<id>`; each worktree
  carries its own focused task.
