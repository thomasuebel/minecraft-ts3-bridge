# 6. TS event relay filtering

Date: 2026-03-22

## Status

Accepted

## Context

Three categories of TS3 ServerQuery events must be filtered correctly before being relayed to Minecraft:

**Text messages (`notifytextmessage`):**
The event carries a `targetmode` field (1 = private, 2 = channel, 3 = server-wide) and a `target` field. It was assumed that `target` would carry the channel ID in channel mode, but the TS3 protocol does not reliably populate it — `target` is typically `0` or absent for channel messages. Filtering by `targetId == channelId` silently drops all channel messages.

Additionally, the query client echoes its own sent messages back as events. The `invokeruid` field identifies the sender; filtering by comparing it against `whoAmI().getUniqueIdentifier()` suppresses the echo. Note: if the person testing uses the same TS3 identity as the ServerQuery login (same UID), their messages will also be filtered — always test with a different account.

**Client join (`notifycliententerview`):**
The event payload includes `client_nickname` and `client_type`. The typed HolyWaffle accessor `e.getClientNickname()` maps to a different internal key and returns `""`. Use `e.get("client_nickname")` (raw map access) instead.

**Client move (`notifyclientmoved`):**
This event does **not** carry `client_nickname`, `client_type`, or `client_unique_identifier`. In channel mode, most join announcements originate here (the client was already on the server and moved into the bridge channel). A live `getClientInfo(clid)` ServerQuery call is required to obtain the display name and determine whether the client is a query client.

**Client leave (`notifyclientleftview`):**
This event also does not carry `client_nickname`, and the client has already disconnected by the time the event fires — a live lookup is impossible. A `ConcurrentHashMap<Integer, String>` nickname cache (keyed by `clid`) is populated on join and move events and consumed on leave. Clients never added to the cache (query clients, unknown clients) are naturally excluded.

## Decision

**Text message filtering (`shouldRelayFromTeamspeak`):**
- Channel mode (`tsBridgeChannelId > 0`): relay any `targetMode=2` message. The `servernotifyregister event=textchannel` subscription already scopes events to the bridge channel — the subscription is the filter; `targetId` is ignored.
- Server-wide mode (`tsBridgeChannelId=0`): relay `targetMode=2` and `targetMode=3`. Always exclude `targetMode=1` (private) — private messages must never be broadcast to all Minecraft players.
- `e.get("target")` is null-checked before parsing; a missing field defaults to `0`.

**Nickname resolution:**
- `onClientJoin`: use `e.get("client_nickname")` (raw map access); populate nickname cache.
- `onClientMoved`: call `gateway.getClientInfo(clid)` (via `TeamspeakGateway`) for name and query-client check; populate nickname cache.
- `onClientLeave`: retrieve and remove name from nickname cache; skip if absent.

## Consequences

- Channel mode relay works correctly regardless of what the TS3 server puts in the `target` field.
- Join/leave display names are always resolved, eliminating the `[TS]  joined` empty-name bug.
- The nickname cache is a field on `TsToMcBridge` and is reset when a new instance is constructed on each `initialize()` call (i.e. on startup and `/ts reload`).
- Private TS messages are never leaked to Minecraft chat, regardless of mode.
