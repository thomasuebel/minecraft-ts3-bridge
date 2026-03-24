# 13. SSH ServerQuery support

Date: 2026-03-23

## Status

Accepted

## Context

The TS3 ServerQuery protocol on port 10011 uses plain TCP — credentials and commands transit in clear text. This is acceptable when the Minecraft server and TS3 server are on the same host (loopback only) or on a trusted private network, but is a meaningful risk when the connection crosses a public network boundary.

TS3 server 3.x ships with an SSH-based ServerQuery on port 10022. HolyWaffle's `TS3Config` supports it via `setProtocol(TS3Query.Protocol.SSH)`. This provides an encrypted transport channel using standard SSH, eliminating the credential-in-transit exposure without requiring VPN or firewall-level solutions.

The network operator review (see project docs) identified this as the most impactful low-effort network security improvement available.

## Decision

Add a `tsQueryProtocol` field to `PluginConfig` with default `"RAW"`. When set to `"SSH"`, `TeamspeakConnection.connect()` calls `ts3Config.setProtocol(TS3Query.Protocol.SSH)` before connecting. A startup log message confirms which protocol is active.

If `tsQueryProtocol=SSH` and `tsQueryPort=10011` (the RAW default), a warning is logged suggesting the operator update the port to 10022.

The default is `"RAW"` to preserve backward compatibility — existing deployments require no config change.

### SSH credential requirement (bug fix, 2026-03-24)

After enabling SSH on a real TS3 server, connections failed immediately with:

> `TS3ConnectionFailedException: Anonymous queries are not supported when using SSH. You must specify a query username and password using TS3Config#setLoginCredentials.`

The original implementation called `ts3Config.setProtocol(TS3Query.Protocol.SSH)` but did not call `ts3Config.setLoginCredentials(...)`. It relied on `api.login()` after connecting — the same pattern used for RAW. This is wrong for SSH: the HolyWaffle library authenticates during the SSH handshake and requires credentials on the `TS3Config` object before `TS3Query.connect()` is called. A post-connect `api.login()` call is not supported.

**Fix:** credentials are now embedded in `TS3Config` via `setLoginCredentials` for SSH, and the `api.login()` call is skipped for SSH in both the initial connect path and the reconnect handler.

To make the SSH/RAW credential decision explicit and unit-testable (without reflection into the third-party `TS3Config` whose getters are package-private), a `ConnectionSetup` record was extracted inside `TeamspeakConnection`. `ConnectionSetup.from(PluginConfig)` resolves the protocol and `embedCredentials` flag, and is tested directly. `buildTs3Config` and `connect()` consume it.

## Consequences

- Operators connecting across untrusted networks can opt into encrypted transport with one config change.
- `tsQueryProtocol=RAW` (default) preserves existing behaviour.
- TS3 server must have SSH ServerQuery enabled (it is enabled by default in TS3 server 3.x).
- SSH ServerQuery uses a different port (10022) from RAW (10011); operators must update `tsQueryPort` when switching.
- The startup log always states which protocol is in use, making misconfiguration visible.
- **SSH requires both `tsQueryUsername` and `tsQueryPassword` to be set** — anonymous SSH queries are rejected by the TS3 server.
