# TS3Bridge

[![CI](https://github.com/thomasuebel/minecraft-ts3-bridge/actions/workflows/ci.yml/badge.svg)](https://github.com/thomasuebel/minecraft-ts3-bridge/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/thomasuebel/minecraft-ts3-bridge/branch/master/graph/badge.svg)](https://codecov.io/gh/thomasuebel/minecraft-ts3-bridge)

My kids play Minecraft and wanted to talk to each other while doing so. I prefer TeamSpeak to Discord - self-hosted, no big tech, no accounts required for voice. The existing plugins (TeamspeakIP, BukkitSpeak) didn't work with the virtual server setup I have through 4netplayers hosting, so I built this one. Hope someone finds it useful.

---

TS3Bridge is a [Paper](https://papermc.io/) plugin for Minecraft that creates a bridge between Minecraft and a TeamSpeak 3 server via ServerQuery:

- Bidirectional chat relay between MC and TS
- Join/leave announcements in both directions
- Players link their MC account to their TS name with `/ts link`
- Works with hosted TS providers (4netplayers, Nitrado, ZAP-Hosting)

---

## Requirements

- Paper 1.21.1+
- Java 21+
- A TeamSpeak 3 server with ServerQuery access

---

## Installation

1. Drop the JAR into `plugins/`.
2. Start the server — `plugins/TS3Bridge/config.yml` is created with defaults.
3. Stop, fill in your TS credentials, restart (or `/ts reload`).

---

## Configuration

`plugins/TS3Bridge/config.yml`:

```yaml
# TeamSpeak server hostname or IP
tsHost: your-ts3-host.example.com
# ServerQuery TCP port (default 10011 for RAW, 10022 for SSH)
tsQueryPort: 10011
# Connection protocol: RAW (plain TCP) or SSH (encrypted)
tsQueryProtocol: RAW
# ServerQuery login
tsQueryUsername: serveradmin
# ServerQuery password
tsQueryPassword: your-query-password
# Virtual server ID — ignored when tsVirtualServerPort > 0
tsVirtualServerId: 1
# Voice port — use this for hosted providers instead of the ID (see below)
tsVirtualServerPort: 0
# Bot name shown in TS
tsQueryNickname: TS3Bridge
# Restrict bridge to a specific channel; 0 = server-wide
tsBridgeChannelId: 0
# Address shown in the join advertisement
tsServerAddress: your-ts3-host.example.com
# Sent to unlinked players on join. Use {address} as a placeholder.
advertisementMessage: "Join our TeamSpeak server: {address}"
# Toggle MC↔TS chat relay
chatBridgeEnabled: true
# Reconnect automatically after a connection drop
tsReconnectEnabled: true
# Log raw TS event payloads at FINE level for troubleshooting
debugLogging: false
```

| Field | Default | Notes |
|---|---|---|
| `tsHost` | `localhost` | TS server hostname or IP |
| `tsQueryPort` | `10011` | ServerQuery TCP port |
| `tsQueryProtocol` | `RAW` | `RAW` (plain TCP) or `SSH` (encrypted, use with port `10022`) |
| `tsQueryUsername` | *(empty)* | ServerQuery login |
| `tsQueryPassword` | *(empty)* | ServerQuery password |
| `tsVirtualServerId` | `1` | Virtual server ID — ignored when `tsVirtualServerPort > 0` |
| `tsVirtualServerPort` | `0` | Voice port — use this for hosted providers instead of the ID (see below) |
| `tsQueryNickname` | `TS3Bridge` | Bot name shown in TS |
| `tsBridgeChannelId` | `0` | Restrict bridge to a specific channel; `0` = server-wide |
| `tsServerAddress` | `localhost` | Address shown in the join advertisement |
| `advertisementMessage` | `Join our TeamSpeak server: {address}` | Sent to unlinked players on join |
| `chatBridgeEnabled` | `true` | Toggle MC↔TS chat relay |
| `tsReconnectEnabled` | `true` | Reconnect automatically after a connection drop; set to `false` to require manual `/ts reload` |
| `debugLogging` | `false` | Log raw TS event payloads at FINE level — requires a Java logging configuration change to see output |

### Hosted providers (4netplayers, Nitrado, ZAP-Hosting)

Hosted providers give you a **ServerQuery port** and a **voice port** — not a meaningful server ID. Use the voice port:

```yaml
tsQueryPort: 11200
tsVirtualServerPort: 10006
```

If you get an error like `invalid serverID (ID 1024)`, that `1024` is a TS3 error code, not your configured value — switch to `tsVirtualServerPort`.

---

## Commands

| Command | Who | Description |
|---|---|---|
| `/ts` | everyone | Show the TS address again |
| `/ts who` | everyone | Who's online in TeamSpeak |
| `/ts link <ts-name>` | everyone | Link your MC account to your TS display name |
| `/ts unlink` | everyone | Remove your link |
| `/ts status` | OP | Connection status, bridge mode, online count |
| `/ts link <mc-player> <ts-name>` | OP | Link another player |
| `/ts unlink <mc-player>` | OP | Remove another player's link |
| `/ts reload` | OP | Reload config and reconnect |

### Linking

1. Join TeamSpeak.
2. In Minecraft: `/ts link <the name shown next to you in TS>`

You must be online in TS when you run the command. Once linked, you won't see the join advertisement anymore and your MC name shows up next to your TS name in `/ts who`.

---

## Chat bridge

When `chatBridgeEnabled` is `true`:

- MC chat → TS: `[MC] PlayerName: message`
- TS chat → MC: `[TS] Nickname: message`
- MC join/leave → TS: `[MC] PlayerName joined/left the server`
- TS join/leave → MC: `[TS] Nickname joined/left`

Set `tsBridgeChannelId` to a channel ID to scope everything to one channel. The bot moves itself into that channel on startup. If you configure a wrong channel ID, the plugin logs a list of all available channels with their IDs and names — grab the right one and use `/ts reload`.

---

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/ts3-bridge-<version>.jar`

---

## Thanks

Big thanks to the maintainers of [HolyWaffle/TeamSpeak-3-Java-API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API) - the ServerQuery client library that made this whole thing a lot easier to build.
