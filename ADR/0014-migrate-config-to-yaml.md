# 14. Migrate operator config from JSON to YAML

Date: 2026-03-23

## Status

Accepted. Supersedes ADR-0002 (file-based config) for the operator config file format.

## Context

ADR-0002 chose JSON for the operator config (`config.json`) citing simplicity and the Gson dependency already being present. Two problems emerged in practice:

1. JSON has no comment support. Operators cannot annotate their config files with reminders about valid values, units, or operational notes. External documentation (README, CLAUDE.md) is required to explain each field.

2. The Bukkit/Paper plugin ecosystem uses `config.yml` as the universal standard. Operators familiar with other plugins expect YAML and find JSON unexpectedly unfamiliar.

YAML is the correct format for operator-facing plugin configuration in this ecosystem. The additional ADR-0013 fields (`tsQueryProtocol`) and ADR-0012 fields (`tsReconnectEnabled`) pushed the config file to a size where inline comments become genuinely useful rather than marginal.

## Decision

`ConfigManager` now reads and writes `config.yml` using Bukkit's `YamlConfiguration`. All field keys are unchanged — only the file format changes.

On startup, if `config.yml` does not exist but `config.json` does, `ConfigManager` reads the old JSON, writes it to `config.yml` with comments, and renames `config.json` to `config.json.bak`. If both exist, `config.yml` takes precedence and a migration is not triggered.

Gson remains in the dependency graph for `MappingsRepository` (`mappings.json`), which stays as JSON. There is no operator-visible benefit to migrating mappings (it is machine-written, not human-edited).

## Consequences

- Operators editing `config.yml` see inline documentation for every field.
- Existing deployments are automatically migrated on next server start; no manual action required.
- `config.json.bak` remains as a safety net; operators can delete it once they are satisfied with the migration.
- Operators managing configs programmatically (deployment scripts that write `config.json`) must update their scripts to write `config.yml` instead.
- The Gson dependency remains (for `mappings.json`); no dependency reduction occurs.
