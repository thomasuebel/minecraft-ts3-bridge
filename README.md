# MinecraftTS

My kids play Minecraft and wanted to talk to each other while doing so. I prefer TeamSpeak to Discord - self-hosted, no big tech, no accounts required for voice. The existing plugins (TeamspeakIP, BukkitSpeak) didn't work with the virtual server setup I have through 4netplayers hosting, so I built this one. Hope someone finds it useful.

---

MinecraftTS is a [Paper](https://papermc.io/) plugin that bridges Minecraft and a TeamSpeak 3 server via ServerQuery:

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
2. Start the server -  `plugins/MinecraftTS/config.json` is created with defaults.
3. Stop, fill in your TS credentials, restart (or `/ts reload`).

---

## Configuration

`plugins/MinecraftTS/config.json`:

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

| Field | Default | Notes |
|---|---|---|
| `tsHost` | `localhost` | TS server hostname or IP |
| `tsQueryPort` | `10011` | ServerQuery TCP port |
| `tsQueryUsername` | *(empty)* | ServerQuery login |
| `tsQueryPassword` | *(empty)* | ServerQuery password |
| `tsVirtualServerId` | `1` | Virtual server ID -  ignored when `tsVirtualServerPort > 0` |
| `tsVirtualServerPort` | `0` | Voice port -  use this for hosted providers instead of the ID (see below) |
| `tsQueryNickname` | `MinecraftTS` | Bot name shown in TS |
| `tsBridgeChannelId` | `0` | Restrict bridge to a specific channel; `0` = server-wide |
| `tsServerAddress` | `localhost` | Address shown in the join advertisement |
| `advertisementMessage` | `Join our TeamSpeak server: {address}` | Sent to unlinked players on join |
| `chatBridgeEnabled` | `true` | Toggle MC↔TS chat relay |
| `debugLogging` | `false` | Log raw TS event payloads -  useful for troubleshooting, noisy in production |

### Hosted providers (4netplayers, Nitrado, ZAP-Hosting)

Hosted providers give you a **ServerQuery port** and a **voice port** -  not a meaningful server ID. Use the voice port:

```json
{
  "tsQueryPort": 11200,
  "tsVirtualServerPort": 10006
}
```

If you get an error like `invalid serverID (ID 1024)`, that `1024` is a TS3 error code, not your configured value -  switch to `tsVirtualServerPort`.

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

Set `tsBridgeChannelId` to a channel ID to scope everything to one channel. The bot moves itself into that channel on startup. There must be a way to figure out the correct channel ID, but I haven't found it yet. So If you add a false channel ID, the plugin will give you a list of available channels with their ID and name. You can you `/ts reload`after configuring it.

---

## Building

```bash
./gradlew shadowJar
```

Output: `build/libs/minecraft-ts3-plugin-<version>.jar`

---

## Thanks

Big thanks to the maintainers of [HolyWaffle/TeamSpeak-3-Java-API](https://github.com/TheHolyWaffle/TeamSpeak-3-Java-API) - the ServerQuery client library that made this whole thing a lot easier to build.
