package de.thomasuebel.mc.ts3bridge.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Api;
import com.github.theholywaffle.teamspeak3.TS3Config;
import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.exception.TS3QueryShutDownException;
import com.github.theholywaffle.teamspeak3.api.reconnect.ConnectionHandler;
import com.github.theholywaffle.teamspeak3.api.reconnect.ReconnectStrategy;
import com.github.theholywaffle.teamspeak3.api.wrapper.Channel;
import com.github.theholywaffle.teamspeak3.api.wrapper.Client;
import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TeamspeakConnection implements TeamspeakGateway {

    static final int TS3_ERR_CLIENT_NICKNAME_INUSE = 513;

    private final PluginConfig config;
    private final Logger logger;
    private Runnable onReconnect;

    private TS3Query query;
    private volatile TS3Api api;
    private volatile boolean connected = false;

    public TeamspeakConnection(PluginConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void setReconnectCallback(Runnable callback) {
        this.onReconnect = callback;
    }

    record ConnectionSetup(TS3Query.Protocol protocol, boolean embedCredentials, String username, String password) {
        static ConnectionSetup from(PluginConfig config) {
            boolean ssh = "SSH".equalsIgnoreCase(config.getTsQueryProtocol());
            return new ConnectionSetup(
                    ssh ? TS3Query.Protocol.SSH : TS3Query.Protocol.RAW,
                    ssh,
                    config.getTsQueryUsername(),
                    config.getTsQueryPassword());
        }
    }

    static boolean isAlreadyInChannelError(Exception e) {
        return e instanceof TS3CommandFailedException tcfe && tcfe.getError().getId() == 770;
    }

    static TS3Config buildTs3Config(PluginConfig config, ConnectionSetup setup) {
        TS3Config ts3Config = new TS3Config();
        ts3Config.setHost(config.getTsHost());
        ts3Config.setQueryPort(config.getTsQueryPort());
        ts3Config.setFloodRate(TS3Query.FloodRate.DEFAULT);
        ts3Config.setProtocol(setup.protocol());
        if (setup.embedCredentials()) {
            ts3Config.setLoginCredentials(setup.username(), setup.password());
        }
        return ts3Config;
    }

    public void connect() {
        logger.info("Connecting to TeamSpeak ServerQuery at "
                + config.getTsHost() + ":" + config.getTsQueryPort() + "...");
        try {
            ConnectionSetup setup = ConnectionSetup.from(config);
            boolean useSsh = setup.embedCredentials();
            TS3Config ts3Config = buildTs3Config(config, setup);

            if (useSsh) {
                logger.info("Using SSH ServerQuery protocol (encrypted). "
                        + "Ensure your TS3 server has SSH ServerQuery enabled (default port 10022).");
                if (config.getTsQueryPort() == 10011) {
                    logger.warning("tsQueryPort is set to 10011 (the default RAW port). "
                            + "SSH ServerQuery typically uses port 10022. "
                            + "Update tsQueryPort in config.yml if your server uses the default SSH port.");
                }
            } else {
                logger.info("Using RAW ServerQuery protocol (plain TCP). "
                        + "Set tsQueryProtocol=SSH in config.yml for encrypted transport.");
            }

            if (config.isTsReconnectEnabled()) {
                ts3Config.setReconnectStrategy(ReconnectStrategy.exponentialBackoff());
                ts3Config.setConnectionHandler(new ConnectionHandler() {
                    @Override
                    public void onConnect(TS3Api reconnectedApi) {
                        logger.info("TeamSpeak connection (re)established. Re-initialising post-connect setup...");
                        api = reconnectedApi;
                        try {
                            if (!useSsh) {
                                api.login(config.getTsQueryUsername(), config.getTsQueryPassword());
                            }
                            if (config.getTsVirtualServerPort() > 0) {
                                api.selectVirtualServerByPort(config.getTsVirtualServerPort());
                            } else {
                                api.selectVirtualServerById(config.getTsVirtualServerId());
                            }
                            try {
                                api.setNickname(config.getTsQueryNickname());
                            } catch (TS3CommandFailedException e) {
                                if (e.getError().getId() != TS3_ERR_CLIENT_NICKNAME_INUSE) {
                                    logger.log(Level.WARNING, "Could not set nickname on reconnect.", e);
                                }
                            }
                            if (config.getTsBridgeChannelId() > 0) {
                                try {
                                    api.moveClient(api.whoAmI().getId(), config.getTsBridgeChannelId());
                                } catch (Exception moveEx) {
                                    if (!isAlreadyInChannelError(moveEx)) {
                                        logger.log(Level.WARNING, "Could not move to bridge channel on reconnect.", moveEx);
                                    }
                                }
                            }
                            if (onReconnect != null) {
                                onReconnect.run();
                            }
                            connected = true;
                        } catch (Exception e) {
                            logger.log(Level.SEVERE, "Failed to complete post-reconnect setup.", e);
                            connected = false;
                        }
                    }

                    @Override
                    public void onDisconnect(TS3Query disconnectedQuery) {
                        connected = false;
                        logger.warning("TeamSpeak connection lost — will reconnect automatically with exponential backoff. "
                                + "Use /ts status to check connection state, or /ts reload to force reconnect.");
                    }
                });
            }

            query = new TS3Query(ts3Config);
            query.connect();

            api = query.getApi();

            if (!useSsh) {
                logger.info("TCP connection established. Logging in as '" + config.getTsQueryUsername() + "'...");
                api.login(config.getTsQueryUsername(), config.getTsQueryPassword());
            } else {
                logger.info("SSH connection established as '" + config.getTsQueryUsername() + "'.");
            }

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
                if (e.getError().getId() == TS3_ERR_CLIENT_NICKNAME_INUSE) {
                    // TS3 error TS3_ERR_CLIENT_NICKNAME_INUSE = TS_ERR_CLIENT_NICKNAME_INUSE
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
                    if (isAlreadyInChannelError(moveEx)) {
                        // TS3 error 770 = already member of channel.
                        // The onConnect handler (running concurrently) moved the client first.
                        logger.info("ServerQuery client is already in bridge channel "
                                + config.getTsBridgeChannelId() + ".");
                    } else {
                        logger.log(Level.WARNING,
                                "Failed to move ServerQuery client to channel " + config.getTsBridgeChannelId()
                                        + " — the channel ID may be wrong. "
                                        + "Set tsBridgeChannelId=0 in config.yml for server-wide mode. "
                                        + "Use /ts reload after correcting the value.",
                                moveEx);
                        logAvailableChannels();
                    }
                }
            }
        } catch (Exception e) {
            connected = false;
            logger.log(Level.SEVERE,
                    "Failed to connect to TeamSpeak ServerQuery. Check the following fields in config.yml: "
                            + "tsHost ('" + config.getTsHost() + "'), "
                            + "tsQueryPort (" + config.getTsQueryPort() + "), "
                            + "tsQueryProtocol ('" + config.getTsQueryProtocol() + "'), "
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

    @Override
    public void registerAllEvents() {
        if (api == null) return;
        api.registerAllEvents();
    }

    @Override
    public String getSelfUniqueId() {
        if (api == null) return "";
        try {
            return api.whoAmI().getUniqueIdentifier();
        } catch (TS3QueryShutDownException e) {
            return "";
        }
    }

    @Override
    public Optional<TsClient> getClientInfo(int clid) {
        if (!connected || api == null) return Optional.empty();
        try {
            com.github.theholywaffle.teamspeak3.api.wrapper.Client info = api.getClientInfo(clid);
            if (info == null || info.isServerQueryClient()) return Optional.empty();
            return Optional.of(new TsClient(info.getUniqueIdentifier(), info.getNickname()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void registerBridge(TsToMcBridge bridge) {
        if (api == null) return;
        api.addTS3Listeners(new com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter() {

            @Override
            public void onTextMessage(com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent e) {
                String targetStr = e.get("target");
                int targetId = (targetStr != null && !targetStr.isEmpty()) ? Integer.parseInt(targetStr) : 0;
                bridge.handleTextMessage(
                        e.getInvokerUniqueId(),
                        Integer.parseInt(e.get("targetmode")),
                        targetId,
                        e.getInvokerName(),
                        e.getMessage());
            }

            @Override
            public void onClientJoin(com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent e) {
                bridge.handleClientJoin(
                        e.getClientType(),
                        e.getUniqueClientIdentifier(),
                        e.getInt("clid"),
                        e.get("client_nickname"),
                        e.getClientTargetId());
            }

            @Override
            public void onClientLeave(com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent e) {
                bridge.handleClientLeave(e.getInt("clid"), e.getClientFromId());
            }

            @Override
            public void onClientMoved(com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent e) {
                bridge.handleClientMoved(e.getInt("clid"), e.getInt("cfid"), e.getTargetChannelId());
            }
        });
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
        try {
            return api.getClients().stream()
                    .filter(client -> !client.isServerQueryClient())
                    .map(Client::getUniqueIdentifier)
                    .toList();
        } catch (TS3QueryShutDownException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<TsClient> getOnlineClients() {
        if (!connected) {
            return Collections.emptyList();
        }
        try {
            return api.getClients().stream()
                    .filter(client -> !client.isServerQueryClient())
                    .map(client -> new TsClient(client.getUniqueIdentifier(), client.getNickname()))
                    .toList();
        } catch (TS3QueryShutDownException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void sendServerMessage(String message) {
        if (!connected) {
            return;
        }
        try {
            api.sendServerMessage(message);
        } catch (TS3QueryShutDownException e) {
            // no-op; onDisconnect manages connected state
        }
    }

    @Override
    public void sendChannelMessage(String message) {
        if (!connected) {
            return;
        }
        try {
            api.sendChannelMessage(message);
        } catch (TS3QueryShutDownException e) {
            // no-op; onDisconnect manages connected state
        }
    }
}
