# 5. Plugin reload without server restart

Date: 2026-03-22

## Status

Accepted

## Context

Changing TeamSpeak credentials or any other config value previously required a full Minecraft server restart — a disruptive operation for a live server. The plugin holds stateful resources (a live TCP connection to TS, registered Bukkit event listeners, in-memory mappings) that must all be torn down and re-initialised cleanly when configuration changes.

Bukkit's built-in `/reload` command is unreliable with plugins that hold external connections; it does not guarantee correct listener deregistration. A plugin-managed reload is more predictable.

## Decision

`MctsPlugin` exposes a `reload()` method that performs a **full teardown and reinitialisation** in a fixed sequence:

1. Flush in-memory mappings to `mappings.json`.
2. Disconnect the existing TS ServerQuery connection.
3. `HandlerList.unregisterAll(this)` — removes all Bukkit event listeners registered by this plugin.
4. Call `initialize()` — re-reads `config.json`, reloads mappings, reconnects to TS, re-registers all listeners and the TS→MC event bridge.

`onEnable` calls `getDataFolder().mkdirs()` then `initialize()` directly. `reload()` skips `mkdirs()` and goes straight to teardown then `initialize()`.

The `/ts reload` command (permission `mcts.admin.reload`) invokes this method via a `Runnable` callback injected at construction time, keeping `TsCommand` decoupled from `MctsPlugin`.

## Consequences

- Config changes take effect immediately without restarting the server.
- TS3 error 513 (`TS_ERR_CLIENT_NICKNAME_INUSE`) is handled gracefully: the previous ServerQuery session may linger on the TS server for a few seconds after disconnect. The plugin logs a warning and continues fully operational under the query account's default name; a second `/ts reload` after a few seconds resolves it.
- The reload sequence is deterministic and the same code path runs on both first enable and reload — no divergence between startup and reload state.
