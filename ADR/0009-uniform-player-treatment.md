# 9. Uniform player treatment — no Bedrock or client-type detection

Date: 2026-03-22

## Status

Accepted

## Context

The plugin originally included a `BedrockDetector` class that identified Geyser/Bedrock Edition players via the Floodgate API (if installed) or a username-prefix heuristic (`.` prefix). The rationale was that Bedrock players cannot run the TeamSpeak desktop client and therefore needed special handling.

In practice:
- The advertisement and linking flow is identical for Java and Bedrock players.
- The Floodgate dependency (`org.geysermc.floodgate:api:2.2.3-SNAPSHOT`) added an external Maven repository and a compile-time dependency for no functional benefit.
- `BedrockDetector` was instantiated in `MctsPlugin` but never actually used (it was marked "kept for future use").
- Treating players differently based on client type introduces complexity and assumptions that may not hold (e.g. a Bedrock player with a desktop PC can run TeamSpeak).

## Decision

Remove all Bedrock/Geyser/Floodgate-specific code. All players are treated identically:

- `BedrockDetector` deleted.
- `BedrockAdvertisementService` renamed to `AdvertisementService` and moved to the `minecraft` package — it serves all players equally.
- `org.geysermc.floodgate:api` dependency and the `https://repo.opencollab.dev/main/` Maven repository removed from `build.gradle`.
- The `bedrock` source package removed entirely.

The join advertisement is sent to every unlinked player on join, regardless of client type.

## Consequences

- Codebase is simpler — one fewer domain package, one fewer external dependency, one fewer detection heuristic to maintain.
- All players receive the same advertisement on join. A player who is already linked (regardless of client type) skips it.
- No Floodgate installation is required or expected. The plugin works identically on servers with or without Geyser.
