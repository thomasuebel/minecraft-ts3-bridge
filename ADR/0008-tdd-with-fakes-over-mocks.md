# 8. TDD with fakes over mocks for application classes

Date: 2026-03-22

## Status

Accepted

## Context

The project follows a strict TDD red/green loop: a failing test must exist before any implementation is written. The test suite uses JUnit 5 and Mockito.

Mockito's inline mock maker (required to mock concrete classes) uses bytecode manipulation that fails at runtime on Java 25, even when `targetCompatibility = 21` is set. Attempting to `@Mock` a concrete application class such as `TeamspeakService`, `UserLinkService`, or `MappingsRepository` produces an `InlineBytecodeGenerator` or `OpenedClassReader` error and aborts the test run.

Additionally, mocking concrete classes couples tests to internal implementation details rather than observable behaviour.

## Decision

**Only mock interfaces and Bukkit/Paper API types** (e.g. `Player`, `CommandSender`) — these are interfaces and Mockito can generate proxies for them without bytecode manipulation.

For concrete application classes that tests need to control, use one of:

- **`FakeTeamspeakGateway`** — an in-memory implementation of `TeamspeakGateway` used across all test suites. Stores sent messages and online clients in plain lists.
- **`FakeAdvertisementService`** — a subclass of `AdvertisementService` with configurable `shouldAdvertise` return value.
- **Real instances backed by `@TempDir`** — `MappingsRepository` and `ConfigManager` operate on a temporary directory; tests get real file I/O with automatic cleanup.

Never add `@Mock` to `TeamspeakService`, `UserLinkService`, `MappingsRepository`, `ChatBridgeService`, or any other concrete application class.

## Consequences

- Tests run reliably on Java 25 without Mockito inline configuration.
- Fakes are explicit, readable, and controlled — test failures point to behaviour, not mock setup.
- `TeamspeakGateway` as an interface (rather than directly using `TeamspeakConnection`) is load-bearing for testability: it is the seam that enables `FakeTeamspeakGateway`.
- Test class naming follows `<ClassName>Test` in the same package as the class under test, consistent with standard Java conventions.
