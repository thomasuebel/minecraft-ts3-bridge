# 11. TS→MC bridge extraction and gateway interface hardening

Date: 2026-03-23

## Status

Accepted

## Context

The TS→MC event bridge — approximately 130 lines of business logic including a nickname cache, event filtering, and Bukkit scheduler dispatch — lived as a private method in `MctsPlugin`. This caused two problems:

1. `MctsPlugin` held a direct compile-time dependency on HolyWaffle's `TS3Api` type (via `teamspeakConnection.getApi()`), defeating the `TeamspeakGateway` interface abstraction. The interface was designed to decouple the rest of the codebase from HolyWaffle, but the plugin entry point bypassed it.

2. The nickname cache (`ConcurrentHashMap<Integer, String>`) was anonymous inline state, silently discarded and recreated on each `/ts reload` call. Its lifecycle was invisible.

The filtering logic in `ChatBridgeService` (`shouldRelayFromTeamspeak`, `shouldRelayTsStatusEvent`) was already correct domain logic, but the code that *called* those methods lived in the plugin class and could not be unit-tested without wiring up the full plugin.

## Decision

Extract a `TsToMcBridge` class into the `teamspeak` package. It owns the nickname cache as a named field and receives the gateway, chat bridge service, debug flag, logger, and a `Consumer<String>` broadcast callback. It exposes package-visible `handle*` methods for each TS event type, taking plain Java types only — no HolyWaffle types.

Add four methods to `TeamspeakGateway`:
- `registerAllEvents()` — delegates to `api.registerAllEvents()`
- `getSelfUniqueId()` — delegates to `api.whoAmI().getUniqueIdentifier()`
- `Optional<TsClient> getClientInfo(int clid)` — wraps `api.getClientInfo()`, returns `Optional.empty()` for query clients
- `registerBridge(TsToMcBridge bridge)` — creates the `TS3EventAdapter` in `TeamspeakConnection`, translating HolyWaffle event objects into plain-type calls on the bridge

Remove `getApi()` from `TeamspeakConnection`. HolyWaffle types are now confined to `TeamspeakConnection` only.

`MctsPlugin` constructs a `TsToMcBridge`, passing a Bukkit scheduler lambda as the broadcast consumer, and calls `register()`.

## Consequences

- `TsToMcBridge` has no HolyWaffle dependency and is fully unit-testable via `FakeTeamspeakGateway.simulate*` methods.
- The nickname cache lifecycle is explicit: it is a field of `TsToMcBridge`, reset when a new instance is constructed on each `initialize()` call.
- HolyWaffle types are confined to `TeamspeakConnection` — the `TeamspeakGateway` interface boundary is clean.
- `MctsPlugin` no longer imports any HolyWaffle classes.
- The `registerBridge` approach means HolyWaffle's `TS3EventAdapter` is an implementation detail of `TeamspeakConnection`, not a concern of callers.
