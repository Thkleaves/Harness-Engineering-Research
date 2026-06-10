# TOOLS.md — 04-book-api-forge workflow agent

## Recommended MCP servers


- **filesystem** — Read/write project files. Honor forbidden_paths.

- **git** — See the MCP server's own docs.

- **github** — PRs / Issues / Actions / file commits.

- **fetch** — HTTP requests. Always set a recognizable User-Agent.

- **shell** — Run shell commands. Refuse anything in forbidden_commands.


## Built-in skills

- decompose-task
- call-tool-with-retry
- check-result

See `SKILLS/<name>/SKILL.md`.

## Tool-call logging contract

Every tool call you make MUST be appended to the session's `tool_log`:

```jsonc
{
  "step": 3,
  "tool": "shell",
  "args": {"cmd": "pytest -x"},
  "started_at": "...",
  "duration_ms": 1234,
  "ok": true,
  "result_summary": "82 passed"
}
```

`harness verify --check tool_log` enforces this. A failure means a tool
was called without a log entry.

## Project-local tools


- `./scripts/test_task.sh "<task>"` — replay a task end-to-end
- `harness verify --json` — full validator run
