# Skill Permission Model

The kit installs `.harness/permissions.json` and a PreToolUse guard that can
enforce per-skill tool policy.

## Policy Shape

```json
{
  "version": 1,
  "default": {
    "allow": ["Read", "Grep", "Glob", "LS"],
    "deny": ["Bash(git push*)"]
  },
  "skills": {
    "orchestrate": {
      "allow": ["Read", "Bash(node .claude/skills/orchestrate/orchestrate.mjs*)"],
      "deny": ["Write", "Edit", "MultiEdit"]
    }
  }
}
```

## Runtime Behavior

- If no active skill is known, the guard stays silent unless
  `.harness/config.json#taskContracts.requireActiveTaskForMutationTargets` is
  enabled and the tool is trying to edit source or technical config files.
- If an active task is known, the guard also reads
  `.harness/task-contracts/<task-id>.json` and applies the task's
  `permissions` policy on top of the skill policy.
- When enforcing, if an active skill has a matching deny rule, the guard returns
  a Claude `permissionDecision: "deny"` response.
- When enforcing, if an active skill or active task has an allow list and the
  tool does not match it, the guard denies the call.
- If the active skill has no policy entry, the guard applies shared default
  denies such as `Bash(git push*)` but does not clamp the skill to the read-only
  default allow list. This prevents stale or external skill names from bricking
  orchestration tools while preserving dangerous-command blocks.
- **The kit ships `AHK_SKILL_PERMISSIONS_MODE=warn` as the default** (set in the
  generated `.claude/settings.json` `env` and the Codex hook wrapper): instead of
  blocking, the guard logs every out-of-policy tool call to `.harness/bypass.log`,
  so the permission matrix is advisory and stays out of the way during normal
  coding while still leaving an audit trail. To make it **block**, set
  `AHK_SKILL_PERMISSIONS_MODE=enforce` in `.claude/settings.json` `env` (or remove
  the key, or export it in your shell). `AHK_ALLOW_BYPASS=1` remains a separate
  one-off override for explicit cases.
  `node .harness/scripts/check-bypass-audit.mjs` fails release readiness until
  every bypass fingerprint is acknowledged in `.harness/bypass-audit.json`.
  In strict release gates, `node .harness/scripts/check-bypass-audit.mjs
  --strict` also accepts approved, unexpired requests under
  `.harness/bypass-requests/*.json`, but only for the declared scope.

The guard reads `AHK_ACTIVE_SKILL` first, then explicit active-skill fields in
the hook payload. It does not treat `skill_invoked` telemetry as active state:
those rows are emitted by a PostToolUse hook after the skill call has returned,
so using them as active context would make a finished skill leak into unrelated
future tool calls. Future active-skill telemetry must use bounded events such as
`skill_started`/`skill_active` and a later completion event.

The guard reads the active task from `AHK_ACTIVE_TASK`,
`AHK_ACTIVE_TASK_ID`, `task_id`/`taskId` fields in the hook payload, or recent
telemetry rows for the same session. If a high-risk task is active but its
contract does not declare permissions, read-only tools can still run but
mutating tools are denied until the contract is made explicit.
Generated projects enable `requireActiveTaskForMutationTargets` by default:
`Edit`, `Write`, and `MultiEdit` calls against configured source roots or
technical config files such as package manifests, lockfiles, CI workflows,
runtime settings, Docker files, and env samples require an active task
contract with `permissions.allow`. Harness proof artifacts under
`.harness/task-contracts`, `.harness/evidence`, `.harness/reviews`,
`.harness/state`, and `.harness/failures/records` are exempt so an agent can
create the contract/evidence trail before touching implementation code.
Active task ids must be stable lowercase ids, and
`.harness/config.json#taskContracts.contractsDir` must stay inside the project
root. The guard also rejects a task contract whose `id` does not match the
active task id, so a corrupted active task state cannot silently apply the
wrong permission contract.

Session isolation is the next blast-radius gate. `node
.harness/scripts/check-session-isolation.mjs --strict` reads the same active
task id and fails when a high-risk or mutating contract runs on a protected
branch, on a branch outside the configured `agent/` or `codex/` prefixes, or
outside a linked git worktree. The permission guard controls the tool call;
the session-isolation gate controls where that tool call is allowed to happen.
Use `node .harness/scripts/prepare-session-worktree.mjs --task <id>` to create
that linked worktree from the contract, write `.harness/active-task.json` plus
`.harness/active-task.env`, write the session manifest under
`.harness/sessions/` in the new worktree, and best-effort record the worktree in
operational state. The checker also warns about stale manifests and generated
worktrees that no longer have a session manifest.

## Skill Contracts

Each skill directory now carries a `skill.json` contract:

```json
{
  "schemaVersion": 1,
  "id": "orchestrate",
  "name": "orchestrate",
  "version": "1.0.0",
  "capabilities": ["orchestration", "workflow"],
  "permissions": {
    "allow": ["Read", "Bash(node .claude/skills/orchestrate/orchestrate.mjs*)"],
    "deny": ["Write", "Edit", "MultiEdit"]
  }
}
```

`.harness/skill-registry.json` is the install-time registry. Run
`node .harness/scripts/check-skill-contracts.mjs` in an installed project, or
`npm run check:skill-contracts` in this repo, to validate version/capability
metadata, explicit permission declarations, and drift between `.claude`,
`.agents`, and template surfaces.

`node .harness/scripts/permissions-compile.mjs --write` writes
`.harness/permissions.compiled.json`. Generated installs run this compiler
during render, then merge the compiler's Claude permission hints into
`.claude/settings.json`. Those settings hints are only the runtime upper bound
needed for Claude to reach the harness hooks; the PreToolUse guard still
enforces the narrower skill and task contract policy.

For skills with explicit permissions, `skill.json` is the source of truth:

- `.harness/permissions.json` must carry the same skill-specific allow/deny
  matrix.
- `SKILL.md` frontmatter `allowed-tools` must not grant anything outside the
  `skill.json` allow list.
- `SKILL.md` frontmatter must never grant a tool denied by `skill.json`.
- `skill.json` must not use overbroad sensitive Bash grants such as `Bash(*)`,
  `Bash(git*)`, `Bash(gh*)`, or `Bash(node*)`. Declare the exact command lane
  the skill needs instead, for example `Bash(git diff*)` or
  `Bash(gh workflow run*)`.

Shared default denies still apply at runtime. For example, `git commit*` and
`git push*` are blocked even when a skill's own `deny` array is empty.

`node .harness/scripts/harness-report.mjs` includes a `Skill permission health`
section that summarizes registry coverage, `.harness/permissions.json` drift,
mutation-capable skills, mutation-denied skills, and overbroad sensitive grants.
Use it as the dashboard view; add `--json --fail-on=fail` when automation needs
a structured status payload. Use `check-skill-contracts` as the blocking gate.

## Task Contracts

Task contracts can narrow permissions further:

```json
{
  "id": "checkout-coupon",
  "riskTier": "high-risk",
  "permissions": {
    "allow": [
      "Read",
      "Grep",
      "Glob",
      "LS",
      "Edit",
      "Write",
      "Bash(npm run*)",
      "Bash(git status*)",
      "Bash(git diff*)"
    ],
    "deny": ["Bash(git commit*)", "Bash(git push*)"]
  }
}
```

Deny rules always win. A non-empty task `allow` list is an additional
restriction, not a replacement for skill policy. High-risk contracts must have
a non-empty task `permissions.allow` list and must not use wildcard access such
as `*` or `Bash(*)`, or overbroad sensitive Bash grants such as `Bash(git*)`.
Claude-style colon wildcards are normalized first, so `Bash(git:*)` is treated
as `Bash(git*)`.

For Codex and other runtimes that surface ordinary shell commands as `Bash`,
the guard maps `Read`, `Grep`, `Glob`, and `LS` onto a deliberately small
read-only command lane (`cat`, `sed -n`, `rg`, `rg --files`, safe `find`,
`git show`, `git diff`, `git blame`, and similar inspection commands). The same
single-command validation applies to explicit `Bash(...)` wildcards, so
`Bash(git status*)` does not allow `git status --short && <another command>`.
Installation and dependency-changing commands such as `npm install` require an
explicit task permission and are not treated as read-only recovery commands.

If a high-risk active task is missing non-wildcard `permissions.allow` or
`scope.allowedLayers`, the PreToolUse guard allows read-only inspection
(`Read`, `Grep`, `Glob`, `LS`, `git status`, `git diff`) but blocks mutating
tools until the task contract is narrowed.

Task contracts can also narrow mutation scope with `scope.allowedLayers`:

```json
{
  "id": "checkout-coupon",
  "scope": {
    "summary": "Add coupon validation to checkout runtime only",
    "allowedLayers": ["runtime", "service"]
  },
  "permissions": {
    "allow": ["Read", "Edit", "Write", "Bash(npm run*)"],
    "deny": ["Bash(git push*)"]
  }
}
```

When an active task has `scope.allowedLayers`, the PreToolUse guard blocks
`Edit`, `Write`, and `MultiEdit` calls that target a different configured
source layer under `.harness/config.json#domains`. Files outside configured
source roots, such as docs and reports, are not blocked by this layer-scope
rule.

The evidence checker applies the same scope after implementation: if an
evidence bundle lists `changedFiles` under a configured source root, those
files must belong to `scope.allowedLayers`. This catches hand-written evidence
or bypassed tool calls before a feature can move to `passes: true`.

Recovery tools are exempt from task allow-list narrowing so a stale active task
cannot trap the agent. The guard still blocks unsafe commands, but allows the
task evidence checker, evidence attestation checker, `explain`, advisor
invocation, read-only code-review graph inspection, terminal readback, and
mutations that only touch harness proof artifacts such as `.harness/evidence/`
or `.harness/reviews/`.
