package de.thomasuebel.mc.ts3bridge.configuration;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {

    private static final String CONFIG_YML = "config.yml";
    private static final String MAPPINGS_FILE = "mappings.json";

    private final Path dataFolder;
    private final Logger logger;
    private PluginConfig config;

    public ConfigManager(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    public void load() {
        loadConfig();
        bootstrapMappings();
    }

    public void save() {
        saveConfig();
    }

    public PluginConfig getConfig() {
        return config;
    }

    private void loadConfig() {
        File configFile = dataFolder.resolve(CONFIG_YML).toFile();
        if (!configFile.exists()) {
            config = new PluginConfig();
            saveConfig();
            logger.info("Created default config.yml — configure your TeamSpeak connection settings "
                    + "in the plugin data folder before restarting.");
            return;
        }

        try {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
            config = new PluginConfig();
            config.setTsHost(yaml.getString("tsHost", "localhost"));
            config.setTsQueryPort(yaml.getInt("tsQueryPort", 10011));
            config.setTsQueryProtocol(yaml.getString("tsQueryProtocol", "RAW"));
            config.setTsQueryUsername(yaml.getString("tsQueryUsername", ""));
            config.setTsQueryPassword(yaml.getString("tsQueryPassword", ""));
            config.setTsVirtualServerId(yaml.getInt("tsVirtualServerId", 1));
            config.setTsVirtualServerPort(yaml.getInt("tsVirtualServerPort", 0));
            config.setTsServerAddress(yaml.getString("tsServerAddress", "localhost"));
            config.setTsQueryNickname(yaml.getString("tsQueryNickname", "TS3Bridge"));
            config.setTsBridgeChannelId(yaml.getInt("tsBridgeChannelId", 0));
            config.setAdvertisementMessage(yaml.getString("advertisementMessage",
                    "Join our TeamSpeak server: {address}"));
            config.setChatBridgeEnabled(yaml.getBoolean("chatBridgeEnabled", true));
            config.setTsReconnectEnabled(yaml.getBoolean("tsReconnectEnabled", true));
            config.setDebugLogging(yaml.getBoolean("debugLogging", false));
            logger.info("Loaded config.yml.");
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Failed to read config.yml — is it valid YAML? Using defaults. "
                            + "Fix or delete the file and use /ts reload to retry.", e);
            config = new PluginConfig();
        }
    }

    private void saveConfig() {
        File configFile = dataFolder.resolve(CONFIG_YML).toFile();
        YamlConfiguration yaml = new YamlConfiguration();

        yaml.set("tsHost", config.getTsHost());
        yaml.set("tsQueryPort", config.getTsQueryPort());
        yaml.set("tsQueryProtocol", config.getTsQueryProtocol());
        yaml.set("tsQueryUsername", config.getTsQueryUsername());
        yaml.set("tsQueryPassword", config.getTsQueryPassword());
        yaml.set("tsVirtualServerId", config.getTsVirtualServerId());
        yaml.set("tsVirtualServerPort", config.getTsVirtualServerPort());
        yaml.set("tsServerAddress", config.getTsServerAddress());
        yaml.set("tsQueryNickname", config.getTsQueryNickname());
        yaml.set("tsBridgeChannelId", config.getTsBridgeChannelId());
        yaml.set("advertisementMessage", config.getAdvertisementMessage());
        yaml.set("chatBridgeEnabled", config.isChatBridgeEnabled());
        yaml.set("tsReconnectEnabled", config.isTsReconnectEnabled());
        yaml.set("debugLogging", config.isDebugLogging());

        yaml.setComments("tsHost", List.of("TeamSpeak server hostname or IP"));
        yaml.setComments("tsQueryPort", List.of("ServerQuery TCP port (default 10011 for RAW, 10022 for SSH)"));
        yaml.setComments("tsQueryProtocol", List.of(
                "Connection protocol: RAW (plain TCP) or SSH (encrypted).",
                "Use SSH when the Minecraft server and TS3 server are on different hosts."));
        yaml.setComments("tsQueryUsername", List.of("ServerQuery login name"));
        yaml.setComments("tsQueryPassword", List.of("ServerQuery password — keep this file out of version control"));
        yaml.setComments("tsVirtualServerId", List.of("Virtual server ID (used when tsVirtualServerPort is 0)"));
        yaml.setComments("tsVirtualServerPort", List.of(
                "Voice port of the virtual server (e.g. 9987).",
                "When > 0, overrides tsVirtualServerId. Use this for 4netplayers and similar hosts."));
        yaml.setComments("tsServerAddress", List.of("Address shown to players in the join advertisement"));
        yaml.setComments("tsQueryNickname", List.of("Display name of the ServerQuery bot in TeamSpeak"));
        yaml.setComments("tsBridgeChannelId", List.of(
                "Channel ID for the chat bridge. 0 = server-wide (all channels)."));
        yaml.setComments("advertisementMessage", List.of(
                "Message sent to unlinked players on join. Use {address} as a placeholder."));
        yaml.setComments("chatBridgeEnabled", List.of("Set to false to disable the MC<->TS chat relay"));
        yaml.setComments("tsReconnectEnabled", List.of(
                "When true, the plugin reconnects automatically after a connection drop.",
                "Set to false to require manual /ts reload after every disconnect."));
        yaml.setComments("debugLogging", List.of(
                "When true, raw TS event payloads are logged at FINE level.",
                "Enable with a Java logging configuration change to see output."));

        try {
            yaml.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config.yml", e);
        }
    }

    private void bootstrapMappings() {
        Path mappingsPath = dataFolder.resolve(MAPPINGS_FILE);
        if (!Files.exists(mappingsPath)) {
            try (var writer = Files.newBufferedWriter(mappingsPath)) {
                writer.write("{}");
                logger.info("Created empty mappings.json.");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create mappings.json", e);
            }
        }
    }
}
