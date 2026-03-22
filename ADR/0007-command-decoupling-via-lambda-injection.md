# 7. Command decoupling via lambda injection

Date: 2026-03-22

## Status

Accepted

## Context

`TsCommand` needs several capabilities that are naturally owned by `MctsPlugin` or the Bukkit `Server` object:

- Triggering a plugin reload.
- Resolving a player's display name from their UUID (for `/ts who`).
- Finding an online player by name (for admin link).
- Building the TS advertisement message (for `/ts` with no args).

Passing `MctsPlugin` or `Server` directly into `TsCommand` would create a hard dependency on the Paper runtime, making unit tests impossible without a running Paper server. Mocking `Server` requires Mockito to cover a large, complex interface full of unrelated methods.

## Decision

Inject only the **specific operations** `TsCommand` needs, expressed as typed functional interfaces:

| Need | Injected type |
|---|---|
| Trigger reload | `Runnable reloadCallback` |
| Resolve name from UUID | `Function<UUID, String> playerNameResolver` |
| Find online player by name | `Function<String, Optional<Player>> onlinePlayerFinder` |
| Build advertisement message | `Supplier<String> advertisementMessageSupplier` |
| Read current config values | `PluginConfig config` (data object, no behaviour) |

`MctsPlugin` wires these up at construction time using method references and lambdas (`this::reload`, `getServer()::getPlayerExact`, etc.).

## Consequences

- `TsCommand` has no dependency on `MctsPlugin` or `Server` — it can be fully unit-tested with `@Mock CommandSender` and plain lambda stubs.
- Each lambda documents exactly which server capability the command uses, rather than hiding it behind a broad `Server` reference.
- Adding a new server capability to the command requires adding exactly one new parameter — the delta is explicit.
- `PluginConfig` is passed directly (not wrapped in a lambda) because it is a plain data object with no behaviour; there is no testability benefit to abstracting it further.
