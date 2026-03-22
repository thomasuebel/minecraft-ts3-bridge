# MinecraftTS — TeamSpeak 3 Bridge Plugin

A Paper 1.21+ plugin that integrates your Minecraft server with a TeamSpeak 3 server via ServerQuery.
Supports Bedrock players (via Geyser/Floodgate), bidirectional chat bridging, and player account linking.

---

## Requirements

- Paper 1.21.1 or later
- Java 21 or later
- A TeamSpeak 3 server with **ServerQuery access** (TCP, not client plugin)

---

## Installation

1. Drop `minecraft-ts3-plugin-<version>.jar` into your server's `plugins/` folder.
2. Start the server once. The plugin will create `plugins/MinecraftTS/config.json` with defaults.
3. Stop the server, fill in your TeamSpeak credentials (see below), then restart.

---

## Configuration (`plugins/MinecraftTS/config.json`)

```json
{
  "tsHost": "your-ts3-host.example.com",
  "tsQueryPort": 10011,
  "tsQueryUsername": "serveradmin",
  "tsQueryPassword": "your-query-password",
  "tsVirtualServerId": 1,
  "tsVirtualServerPort": 0,
  "tsQueryNickname": "MinecraftTS",
  "tsBridgeChannelId": 0,
  "tsServerAddress": "your-ts3-host.example.com",
  "advertisementMessage": "Join our TeamSpeak server: {address}",
  "chatBridgeEnabled": true,
  "debugLogging": false
}
```

| Field | Description | Default |
|---|---|---|
| `tsHost` | Hostname or IP of the TeamSpeak server | `localhost` |
| `tsQueryPort` | **ServerQuery** TCP port (not the voice port) | `10011` |
| `tsQueryUsername` | ServerQuery login username | *(empty)* |
| `tsQueryPassword` | ServerQuery login password | *(empty)* |
| `tsVirtualServerId` | Virtual server ID (used when `tsVirtualServerPort` is 0) | `1` |
| `tsVirtualServerPort` | Voice port of the virtual server — use this instead of `tsVirtualServerId` for hosted providers (see below) | `0` (disabled) |
| `tsQueryNickname` | Nickname shown in TeamSpeak for messages sent by the plugin | `MinecraftTS` |
| `tsBridgeChannelId` | TeamSpeak channel ID the chat bridge operates in; `0` = server-wide (no channel filter) | `0` |
| `tsServerAddress` | Address advertised to players who cannot use TeamSpeak | `localhost` |
| `advertisementMessage` | Join message template; `{address}` is replaced with `tsServerAddress` | `Join our TeamSpeak server: {address}` |
| `chatBridgeEnabled` | Enable bidirectional chat and status message relay between MC and TS | `true` |
| `debugLogging` | Log raw TS event payloads at INFO level for bridge diagnostics; leave `false` in production | `false` |

---

## Selecting the Virtual Server: ID vs Port

TeamSpeak's ServerQuery offers two ways to select which virtual server to connect to:

- **By ID** (`tsVirtualServerId`): the internal numeric ID assigned by the TS3 software (usually `1` for a single server instance).
- **By Port** (`tsVirtualServerPort`): the UDP port that TS3 **clients** connect on (e.g. `9987` by default, or a custom port like `10006`).

When `tsVirtualServerPort` is set to a value greater than `0`, it takes priority and `tsVirtualServerId` is ignored.

### Standard self-hosted setup

Most self-hosted TS3 servers have a single virtual server with ID `1`:

```json
{
  "tsVirtualServerId": 1,
  "tsVirtualServerPort": 0
}
```

### Hosted providers (e.g. 4netplayers, Nitrado, ZAP-Hosting)

Hosted providers typically give you:
- A **ServerQuery port** (e.g. `11200`) — goes into `tsQueryPort`
- A **voice port** that clients connect on (e.g. `10006`) — goes into `tsVirtualServerPort`

The virtual server ID reported by the provider is usually meaningless or unavailable.
Use the voice port to select the server instead:

```json
{
  "tsQueryPort": 11200,
  "tsVirtualServerId": 1,
  "tsVirtualServerPort": 10006
}
```

If you try to connect using a voice port as a server ID (i.e. setting `tsVirtualServerId: 10006`),
you will get an error like `invalid serverID (ID 1024)` — where `1024` is the TS3 **error code**,
not your configured value. Switch to `tsVirtualServerPort` to fix this.

---

## Commands

All commands are subcommands of `/ts`.

| Command | Permission | Description |
|---|---|---|
| `/ts` | *(any)* | Show the TeamSpeak server address and available subcommands |
| `/ts who` | `mcts.command.who` | List players online in TeamSpeak with their display names and MC link status |
| `/ts status` | `mcts.admin.status` | Show the ServerQuery connection status |
| `/ts link <ts-name>` | `mcts.command.link` | Link your Minecraft account to your TeamSpeak display name |
| `/ts link <mc-player> <ts-name>` | `mcts.admin.link` | Link another player's account (admin) |
| `/ts unlink` | `mcts.command.unlink` | Remove your Minecraft↔TeamSpeak link |
| `/ts reload` | `mcts.admin.reload` | Reload config and reconnect to TeamSpeak without restarting |

### TeamSpeak server address

When a player joins the server they receive the configured advertisement message in chat (e.g. `Join our TeamSpeak server: ts.example.com`), unless their account is already linked to TeamSpeak. Running `/ts` with no arguments prints the same message again at any time.

The message text is set via `advertisementMessage` in `config.json`.

### Linking your account (players)

1. Join the TeamSpeak server.
2. In Minecraft, run `/ts link <your-ts-display-name>` — type the name that appears next to you in TeamSpeak.

You must be **online in TeamSpeak at the time of linking**. If multiple people have the same display name, the command will list them and ask you to contact an admin.

### Linking a player (admins)

If a player cannot self-link (e.g. name collision), an OP can link them:

```
/ts link <minecraft-player-name> <ts-display-name>
```

Both the Minecraft player and the TeamSpeak user must be online at the time.

---

## Permissions

Permission nodes work with LuckPerms. If LuckPerms is not installed, OP status is used as a fallback.

| Node | Default | Description |
|---|---|---|
| `mcts.command.use` | `true` | Access to the `/ts` command at all |
| `mcts.command.who` | `true` | `/ts who` |
| `mcts.command.link` | `true` | `/ts link <ts-name>` (self-link) |
| `mcts.command.unlink` | `true` | `/ts unlink` |
| `mcts.admin.status` | OP | `/ts status` |
| `mcts.admin.link` | OP | `/ts link <mc-player> <ts-name>` (link another player) |
| `mcts.admin.reload` | OP | `/ts reload` |

---

## Chat Bridge

When `chatBridgeEnabled` is `true`:

- Minecraft chat messages are sent to TeamSpeak as `[MC] PlayerName: message`.
- TeamSpeak channel messages are broadcast to all Minecraft players as `[TS] Nickname: message`.
- Minecraft player join/leave events are announced in TeamSpeak as `[MC] PlayerName joined/left the server`.
- TeamSpeak client join/leave events are announced in Minecraft as `[TS] Nickname joined/left`.

Bedrock players (via Geyser) participate in the bridge automatically — their messages appear in TeamSpeak just like Java players.

### Channel-scoped bridge

By default (`tsBridgeChannelId: 0`) the bridge uses server-wide messages, so everyone on the TS server sees MC chat regardless of which channel they are in.

To restrict the bridge to a single channel, set `tsBridgeChannelId` to the numeric ID of that channel:

```json
{
  "tsBridgeChannelId": 42
}
```

When a channel is configured:
- **MC→TS**: chat messages and join/leave announcements are sent as channel messages (visible only to clients in that channel). The ServerQuery client is automatically moved into the channel on startup.
- **TS→MC**: only chat messages typed in that channel are relayed. Join/leave announcements are shown when a client enters or exits the configured channel specifically (including switching channels).

To find a channel's ID in TeamSpeak, right-click the channel → **Edit Channel** and note the ID, or use a ServerQuery `channellist` command.

---

## Player Mappings (`plugins/MinecraftTS/mappings.json`)

Player links are stored in `mappings.json` as a map of Minecraft UUID → TeamSpeak UID.
This file is created automatically on first start and saved on every link/unlink and on server shutdown.
Do not edit it manually while the server is running.

---

## Building from source

Requires Java 21+ (installed via SDKMAN or otherwise).

```bash
./gradlew build
```

The output jar is at `build/libs/minecraft-ts3-plugin-<version>.jar`.
