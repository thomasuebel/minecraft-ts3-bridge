# Paper 1.21.x Compatibility Report

Generated: 2026-03-24

---

## API Surface Used

| API | Package |
|---|---|
| `PlayerJoinEvent`, `PlayerQuitEvent` | `org.bukkit.event.player` |
| `AsyncChatEvent` | `io.papermc.paper.event.player` |
| `JavaPlugin`, `HandlerList`, `Listener`, `EventHandler` | `org.bukkit.*` |
| `Command`, `CommandSender`, `TabCompleter` | `org.bukkit.command` |
| `Player`, `OfflinePlayer` | `org.bukkit.entity` |
| `YamlConfiguration` | `org.bukkit.configuration.file` |
| `Component`, `PlainTextComponentSerializer` | `net.kyori.adventure.*` |

---

## Version-by-Version Assessment

| Version | Compatible | Notes |
|---|---|---|
| 1.21 | ✅ | Base version; all APIs present |
| 1.21.1 | ✅ | Tested and confirmed working |
| 1.21.2 | ✅ | No relevant API changes |
| 1.21.3 | ✅ | Timings set to no-op — plugin doesn't use it |
| 1.21.4 | ✅ | No relevant API changes |
| 1.21.5 | ✅ | No relevant API changes |
| 1.21.6 | ✅ | No relevant API changes |
| 1.21.7 | ✅ | `PlayerLoginEvent` deprecated — plugin doesn't use it; server reload deprecated — plugin doesn't use `Bukkit.reload()` |
| 1.21.8 | ✅ | No relevant API changes |
| 1.21.9 | ✅ | No relevant API changes |
| 1.21.10 | ✅ | No relevant API changes |
| 1.21.11 | ✅ | Gamerule names changed to snake_case — plugin doesn't use gamerules. Tested live on this version. |

---

## Risk Factors: None

- No NMS / internal server code — immune to the remapper removal coming in 26.1
- No deprecated APIs used
- `AsyncChatEvent` is Paper-only (not Spigot) — plugin already Paper-exclusive by design
- Command registration uses `plugin.yml` style — compatible with all 1.21.x; the new `LifecycleEventManager` API is additive, not a replacement yet

---

## Recommendations for Modrinth

- Declare compatibility with all of 1.21–1.21.11 on the Modrinth project page
- `api-version: '1.21'` in `plugin.yml` is correct — Paper loads the plugin on any 1.21.x server
