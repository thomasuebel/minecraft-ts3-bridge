# 2. File-based configuration and persistence

Date: 2026-03-22

## Status

Accepted

## Context

The plugin needs to store two categories of data:

- **Connection settings** — TS host, credentials, channel IDs, feature toggles. These are edited by a server admin and must survive server restarts.
- **Player identity mappings** — a map of Minecraft UUID → TeamSpeak UID representing voluntarily linked accounts. This data is small (one entry per linked player), changes infrequently, and must survive restarts.

Installing a database (SQLite, MySQL) would add an external dependency and operational burden for what is a small, bounded dataset. YAML (Bukkit's default format) carries parsing quirks and requires the Bukkit config API. JSON is simple, human-readable, and supported natively by Gson already in the dependency graph.

## Decision

Persist both categories as **JSON files** in the plugin's data folder:

- `config.json` — all plugin settings, serialised/deserialised by `ConfigManager` using Gson. Defaults are written on first start.
- `mappings.json` — the UUID→UID map, serialised/deserialised by `MappingsRepository`. Created empty on first start.

Both files are bootstrapped automatically on first plugin load so downstream code never needs to handle their absence.

## Consequences

- No database dependency; zero operational setup beyond filling in credentials.
- `config.json` is human-editable and can be updated without recompiling the plugin; `/ts reload` applies changes at runtime.
- Mappings are loaded fully into memory on startup and flushed to disk on every link/unlink operation and on shutdown — acceptable given the small dataset size.
- Concurrent write safety is handled at the `MappingsRepository` level; the in-memory map is the authoritative source during a running session.
- Existing `config.json` files with unknown fields (e.g. removed options like `advertisementMode`) are silently ignored by Gson on load — forward compatibility is free.
