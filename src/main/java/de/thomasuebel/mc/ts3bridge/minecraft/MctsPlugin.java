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
import de.thomasuebel.mc.ts3bridge.teamspeak.TsToMcBridge;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;
import de.thomasuebel.mc.ts3bridge.user.UserLinkService;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public class MctsPlugin extends JavaPlugin {

    private static final int BSTATS_PLUGIN_ID = 30483;

    private TeamspeakConnection teamspeakConnection;
    private TeamspeakService teamspeakService;
    private ConfigManager configManager;
    private MappingsRepository mappingsRepository;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        new Metrics(this, BSTATS_PLUGIN_ID);
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
                    + "Fix config.yml and use /ts reload to retry.");
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
                name -> java.util.Optional.ofNullable(getServer().getPlayerExact(name))
                        .map(p -> (java.util.UUID) p.getUniqueId())
                        .or(() -> {
                            org.bukkit.OfflinePlayer op = getServer().getOfflinePlayerIfCached(name);
                            return op != null && op.hasPlayedBefore() ? java.util.Optional.of(op.getUniqueId()) : java.util.Optional.empty();
                        }),
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
            getLogger().info("Chat bridge is DISABLED (set chatBridgeEnabled=true in config.yml to enable).");
        }

        pluginManager.registerEvents(
                new PlayerJoinListener(advertisementService, chatBridgeService,
                        config.isChatBridgeEnabled(), getLogger()), this);
        getLogger().info("Registered player join listener.");

        pluginManager.registerEvents(
                new PlayerQuitListener(chatBridgeService, config.isChatBridgeEnabled()), this);
        getLogger().info("Registered player quit listener.");

        // --- TS→MC chat bridge ---
        TsToMcBridge tsToMcBridge = new TsToMcBridge(
                teamspeakConnection, chatBridgeService,
                config.isDebugLogging(), getLogger(),
                msg -> getServer().getScheduler().runTask(this,
                        () -> getServer().broadcast(net.kyori.adventure.text.Component.text(msg))));
        teamspeakConnection.setReconnectCallback(tsToMcBridge::register);
        if (teamspeakService.isConnected()) {
            tsToMcBridge.register();
        } else {
            getLogger().warning("TS→MC chat bridge not registered — not connected to TeamSpeak.");
        }

        getLogger().info("TS3Bridge enabled successfully.");
    }
}
