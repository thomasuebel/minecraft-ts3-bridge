# Network Security Guide

This guide covers the network requirements and security recommendations for running TS3Bridge.

## Port requirements

| Protocol | Port | Direction | Required |
|---|---|---|---|
| TCP | 10011 (RAW) or 10022 (SSH) | Minecraft server → TS3 server | Yes |

The Minecraft server must be able to initiate a TCP connection to the TS3 server's ServerQuery port. No inbound port is required on the Minecraft server.

## Topology recommendations

### Same machine (recommended for small communities)

If both the Minecraft server and the TS3 server run on the same machine, set `tsHost: localhost` in `config.yml`. The ServerQuery port never leaves the loopback interface. No firewall rules are required.

### Same private network (VPC, LAN, datacenter)

If both servers are on the same private network (e.g. two VMs in the same datacenter), the ServerQuery traffic stays within the private network. Ensure the TS3 server's firewall allows inbound TCP on the query port from the Minecraft server's private IP only.

### Different public hosts (internet-facing)

If the servers are on separate public IPs:

1. **Use SSH protocol.** Set `tsQueryProtocol: SSH` and `tsQueryPort: 10022` in `config.yml`. This encrypts credentials and commands in transit.
2. **Restrict the query port at the firewall.** Allow inbound TCP to the query port on the TS3 server **only from the Minecraft server's public IP**. Do not leave the port open to the internet.
3. **Consider a VPN.** WireGuard or similar provides a private network overlay between the two servers, eliminating the need for firewall-level IP restrictions.

### Hosted services (Nitrado, 4netplayers, etc.)

Many managed TS3 hosts do not expose the ServerQuery port externally, or do not allow firewall customisation. In this case, this plugin may not work unless you can run the Minecraft server on the same host or within the same network as the TS3 server. Contact your hosting provider to ask whether ServerQuery access from an external IP is possible.

## Credential security

The `tsQueryPassword` in `config.yml` is stored in plain text. Ensure:

- `config.yml` is **not committed to version control** — the `.gitignore` already excludes `plugins/TS3Bridge/config.yml`.
- File permissions restrict read access to the server process user only.
- The ServerQuery account is a **dedicated account with minimal permissions**, not the `serveradmin` account.

## Creating a minimal-permission ServerQuery account

Rather than using `serveradmin`, create a dedicated ServerQuery account:

1. Log into the TS3 server as `serveradmin`.
2. Create a new ServerQuery login: `serverqueryadd client_login_name=ts3bridge client_login_password=yourpassword` (or use the TS3 web UI if available).
3. The plugin requires these permissions at minimum:
   - `b_serverinstance_info_view`
   - `b_virtualserver_select`
   - `b_virtualserver_info_view`
   - `b_channel_info`
   - `b_client_info_view`
   - `i_channel_subscribe_power`
   - `b_virtualserver_notify_register`
   - Appropriate `i_client_move_power` if `tsBridgeChannelId > 0`

Using a scoped account limits the blast radius if the `config.yml` credentials are ever exposed.
