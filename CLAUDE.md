# TS3Bridge — CLAUDE.md

A Minecraft plugin bridging Minecraft chat and player presence with a TeamSpeak 3 server via ServerQuery.

---

## Environment

- **Java:** 21 installed via SDKMAN
- **Build:** Gradle Wrapper (`./gradlew`). Never use a globally installed Gradle.
- **Target:** Java 21-25 / Paper 1.21.1 / Geyser-Spigot 2.9.5 / TS3 Server 3.6.2

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT` | Paper plugin API (compileOnly) |
| `com.github.theholywaffle:teamspeak3-api:1.3.0` | HolyWaffle TS3 ServerQuery client |
| `com.google.code.gson:gson:2.10.1` | JSON config serialisation |
| `org.junit.jupiter:junit-jupiter:5.10.2` | Unit testing |
| `org.mockito:mockito-core:5.15.2` | Mocking (interfaces/Bukkit types only — see ADR-0008) |

## Package Structure

Base: `de.thomasuebel.mc.ts3bridge`

```
├── configuration/   — PluginConfig, ConfigManager, file bootstrapping
├── teamspeak/       — TeamspeakGateway, TeamspeakConnection, TeamspeakService, TsClient
├── user/            — UserLinkService, MappingsRepository, LinkResult, UnlinkResult
├── chat/            — ChatBridgeService (bidirectional relay logic)
└── minecraft/       — MctsPlugin, AdvertisementService
    ├── command/         TsCommand (/ts subcommands)
    └── listener/        MinecraftChatListener, PlayerJoinListener, PlayerQuitListener
```

## Configuration (`config.json`)

| Field | Default | Description |
|---|---|---|
| `tsHost` | `localhost` | TS server hostname |
| `tsQueryPort` | `10011` | ServerQuery TCP port |
| `tsQueryUsername` | *(empty)* | ServerQuery login |
| `tsQueryPassword` | *(empty)* | ServerQuery password |
| `tsVirtualServerId` | `1` | Virtual server ID |
| `tsVirtualServerPort` | `0` | Voice port (when > 0, overrides `tsVirtualServerId`) |
| `tsServerAddress` | `localhost` | Address shown in advertisement |
| `tsQueryNickname` | `TS3Bridge` | Bot display name in TS |
| `tsBridgeChannelId` | `0` | Bridge channel ID; 0 = server-wide |
| `advertisementMessage` | `Join our TeamSpeak server: {address}` | Join message template |
| `chatBridgeEnabled` | `true` | Enable MC↔TS chat relay |
| `debugLogging` | `false` | Log raw TS event payloads at INFO |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/ts` | *(any)* | Show advertisement message + subcommand list |
| `/ts who` | `mcts.command.who` | List online TS clients with MC link status |
| `/ts status` | `mcts.admin.status` | Connection status, host, bridge mode, client count |
| `/ts link <ts-name>` | `mcts.command.link` | Self-link by TS display name |
| `/ts link <mc-player> <ts-name>` | `mcts.admin.link` | Admin link |
| `/ts unlink` | `mcts.command.unlink` | Remove MC↔TS link |
| `/ts reload` | `mcts.admin.reload` | Reload config and reconnect |

Future extension points (not implemented): `/ts msg`, `/ts channel`

## Permissions

LuckPerms nodes; vanilla OP as fallback. Admin commands (`status`, `reload`, `link <mc-player> <ts-name>`) require OP fallback.

## Plugin Metadata

- **Name:** `TS3Bridge`
- **Main class:** `de.thomasuebel.mc.ts3bridge.minecraft.MctsPlugin`
- **API version:** `1.21`

---

## Architecture Decisions

All significant decisions are recorded in [`ADR/`](ADR/):
