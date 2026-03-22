# 4. Player linking by TeamSpeak display name

Date: 2026-03-22

## Status

Accepted

## Context

Players need to associate their Minecraft account with their TeamSpeak identity so the plugin can correlate presence and skip redundant advertisements. The underlying storage key is the TS **UID** (a stable base64 identifier), but UIDs are opaque strings that players cannot reasonably be expected to know or type.

Earlier iterations used `/ts link <ts-uid>` directly. This was unusable in practice: players had no obvious way to find their own UID without navigating TS3 client menus.

The TS3 ServerQuery `clientlist` command returns all currently connected clients including their UID and display name. A display name is what players see next to themselves in the TS channel list — something they already know.

## Decision

Linking uses the player's **current TeamSpeak display name** (case-insensitive exact match) rather than their UID:

- `/ts link <ts-name>` — self-link; the player must be online in TS at the time.
- `/ts link <mc-player> <ts-name>` — admin link (`mcts.admin.link` / OP); links another online MC player.

The command resolves the display name to a UID at link time via a live `clientlist` query. The UID is what is persisted in `mappings.json`. If zero or more than one clients match the name, the command reports an error rather than guessing.

The player must be **online in TeamSpeak at the time of linking**. This serves as implicit verification that the person running the command controls (or is present with) the TS identity being linked.

## Consequences

- Players interact with a familiar name rather than an opaque UID — the UX is self-service without documentation.
- Ambiguous display names (two clients with the same name) require admin intervention; this is an edge case and a reasonable tradeoff.
- The UID stored in `mappings.json` remains stable even if the player later renames their TS identity — links do not break on rename.
- Linking requires an active TS connection. If the ServerQuery connection is down, `/ts link` is unavailable.
