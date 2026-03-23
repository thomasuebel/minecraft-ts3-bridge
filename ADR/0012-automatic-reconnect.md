# 12. Automatic reconnection with exponential backoff

Date: 2026-03-23

## Status

Accepted

## Context

If the TeamSpeak ServerQuery TCP connection drops mid-session (TS server restart, network hiccup, idle timeout), `TeamspeakConnection.connected` remains `true` until the next API call throws, at which point the plugin has no recovery path. The bridge silently stops relaying; operators must notice and manually run `/ts reload`. BukkitSpeak, the closest equivalent plugin in the ecosystem, already implements exponential backoff reconnection.

HolyWaffle's `TS3Config` supports this natively via `setReconnectStrategy(ReconnectStrategy)` and `setConnectionHandler(ConnectionHandler)`. The `ConnectionHandler.onConnect` callback fires on every (re)connection, including the initial one.

## Decision

When `tsReconnectEnabled=true` (default), set `ReconnectStrategy.exponentialBackoff()` and register a `ConnectionHandler` on the `TS3Config` before connecting. The `onConnect` callback re-runs the post-connect setup (login, virtual server selection, nickname, channel move) and then invokes a `Runnable` reconnect callback, which re-calls `TsToMcBridge.register()` to re-bind the TS event listeners.

The reconnect callback is set via `TeamspeakConnection.setReconnectCallback(Runnable)` after the bridge is constructed, avoiding a circular construction dependency.

`onDisconnect` logs a warning and sets `connected = false`. `onConnect` sets `connected = true` after post-connect setup succeeds.

`tsReconnectEnabled=false` disables this behaviour entirely, restoring the previous manual-reload-only operation. This is an escape hatch for operators who prefer explicit control.

The `ConnectionHandler.onConnect` callback fires on the HolyWaffle library thread. Only TS3 API calls are made in this callback — no Bukkit API calls are safe here. The `TsToMcBridge.register()` call only invokes `gateway.registerAllEvents()`, `gateway.getSelfUniqueId()`, and `gateway.registerBridge()`, all of which are TS3 API calls.

## Consequences

- The bridge recovers automatically after TS server restarts or network interruptions without operator intervention.
- `tsReconnectEnabled=false` in `config.json` restores the previous behaviour.
- Operators see a clear WARNING log when the connection drops, and an INFO log when it is restored.
- The circular construction dependency (bridge needs gateway, gateway's reconnect needs bridge) is broken by the `setReconnectCallback` setter called after bridge construction.
