# Architecture — 03-book-api-kit-review

This document is the source of truth for how code is organized. Any deviation
must be justified in an ADR under `.harness/docs/adr/`.

## Layer order (forward-only)

```
types → config → repo → service → runtime → ui
```

Code in a higher layer may import from any lower layer. Code in a lower layer
**must not** import from a higher layer. The structural test enforces this
mechanically — see `.harness/config.json` and the
`npm run harness:check` command.

Existing violations may be grandfathered in
`.harness/structural-baseline.json`, but that file is debt, not permission.
Run `node .harness/scripts/check-structural-baseline.mjs` to verify the
baseline is well-formed and has not grown versus `HEAD`.

## Layer responsibilities

| Layer       | Responsibility                                                              |
| ----------- | --------------------------------------------------------------------------- |
| `types`     | Pure data shapes. No I/O, no business logic, no framework imports.          |
| `config`    | Static configuration (env loading, feature flags, constants).               |
| `repo`      | Persistence and external-system gateways. Returns plain values.             |
| `service`   | Business logic. Orchestrates `repo` calls. Pure where possible.             |
| `runtime`   | Framework adapters: HTTP routes, CLI commands, queue handlers.              |
| `ui`        | Rendering, components, presentation logic.                                  |

## Cross-cutting concerns: `providers/`

Auth, telemetry, feature flags, observability — anything that would otherwise
cut across layers — enters through `providers/`. Each provider exposes a
single typed interface; consumers depend on the interface, not the
implementation.

## Governance rules

For TypeScript projects, the structural test also enforces selected
`structuralTest.rules` from `.harness/config.json`:

- `no-raw-env-outside-config`: read environment variables in `config`, then
  pass typed values inward.
- `no-db-in-ui`: UI code must not import repo-layer modules or database client
  packages directly.
- `no-provider-bypass`: auth, telemetry, and feature-flag SDKs must be imported
  only from provider boundary modules.
- `no-dynamic-import-in-layered-code`: optional strict mode for projects that
  want every dependency visible to static analysis.

## Adding a new module

1. Decide which layers it touches.
2. Run `/inspect-module <existing-similar-module>` to mirror the pattern.
3. Create files under `src/{domain}/{layer}/`.
4. Write tests in the same layer.
5. Run the structural test. If it fails, do **not** disable it — fix the import.

## Recent decisions

(Most recent first. Created automatically by `/add-adr`.)

- `0001-use-agent-harness-kit.md` — Adopt agent-harness-kit as the harness layer.
