# 1. TeamSpeak integration via ServerQuery only

Date: 2026-03-22

## Status

Accepted

## Context

The plugin needs to bridge Minecraft chat and player presence with a TeamSpeak 3 server. Two integration models exist:

- **Seated bot client** — a real TS3 client that joins channels, appears in the user list, and can be moved between channels programmatically.
- **ServerQuery** — a TCP text protocol for server administration; invisible to regular TS clients; full access to server state, chat, and events without occupying a channel slot.

The TS server is hosted by 4netplayers. Full TS admin access is not available, but ServerQuery credentials are provided. The seated-client model would require a dedicated TS identity and relies on the TS client application being installed and running alongside the Minecraft server — fragile and operationally complex. Research into existing plugins (BukkitSpeak, TeamspeakIP) shows they also use ServerQuery, not seated clients — ServerQuery is the universal approach in the ecosystem.

The HolyWaffle `teamspeak3-api` library (`com.github.theholywaffle:teamspeak3-api:1.3.0`) provides a mature Java ServerQuery client with event listener support.

## Decision

Use **ServerQuery exclusively** via the HolyWaffle library. The plugin connects as a query client, not as a visible TS user. No functionality that requires a seated client (e.g. automatic channel assignment of players) will be implemented.

## Consequences

- The query client is invisible to regular TS users — no ghost bot in the channel list.
- All chat relay, join/leave announcements, and client lookups are performed through ServerQuery commands and events.
- Feature scope is limited to what ServerQuery supports. Auto-moving players between TS channels based on Minecraft activity is explicitly out of scope.
- `TeamspeakGateway` interface abstracts HolyWaffle from the rest of the codebase, enabling clean testing without live TS connections.
