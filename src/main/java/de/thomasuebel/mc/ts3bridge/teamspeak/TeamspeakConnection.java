package de.thomasuebel.mc.ts3bridge.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeamspeakConnection implements TeamspeakGateway {

    private final PluginConfig config;
    private final Logger logger;

    private TS3Query query;
    private TS3Api api;
    private boolean connected = false;

    public TeamspeakConnection(PluginConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void connect() {
        logger.info("Connecting to TeamSpeak ServerQuery at "
                + config.getTsHost() + ":" + config.getTsQueryPort() + "...");
        try {
            TS3Config ts3Config = new TS3Config();
            ts3Config.setHost(config.getTsHost());
            ts3Config.setQueryPort(config.getTsQueryPort());
            ts3Config.setFloodRate(TS3Query.FloodRate.DEFAULT);

            query = new TS3Query(ts3Config);
            query.connect();

            api = query.getApi();

            logger.info("TCP connection established. Logging in as '" + config.getTsQueryUsername() + "'...");
            api.login(config.getTsQueryUsername(), config.getTsQueryPassword());

            if (config.getTsVirtualServerPort() > 0) {
                logger.info("Login successful. Selecting virtual server by voice port " + config.getTsVirtualServerPort() + "...");
                api.selectVirtualServerByPort(config.getTsVirtualServerPort());
            } else {
                logger.info("Login successful. Selecting virtual server by ID " + config.getTsVirtualServerId() + "...");
                api.selectVirtualServerById(config.getTsVirtualServerId());
            }

            connected = true;
            logger.info("Successfully connected to TeamSpeak ServerQuery.");

            try {
                api.setNickname(config.getTsQueryNickname());
            } catch (TS3CommandFailedException e) {
                if (e.getError().getId() == 513) {
                    // TS3 error 513 = TS_ERR_CLIENT_NICKNAME_INUSE
                    // Happens on reload when the previous ServerQuery session hasn't fully
                    // closed on the server side yet. The plugin is otherwise fully connected.
                    logger.warning("Could not set nickname '" + config.getTsQueryNickname()
                            + "' — it is still in use by the previous session closing on the TS server. "
                            + "Messages will appear under the query account name for now. "
                            + "Run /ts reload again in a few seconds to retry.");
                } else {
                    logger.log(Level.WARNING,
                            "Could not set nickname '" + config.getTsQueryNickname() + "'.", e);
                }
            }

            if (config.getTsBridgeChannelId() > 0) {
                logger.info("Moving ServerQuery client to bridge channel " + config.getTsBridgeChannelId() + "...");
                try {
                    api.moveClient(api.whoAmI().getId(), config.getTsBridgeChannelId());
                    logger.info("Moved to bridge channel " + config.getTsBridgeChannelId() + ".");
                } catch (Exception moveEx) {
                    logger.log(Level.WARNING,
                            "Failed to move ServerQuery client to channel " + config.getTsBridgeChannelId()
                                    + " — the channel ID may be wrong. "
                                    + "Set tsBridgeChannelId=0 in config.json for server-wide mode. "
                                    + "Use /ts reload after correcting the value.",
                            moveEx);
                    logAvailableChannels();
                }
            }
        } catch (Exception e) {
            connected = false;
            logger.log(Level.SEVERE,
                    "Failed to connect to TeamSpeak ServerQuery. Check the following fields in config.json: "
                            + "tsHost ('" + config.getTsHost() + "'), "
                            + "tsQueryPort (" + config.getTsQueryPort() + "), "
                            + "tsQueryUsername ('" + config.getTsQueryUsername() + "'), "
                            + "tsQueryPassword, "
                            + "tsVirtualServerId (" + config.getTsVirtualServerId() + "), "
                            + "tsVirtualServerPort (" + config.getTsVirtualServerPort() + "). "
                            + "Tip: if your TS3 host gives you a voice port instead of a server ID "
                            + "(common with 4netplayers and similar providers), set tsVirtualServerPort "
                            + "to that port and leave tsVirtualServerId at 1.",
                    e);
        }
    }

    public void disconnect() {
        logger.info("Disconnecting from TeamSpeak ServerQuery...");
        if (query != null) {
            try {
                query.exit();
                logger.info("Disconnected from TeamSpeak ServerQuery.");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while disconnecting from TeamSpeak ServerQuery.", e);
            }
        }
        connected = false;
        api = null;
        query = null;
    }

    private void logAvailableChannels() {
        try {
            List<Channel> channels = api.getChannels();
            logger.warning("Available channels on this TeamSpeak server:");
            if (channels.isEmpty()) {
                logger.warning("  (no channels found)");
            } else {
                for (Channel channel : channels) {
                    logger.warning("  ID " + channel.getId() + " — " + channel.getName());
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not retrieve channel list.", e);
        }
    }

    /** Exposes the raw API for registering ServerQuery event listeners (TS→MC bridge). */
    public TS3Api getApi() {
        return api;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<String> getOnlineClientUids() {
        if (!connected) {
            return Collections.emptyList();
        }
        return api.getClients().stream()
                .filter(client -> !client.isServerQueryClient())
                .map(Client::getUniqueIdentifier)
                .toList();
    }

    @Override
    public List<TsClient> getOnlineClients() {
        if (!connected) {
            return Collections.emptyList();
        }
        return api.getClients().stream()
                .filter(client -> !client.isServerQueryClient())
                .map(client -> new TsClient(client.getUniqueIdentifier(), client.getNickname()))
                .toList();
    }

    @Override
    public void sendServerMessage(String message) {
        if (!connected) {
            return;
        }
        api.sendServerMessage(message);
    }

    @Override
    public void sendChannelMessage(String message) {
        if (!connected) {
            return;
        }
        api.sendChannelMessage(message);
    }
}
