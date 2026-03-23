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

## Consequences

- Operators connecting across untrusted networks can opt into encrypted transport with one config change.
- `tsQueryProtocol=RAW` (default) preserves existing behaviour.
- TS3 server must have SSH ServerQuery enabled (it is enabled by default in TS3 server 3.x).
- SSH ServerQuery uses a different port (10022) from RAW (10011); operators must update `tsQueryPort` when switching.
- The startup log always states which protocol is in use, making misconfiguration visible.
