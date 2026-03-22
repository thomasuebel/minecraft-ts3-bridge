# 3. Domain-driven package structure

Date: 2026-03-22

## Status

Accepted

## Context

A Minecraft plugin commonly places all classes in a single flat package or organises by layer (controllers, services, models). Neither approach scales well as features grow: flat packages make ownership ambiguous, and layer-based packages scatter a single domain's logic across multiple locations.

The plugin covers several distinct concerns — TS connection management, player identity linking, chat bridging, configuration, and MC lifecycle wiring. Each concern has its own data model, failure modes, and testing needs.

## Decision

Organise the codebase by **domain** under the base package `de.thomasuebel.mc.ts3bridge`:

```
├── configuration/   — JSON config loading/saving, PluginConfig POJO, file bootstrapping
├── teamspeak/       — ServerQuery connection, TeamspeakGateway interface, TeamspeakService, TsClient
├── user/            — Player↔TS identity model, UserLinkService, MappingsRepository
├── chat/            — Bidirectional bridge relay logic, ChatBridgeService
└── minecraft/       — Paper plugin entry point (MctsPlugin), AdvertisementService,
    ├── command/         /ts command handler
    └── listener/        Bukkit event listeners (chat, join, quit)
```

The `minecraft` package is the wiring layer: it depends on all other domains but no other domain depends on it. Cross-domain dependencies flow inward toward `configuration` and `teamspeak`; `chat` and `user` are independent of each other.

## Consequences

- Each domain is independently testable without bringing up the full plugin.
- Adding a new feature means identifying which domain it belongs to before writing any code — a useful design forcing function.
- The `minecraft` package is the only place that should reference Bukkit/Paper APIs directly (except `user` for `UUID`, which is part of the JDK).
- `TeamspeakGateway` is the explicit boundary between the `teamspeak` domain and everything that depends on TS state — all test doubles implement this interface.
