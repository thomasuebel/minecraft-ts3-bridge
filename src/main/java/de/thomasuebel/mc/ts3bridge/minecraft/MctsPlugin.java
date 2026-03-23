package de.thomasuebel.mc.ts3bridge.minecraft;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import de.thomasuebel.mc.ts3bridge.configuration.ConfigManager;
import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.minecraft.command.TsCommand;
import de.thomasuebel.mc.ts3bridge.minecraft.listener.MinecraftChatListener;
import de.thomasuebel.mc.ts3bridge.minecraft.listener.PlayerJoinListener;
import de.thomasuebel.mc.ts3bridge.minecraft.listener.PlayerQuitListener;
import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakConnection;
import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakService;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;
import de.thomasuebel.mc.ts3bridge.user.UserLinkService;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.logging.Level;

public class MctsPlugin extends JavaPlugin {

    private TeamspeakConnection teamspeakConnection;
    private TeamspeakService teamspeakService;
    private ConfigManager configManager;
    private MappingsRepository mappingsRepository;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        initialize();
    }

    @Override
    public void onDisable() {
        getLogger().info("Shutting down TS3Bridge...");

        if (mappingsRepository != null) {
            getLogger().info("Saving player mappings...");
            mappingsRepository.save();
        }
        if (teamspeakConnection != null) {
            teamspeakConnection.disconnect();
        }

        getLogger().info("TS3Bridge disabled.");
    }

    public void reload() {
        getLogger().info("Reloading TS3Bridge...");

        if (mappingsRepository != null) {
            getLogger().info("Saving player mappings...");
            mappingsRepository.save();
        }
        if (teamspeakConnection != null) {
            teamspeakConnection.disconnect();
        }

        HandlerList.unregisterAll(this);

        initialize();

        getLogger().info("TS3Bridge reload complete.");
    }

    private void initialize() {
        Path dataFolder = getDataFolder().toPath();

        // --- Configuration ---
        getLogger().info("Loading configuration...");
        configManager = new ConfigManager(dataFolder, getLogger());
        configManager.load();
        PluginConfig config = configManager.getConfig();

        // --- Persistence ---
        getLogger().info("Loading player mappings...");
        mappingsRepository = new MappingsRepository(dataFolder, getLogger());
        mappingsRepository.load();

        // --- TeamSpeak ---
        teamspeakConnection = new TeamspeakConnection(config, getLogger());
        teamspeakConnection.connect();
        teamspeakService = new TeamspeakService(teamspeakConnection);

        if (!teamspeakService.isConnected()) {
            getLogger().warning("Plugin is running without a TeamSpeak connection. "
                    + "Commands and chat bridge will be unavailable until the connection is restored. "
                    + "Fix config.json and use /ts reload to retry.");
        }

        // --- Services ---
        UserLinkService userLinkService = new UserLinkService(mappingsRepository, teamspeakConnection, getLogger());
        ChatBridgeService chatBridgeService = new ChatBridgeService(teamspeakConnection, config.getTsBridgeChannelId());
        AdvertisementService advertisementService = new AdvertisementService(config, mappingsRepository);

        // --- Commands ---
        getLogger().info("Registering commands...");
        var tsCommand = new TsCommand(
                teamspeakService, userLinkService, mappingsRepository,
                this::reload,
                uuid -> {
                    org.bukkit.OfflinePlayer op = getServer().getOfflinePlayer(uuid);
                    String name = op.getName();
                    return name != null ? name : uuid.toString();
                },
                name -> java.util.Optional.ofNullable(getServer().getPlayerExact(name)),
                advertisementService::buildAdvertisementMessage,
                config
        );
        var commandMeta = getCommand("ts");
        if (commandMeta != null) {
            commandMeta.setExecutor(tsCommand);
            commandMeta.setTabCompleter(tsCommand);
            getLogger().info("Registered /ts command.");
        } else {
            getLogger().severe("Failed to register /ts command — is it declared in plugin.yml?");
        }

        // --- Listeners ---
        getLogger().info("Registering event listeners...");
        var pluginManager = getServer().getPluginManager();

        if (config.isChatBridgeEnabled()) {
            pluginManager.registerEvents(new MinecraftChatListener(chatBridgeService, true), this);
            getLogger().info("Chat bridge is ENABLED — Minecraft chat will be relayed to TeamSpeak.");
        } else {
            pluginManager.registerEvents(new MinecraftChatListener(chatBridgeService, false), this);
            getLogger().info("Chat bridge is DISABLED (set chatBridgeEnabled=true in config.json to enable).");
        }

        pluginManager.registerEvents(
                new PlayerJoinListener(advertisementService, chatBridgeService,
                        config.isChatBridgeEnabled(), getLogger()), this);
        getLogger().info("Registered player join listener.");

        pluginManager.registerEvents(
                new PlayerQuitListener(chatBridgeService, config.isChatBridgeEnabled()), this);
        getLogger().info("Registered player quit listener.");

        // --- TS→MC chat bridge ---
        registerTsToMcBridge(chatBridgeService, config.isDebugLogging());

        getLogger().info("TS3Bridge enabled successfully.");
    }

    /**
     * Registers a ServerQuery event listener for incoming TeamSpeak channel text messages
     * and broadcasts them to Minecraft chat.
     */
    private void registerTsToMcBridge(ChatBridgeService chatBridgeService, boolean debugLogging) {
        if (!teamspeakService.isConnected()) {
            getLogger().warning("TS→MC chat bridge not registered — not connected to TeamSpeak.");
            return;
        }

        try {
            teamspeakConnection.getApi().registerAllEvents();
            String selfUid = teamspeakConnection.getApi().whoAmI().getUniqueIdentifier();

            // Cache clid→nickname so onClientLeave can show the name after the client
            // has already disconnected (notifyclientleftview never carries client_nickname).
            java.util.concurrent.ConcurrentHashMap<Integer, String> clientNicknames =
                    new java.util.concurrent.ConcurrentHashMap<>();

            teamspeakConnection.getApi().addTS3Listeners(
                    new com.github.theholywaffle.teamspeak3.api.event.TS3EventAdapter() {

                        @Override
                        public void onTextMessage(
                                com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent e) {
                            try {
                                if (debugLogging) getLogger().fine("[TS Bridge] onTextMessage fired — raw event: "
                                        + e);
                                if (e.getInvokerUniqueId().equals(selfUid)) return;
                                int targetMode = Integer.parseInt(e.get("targetmode"));
                                String targetStr = e.get("target");
                                int targetId = (targetStr != null && !targetStr.isEmpty())
                                        ? Integer.parseInt(targetStr) : 0;
                                if (debugLogging) getLogger().fine("[TS Bridge] targetMode=" + targetMode
                                        + " targetId=" + targetId + " channelId config="
                                        + chatBridgeService.getChannelId());
                                if (!chatBridgeService.shouldRelayFromTeamspeak(targetMode, targetId)) {
                                    if (debugLogging) getLogger().fine("[TS Bridge] Message filtered by shouldRelayFromTeamspeak.");
                                    return;
                                }
                                String formatted = chatBridgeService.formatTeamspeakMessage(
                                        e.getInvokerName(), e.getMessage());
                                broadcast(formatted);
                            } catch (Exception ex) {
                                getLogger().log(Level.SEVERE,
                                        "Unexpected error while processing incoming TeamSpeak message.", ex);
                            }
                        }

                        @Override
                        public void onClientJoin(
                                com.github.theholywaffle.teamspeak3.api.event.ClientJoinEvent e) {
                            try {
                                if (debugLogging) getLogger().fine("[TS Bridge] onClientJoin fired — raw event: "
                                        + e);
                                if (e.getClientType() == 1) return;
                                if (selfUid.equals(e.getUniqueClientIdentifier())) return;
                                String nickname = e.get("client_nickname");
                                clientNicknames.put(e.getInt("clid"), nickname);
                                if (!chatBridgeService.shouldRelayTsStatusEvent(e.getClientTargetId())) return;
                                broadcast(chatBridgeService.formatTsClientJoined(nickname));
                            } catch (Exception ex) {
                                getLogger().log(Level.SEVERE,
                                        "Unexpected error while processing TeamSpeak client join.", ex);
                            }
                        }

                        @Override
                        public void onClientLeave(
                                com.github.theholywaffle.teamspeak3.api.event.ClientLeaveEvent e) {
                            try {
                                if (debugLogging) getLogger().fine("[TS Bridge] onClientLeave fired — raw event: "
                                        + e);
                                // notifyclientleftview does not include client_nickname or client_type;
                                // use the nickname cache (populated on join/move). Unknown clients
                                // (ServerQuery, or clients we never saw join) are naturally excluded.
                                int clid = e.getInt("clid");
                                String nickname = clientNicknames.remove(clid);
                                if (nickname == null) return;
                                if (!chatBridgeService.shouldRelayTsStatusEvent(e.getClientFromId())) return;
                                broadcast(chatBridgeService.formatTsClientLeft(nickname));
                            } catch (Exception ex) {
                                getLogger().log(Level.SEVERE,
                                        "Unexpected error while processing TeamSpeak client leave.", ex);
                            }
                        }

                        @Override
                        public void onClientMoved(
                                com.github.theholywaffle.teamspeak3.api.event.ClientMovedEvent e) {
                            try {
                                if (debugLogging) getLogger().fine("[TS Bridge] onClientMoved fired — raw event: "
                                        + e);
                                // notifyclientmoved does not include client_type or client_nickname;
                                // fetch client info from ServerQuery to filter query clients and get
                                // the display name.
                                int clid = e.getInt("clid");
                                com.github.theholywaffle.teamspeak3.api.wrapper.Client info =
                                        teamspeakConnection.getApi().getClientInfo(clid);
                                if (info == null || info.isServerQueryClient()) return;
                                if (selfUid.equals(info.getUniqueIdentifier())) return;
                                String nickname = info.getNickname();
                                clientNicknames.put(clid, nickname);
                                int fromChannel = e.getInt("cfid");
                                int toChannel = e.getTargetChannelId();
                                if (chatBridgeService.shouldRelayTsStatusEvent(toChannel)) {
                                    broadcast(chatBridgeService.formatTsClientJoined(nickname));
                                } else if (chatBridgeService.shouldRelayTsStatusEvent(fromChannel)) {
                                    broadcast(chatBridgeService.formatTsClientLeft(nickname));
                                }
                            } catch (Exception ex) {
                                getLogger().log(Level.SEVERE,
                                        "Unexpected error while processing TeamSpeak client move.", ex);
                            }
                        }

                        private void broadcast(String message) {
                            getServer().getScheduler().runTask(MctsPlugin.this,
                                    () -> getServer().broadcast(
                                            net.kyori.adventure.text.Component.text(message)));
                        }
                    });
            getLogger().info("TS→MC chat bridge registered — TeamSpeak messages will appear in Minecraft chat.");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE,
                    "Failed to register TS→MC chat bridge event listener. "
                            + "TeamSpeak messages will not appear in Minecraft chat.",
                    e);
        }
    }
}
