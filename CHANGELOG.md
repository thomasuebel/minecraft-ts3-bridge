# Changelog

All notable changes to TS3Bridge are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased] — 1.1.3-SNAPSHOT

### Iteration 28 — Fix SSH ServerQuery authentication

SSH connections failed immediately with `Anonymous queries are not supported when using SSH` even when `tsQueryUsername` and `tsQueryPassword` were set in `config.yml`.

**Root cause:** The HolyWaffle library authenticates during the SSH handshake and requires credentials on the `TS3Config` object (via `setLoginCredentials`) before `connect()` is called. The original implementation set the SSH protocol flag but relied on the post-connect `api.login()` call — the pattern used for RAW TCP. For SSH, that post-connect login is neither supported nor needed.

**Fixes:**
- `TeamspeakConnection` now calls `ts3Config.setLoginCredentials(username, password)` when `tsQueryProtocol=SSH`
- The `api.login()` call is skipped for SSH in both the initial connect path and the reconnect handler
- Extracted a `ConnectionSetup` record inside `TeamspeakConnection` to make the SSH/RAW credential decision explicit and unit-testable; `ConnectionSetup.from(PluginConfig)` resolves the protocol and `embedCredentials` flag without touching the third-party `TS3Config` API
- ADR-0013 updated to document the SSH credential requirement and the `ConnectionSetup` architectural choice

---

### Iteration 27 — Remove Bedrock/Floodgate; all players treated equally

All players now receive identical treatment on join — no Bedrock detection, no Floodgate integration.

- Deleted `BedrockDetector` and the entire `bedrock` source package
- Renamed `BedrockAdvertisementService` → `AdvertisementService`, moved to `minecraft` package — the join advertisement logic is the same for all players
- Removed `org.geysermc.floodgate:api` compile dependency and the `https://repo.opencollab.dev/main/` repository from `build.gradle`
- All existing behaviour preserved: unlinked players receive the advertisement on join; linked players skip it

### Iteration 26 — Remove ACTIONBAR advertisement mode

The `advertisementMode` config field and `ACTIONBAR` delivery path have been removed. The join advertisement is always sent as a chat message.

- Deleted `AdvertisementMode` enum
- Removed `advertisementMode` from `PluginConfig`, `BedrockAdvertisementService`, `PlayerJoinListener`, and all tests
- `PlayerJoinListener` now calls `player.sendMessage()` directly — no mode branching
- Existing `config.json` files with `"advertisementMode": "CHAT"` continue to work (Gson ignores unknown fields on load)

### Iteration 25 — Richer `/ts status` output

`/ts status` now shows five lines instead of one:

- **Connection state**: `Connected` or `Disconnected — use /ts reload to retry`
- **Host**: TS hostname + ServerQuery port, and either `virtual server port <N>` or `virtual server ID <N>`
- **Bot nickname**: the query client's display name in TeamSpeak (connected only)
- **Bridge mode + chat toggle**: `Bridge: channel #<id>` or `Bridge: server-wide`, plus `Chat: enabled/disabled` (connected only)
- **Client count**: total non-query clients currently online in TeamSpeak (connected only)

### Iteration 24 — `/ts` shows advertisement message

- Running `/ts` with no subcommand now prints the configured advertisement message (e.g. `Join our TeamSpeak server: ts.example.com`) on the first line, followed by the existing subcommand usage line
- Players can use `/ts` at any time to retrieve the TS server address without asking an admin
- `TsCommand` receives the message via a `Supplier<String>` injected from `MctsPlugin` — keeps the command decoupled from `BedrockAdvertisementService` and fully unit-testable

### Iteration 23 — Fix nickname in TS client join/leave announcements (channel mode)

**Root cause:** In channel mode, real TS clients are typically already on the server before entering the bridge channel. Their arrival fires `notifyclientmoved`, not `notifycliententerview`. The `notifyclientmoved` TS3 event **does not carry `client_nickname`** (or `client_type`), so `e.get("client_nickname")` always returned `""`.

**Fixes:**
- **`onClientMoved`**: replaced `e.get("client_nickname")` with a live `getClientInfo(clid)` ServerQuery call, which provides both the display name and `isServerQueryClient()` for proper filtering. `client_type` and `client_unique_identifier` are also absent from move events, so both filters now use the fetched `Client` object.
- **`onClientLeave`**: `notifyclientleftview` never carries `client_nickname` either, and the client has already disconnected by the time the event fires so a live lookup is not possible. Added a `ConcurrentHashMap<Integer, String>` nickname cache (keyed by `clid`) that is populated on join and move events and consumed on leave. Unknown clients (ServerQuery, or clients never observed entering the bridge scope) are naturally excluded by a missing cache entry.
- **`onClientJoin`**: now also populates the nickname cache on server-wide join (already had the correct `client_nickname` field available from `notifycliententerview`).

### Iteration 22 — `debugLogging` config flag for TS event diagnostics

- **Added** `debugLogging` boolean to `PluginConfig` and `config.json` (default `false`)
- When `true`, raw TS event map contents are logged at INFO for both `onTextMessage` and `onClientJoin`, plus targetMode/targetId filter decisions — useful for diagnosing bridge issues without permanent log noise
- Raw event dumps are suppressed when `debugLogging=false` (the default)

### Iteration 21 — Fix missing nickname in TS client join announcements

- `[TS]  joined` (empty nickname) fixed: `onClientJoin` was calling `e.getClientNickname()` (HolyWaffle typed accessor), which maps to a different internal key and returns `""` on `ClientJoinEvent`
- Fixed to `e.get("client_nickname")` — consistent with how `onClientLeave` and `onClientMoved` already retrieve the nickname
- No behaviour change other than the nickname now appearing correctly

### Iteration 20 — Fix TS→MC chat relay (channel mode)

**Bug:** TS channel messages were silently dropped when `tsBridgeChannelId > 0`. Root cause: `shouldRelayFromTeamspeak` checked `targetId == channelId`, assuming the TS3 `notifytextmessage` event includes the channel ID in its `target` field. The TS3 ServerQuery protocol does not reliably do this — `target` is typically `0` or absent for channel messages.

**Fixes:**
- **Channel mode** (`tsBridgeChannelId > 0`): now relays any `targetMode=2` message without checking `targetId`. The `textchannel` subscription (`servernotifyregister event=textchannel`) already guarantees the event is from the bridge channel — a `targetId` check is both redundant and wrong.
- **Server-wide mode** (`tsBridgeChannelId=0`): now explicitly excludes `targetMode=1` (private messages). Previously, private TS messages directed at the query client would have been broadcast to all Minecraft players.
- **Defensive `target` parsing** in `MctsPlugin.onTextMessage`: `e.get("target")` is null-checked; a missing or empty field defaults to `0` instead of throwing `NumberFormatException`.
- Tests updated to match actual TS3 protocol behaviour (`target=0` is the typical channel message event value, not the channel ID).

### Iteration 19 — Graceful handling of "nickname in use" on reload

- `setNickname` is now wrapped in its own try-catch, separate from the fatal connection try-catch
- `connected = true` is set before `setNickname` — the plugin is fully operational even if the nickname can't be claimed
- TS3 error 513 (`TS_ERR_CLIENT_NICKNAME_INUSE`) is handled specifically: logs a `WARNING` explaining that the previous session is still closing on the TS server side, and instructs the admin to run `/ts reload` again in a few seconds
- Any other `setNickname` failure is also non-fatal: logs a `WARNING` with the exception and continues
- Follows the same pattern established for the channel move fix (iteration 17): only genuine TCP/login/server-selection failures are fatal

### Iteration 18 — Link by TS nickname; `/ts who` shows display names

**`/ts who` improvements:**
- Now shows each online client's TeamSpeak **display name** instead of their opaque UID
- Linked clients show the associated MC player's name: `Steve (linked to MC: MinecraftSteve)`
- Unlinked clients show: `JohnDoe (not linked)`
- `TeamspeakGateway.getOnlineClients()` added — returns `List<TsClient>` (uid + nickname pairs) instead of uid strings only
- New `TsClient(String uid, String nickname)` record in the `teamspeak` package

**`/ts link` — link by TS display name:**
- `/ts link <ts-nickname>` — players now type their **TeamSpeak display name** instead of their UID
- Exact, case-insensitive match against currently-online TS clients
- 0 matches → `"No TS user named '…' is currently online."` with hint to check TS connection
- 2+ matches → lists all matching names, asks player to contact an admin
- 1 match → links via resolved UID (same underlying `UserLinkService.link()` as before)

**`/ts link <mc-player> <ts-nickname>` — admin link (new):**
- OPs (or holders of `mcts.admin.link`) can link any **online** MC player to a TS display name
- Same 0 / ambiguous / success logic as self-link
- Added `mcts.admin.link` permission node (default: op)

**Infrastructure:**
- `TeamspeakService.findOnlineClientsByNickname(String)` — case-insensitive exact match returning all hits
- `TsCommand` now receives `Function<UUID, String> playerNameResolver` and `Function<String, Optional<Player>> onlinePlayerFinder` lambdas, injected from `MctsPlugin` — keeps command decoupled from `Server` and fully unit-testable
- `FakeTeamspeakGateway.addOnlineClient(uid, nickname)` — new test helper; `addOnlineUid(uid)` kept as shorthand that uses the UID as its own nickname

### Iteration 17 — Invalid bridge channel ID: log available channels

- When `tsBridgeChannelId` is set but `moveClient` fails (e.g. wrong channel ID), the plugin now logs a `WARNING` with the full list of available channels (ID + name) instead of treating it as a fatal connection error
- `connected = true` is now set before the channel move attempt — a bad channel ID no longer marks the plugin as disconnected; all other functionality (chat bridge, commands) continues to work
- Warning message directs the admin to set `tsBridgeChannelId=0` for server-wide mode or use `/ts reload` after correcting the value
- If the channel list fetch itself fails, that is also caught and logged without crashing

### Iteration 16 — `/ts reload` command

- **Added** `/ts reload` subcommand — reloads `config.json` and `mappings.json` from disk and reconnects to TeamSpeak without restarting the server
- **Added** `mcts.admin.reload` permission (default: op); vanilla OP fallback consistent with other admin commands
- **Added** `MctsPlugin.reload()` — canonical reload sequence: save mappings → disconnect TS → `HandlerList.unregisterAll(this)` → `initialize()` with fresh instances
- **Refactored** `MctsPlugin.onEnable` body extracted into `initialize()`; both `onEnable` and `reload()` call it; no logic duplication
- **Changed** `TsCommand` constructor accepts a `Runnable reloadCallback` — plugin lifecycle callback injected at wire-up time, keeping the command decoupled from `MctsPlugin` and unit-testable
- **Updated** plugin.yml: added `mcts.admin.reload` permission node and `reload` to the command usage string
- **Updated** failed-connect warning message from "restart the server" to "use /ts reload to retry"
- **Added** `TsCommandTest` — tests reload permission gate, callback invocation, confirmation message, and tab completion; follows project pattern of only mocking Bukkit interfaces (`CommandSender`), not concrete application classes

### Iteration 15 — Status message bridge (join/leave events)

**TS→MC:**
- When a TeamSpeak client joins the server (or enters the bridge channel), `[TS] Nickname joined` is broadcast in Minecraft
- When a TeamSpeak client leaves the server (or leaves the bridge channel), `[TS] Nickname left` is broadcast in Minecraft
- When channel mode is active, only clients entering/leaving the configured channel trigger announcements; `onClientMoved` handles channel switches (join & leave side)
- When no channel is configured, all client join/leave events are relayed server-wide

**MC→TS:**
- When a Minecraft player joins, `[MC] PlayerName joined the server` is sent to TeamSpeak
- When a Minecraft player leaves, `[MC] PlayerName left the server` is sent to TeamSpeak
- Both use channel or server-wide mode consistent with `tsBridgeChannelId`

**Infrastructure:**
- `PlayerJoinListener` now accepts `ChatBridgeService` and `chatBridgeEnabled` flag; relays join regardless of whether the advertisement is sent
- New `PlayerQuitListener` handles the MC player quit event
- New `FakeBedrockAdvertisementService` test double (concrete classes cannot be Mockito-mocked on Java 25)
- Paper API promoted from `testCompileOnly` to `testImplementation` so listener tests can run

### Iteration 14 — Channel-scoped chat bridge

- **Added** `tsBridgeChannelId` to `PluginConfig` (int, default `0` = no channel filter)
- When `tsBridgeChannelId > 0`:
  - **MC→TS**: `ChatBridgeService` sends via `sendChannelMessage` instead of `sendServerMessage`; the ServerQuery client is moved into the configured channel on startup
  - **TS→MC**: only channel text messages (`targetmode=2`) targeted at the configured channel are relayed to Minecraft; server-wide and private TS messages are ignored
- When `tsBridgeChannelId == 0` (default), existing behaviour is preserved
- **Added** `TeamspeakGateway.sendChannelMessage(String)` — sends to the ServerQuery client's current channel
- **Added** `ChatBridgeService.shouldRelayFromTeamspeak(int targetMode, int targetId)` — filter logic, fully unit-tested

### Iteration 13 — Configurable ServerQuery nickname

- **Added** `tsQueryNickname` to `PluginConfig` (default `"TS3Bridge"`)
- `TeamspeakConnection.connect()` now calls `api.setNickname()` after selecting the virtual server, so MC→TS chat messages appear under a recognisable name instead of the query account name
- Fixes the "Spy981"-style display where the query account name collided with a real player's TS nickname (causing TS to append a number)

### Iteration 12 — Advertisement sent to all players; configurable message

- **Changed** `PlayerJoinListener` to send the advertisement to **all** unlinked players on join, not only Bedrock players; `BedrockDetector` is no longer a dependency of the listener
- **Added** `advertisementMessage` field to `PluginConfig` — default `"Join our TeamSpeak server: {address}"`; the `{address}` placeholder is replaced at runtime with `tsServerAddress`
- `BedrockAdvertisementService.buildAdvertisementMessage()` now resolves the template from config instead of using a hardcoded string

### Iteration 11 — Virtual server port support

- **Added** `tsVirtualServerPort` to `PluginConfig` (default `0` = disabled)
- When `tsVirtualServerPort > 0`, `TeamspeakConnection` calls `selectVirtualServerByPort()` instead of `selectVirtualServerById()`; `tsVirtualServerId` is ignored
- This fixes connection failures on hosted providers (4netplayers, Nitrado, ZAP-Hosting) that expose a voice port rather than a meaningful server ID
- Error message on connect failure now includes `tsVirtualServerPort` and explains the ID-vs-port distinction
- Added `README.md` documenting the full configuration reference, the ID-vs-port distinction, commands, permissions, chat bridge, and Bedrock behaviour

**Background:** The TS3 error `invalid serverID (ID 1024)` means error code `1024` (0x400), not that the configured value was `1024`. It is thrown when the server ID does not match any virtual server — typically because the voice port was mistakenly used as a server ID.

### Iteration 10 — Version token fix

- Fixed `plugin.yml` reporting `${version}` instead of the real version
- Added `processResources { filesMatching('plugin.yml') { expand(version: project.version) } }` to `build.gradle`

### Iteration 9 — Logging improvements

- All classes that log now receive a `java.util.logging.Logger` via constructor injection (no static loggers)
- **`ConfigManager`**: logs whether `config.json` was created (first start) or loaded; corrupted JSON logs `SEVERE` with stacktrace and tells the admin to fix or delete the file
- **`MappingsRepository`**: logs mapping count on load and save; load errors log `SEVERE` with stacktrace; save errors log `SEVERE` warning that data may be lost before rethrowing
- **`TeamspeakConnection`**: step-by-step connect logging (TCP → login → virtual server); failure logs `SEVERE` with full stacktrace and names every relevant config field; disconnect is logged including errors
- **`UserLinkService`**: every link/unlink attempt is logged at `INFO` (server-side audit trail); failure reasons are logged with player UUID and TS UID
- **`BedrockDetector`**: logs on construction whether Floodgate or prefix fallback is active; Floodgate runtime failures log `WARNING` with stacktrace
- **`PlayerJoinListener`**: logs when an advertisement is sent (mode included) and when skipped for linked players
- **`MctsPlugin`**: each startup phase logged individually; warns clearly if TS is unavailable; `onTextMessage` exceptions are caught and logged with `SEVERE` + stacktrace instead of crashing silently; bridge registration failure is caught and logged

### Iteration 8 — Package rename

- Renamed all packages from `dev.thomas.mcts` to `de.thomasuebel.mc.ts3bridge`
- Updated `plugin.yml` main class reference and `build.gradle` group accordingly

### Iteration 7 — `minecraft` domain (wire-up)

- **Added** `MctsPlugin` — Paper plugin entry point; wires all domains together on enable/disable
- **Added** `TsCommand` — handles `/ts who`, `/ts status`, `/ts link <uid>`, `/ts unlink`; tab-completion included; designed for easy extension with new subcommands
- **Added** `MinecraftChatListener` — listens to `AsyncChatEvent` and relays to TeamSpeak when bridge is enabled
- **Added** `PlayerJoinListener` — sends TS advertisement to players on join
- TS→MC bridge registered via `TS3EventAdapter.onTextMessage` — incoming TS messages are broadcast on the Bukkit main thread
- LuckPerms permission nodes used with vanilla OP as fallback

### Iteration 6 — `bedrock` domain (TDD) *(superseded by iteration 27)*

- **Added** `BedrockDetector` — detects Geyser/Bedrock players; uses Floodgate API if present, falls back to username prefix check (`.`)
- **Added** `BedrockAdvertisementService` — determines whether a player should receive the TS advertisement on join (linked players are skipped); builds the advertisement message from config

### Iteration 5 — `chat` domain (TDD)

- **Added** `ChatBridgeService` — formats and relays chat in both directions:
  - MC → TS: `[MC] PlayerName: message` via `sendServerMessage`
  - TS → MC: formats as `[TS] Nickname: message` for Minecraft broadcast
- Blank and empty messages are silently dropped and not relayed

### Iteration 4 — `user` domain (TDD)

- **Added** `MappingsRepository` — loads/saves `mappings.json` as a `Map<mc-uuid, ts-uid>`; thread-safe read/write via Gson
- **Added** `UserLinkService` — orchestrates link/unlink with validation: checks TS connectivity, prevents duplicate links, verifies TS UID is online at time of linking
- **Added** `LinkResult` enum — `SUCCESS`, `ALREADY_LINKED`, `TS_UID_NOT_ONLINE`, `TEAMSPEAK_UNAVAILABLE`
- **Added** `UnlinkResult` enum — `SUCCESS`, `NOT_LINKED`
- Mappings are persisted to disk on every link and unlink operation

### Iteration 3 — `teamspeak` domain (TDD)

- **Added** `TeamspeakGateway` interface — abstracts HolyWaffle's `TS3Api` from the rest of the codebase; enables clean mocking in tests without Mockito inline bytecode manipulation (which fails on Java 25)
- **Added** `TeamspeakConnection` — implements `TeamspeakGateway`; wraps HolyWaffle `TS3Query`; handles connect/disconnect lifecycle; exposes raw `TS3Api` for ServerQuery event listener registration
- **Added** `TeamspeakService` — business logic over the gateway (online UID list, connection status, send server message)
- **Added** `FakeTeamspeakGateway` (test helper) — in-memory stub used across all test suites, avoiding the need to mock concrete HolyWaffle classes

### Iteration 2 — `configuration` domain (TDD)

- **Added** `PluginConfig` — POJO holding all plugin settings with sane defaults
- **Added** `AdvertisementMode` enum — `CHAT` / `ACTIONBAR`
- **Added** `ConfigManager` — loads `config.json` on startup, creates it with defaults on first run; bootstraps empty `mappings.json` if missing; saves on request
- All config files are guaranteed to exist after `ConfigManager.load()` — no null checks needed downstream

**`config.json` fields (defaults):**

| Field | Default | Description |
|---|---|---|
| `tsHost` | `localhost` | TeamSpeak server hostname |
| `tsQueryPort` | `10011` | ServerQuery TCP port |
| `tsQueryUsername` | *(empty)* | ServerQuery login |
| `tsQueryPassword` | *(empty)* | ServerQuery password |
| `tsVirtualServerId` | `1` | Virtual server ID |
| `tsVirtualServerPort` | `0` | Voice port (alternative to ID, see iteration 11) |
| `tsServerAddress` | `localhost` | Address advertised to players |
| `advertisementMessage` | `Join our TeamSpeak server: {address}` | Join message template (see iteration 12) |
| `chatBridgeEnabled` | `true` | Enable/disable MC↔TS chat relay |
| `tsQueryNickname` | `TS3Bridge` | Nickname shown in TS for ServerQuery messages (see iteration 13) |
| `tsBridgeChannelId` | `0` | TS channel ID for the chat bridge; 0 = server-wide (see iteration 14) |
| `debugLogging` | `false` | Log raw TS event data at INFO for bridge diagnostics (see iteration 22) |

### Iteration 1 — Project scaffolding

- Initialised Gradle project with Gradle wrapper (9.4.1) and Shadow plugin (`com.gradleup.shadow 8.3.6`)
- Configured Java 21 source/target compatibility (runs on Java 25 via SDKMAN)
- Added dependencies: Paper API 1.21.1, HolyWaffle TeamSpeak-3-Java-API 1.3.0, Gson 2.10.1, JUnit 5, Mockito 5
- Created `plugin.yml` with `/ts` command declaration and permission nodes
- Created stub `MctsPlugin` main class
- Created `.gitignore` and `CLAUDE.md` (project context reference)
- Added Floodgate API (`org.geysermc.floodgate:api:2.2.3-SNAPSHOT`) as compile-only dependency for Bedrock detection
- Resolved Gradle 9.x compatibility: added `junit-platform-launcher` runtime dependency, switched from deprecated Groovy space-assignment DSL
