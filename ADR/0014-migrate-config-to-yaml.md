# 14. Use YAML for operator config

Date: 2026-03-23

## Status

Accepted. Supersedes ADR-0002 (file-based config) for the operator config file format.

## Context

ADR-0002 chose JSON for the operator config (`config.json`) citing simplicity and the Gson dependency already being present. Two problems emerged in practice:

1. JSON has no comment support. Operators cannot annotate their config files with reminders about valid values, units, or operational notes. External documentation is required to explain each field.

2. The Bukkit/Paper plugin ecosystem uses `config.yml` as the universal standard. Operators familiar with other plugins expect YAML.

## Decision

`ConfigManager` reads and writes `config.yml` using Bukkit's `YamlConfiguration`. All field keys are unchanged from the previous JSON layout. `saveConfig()` sets inline comments on every key so the file is self-documenting.

There is no migration path from `config.json`. The plugin has a single operator; greenfield adoption of `config.yml` is the correct approach.

Gson remains in the dependency graph for `MappingsRepository` (`mappings.json`), which stays as JSON — it is machine-written, not human-edited.

## Consequences

- The config file is self-documenting via inline YAML comments.
- Gson is no longer used in `ConfigManager`; the import is removed.
- `mappings.json` is unaffected.
