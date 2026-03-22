package de.thomasuebel.mc.ts3bridge.configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.json";
    private static final String MAPPINGS_FILE = "mappings.json";

    private final Path dataFolder;
    private final Gson gson;
    private final Logger logger;
    private PluginConfig config;

    public ConfigManager(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
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
        Path configPath = dataFolder.resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                config = gson.fromJson(reader, PluginConfig.class);
                logger.info("Loaded config.json.");
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Failed to read config.json — is it valid JSON? Using defaults. Fix or delete the file and restart.",
                        e);
                config = new PluginConfig();
            }
        } else {
            config = new PluginConfig();
            saveConfig();
            logger.info("Created default config.json — configure your TeamSpeak connection settings in the plugin data folder before restarting.");
        }
    }

    private void saveConfig() {
        Path configPath = dataFolder.resolve(CONFIG_FILE);
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config.json", e);
        }
    }

    private void bootstrapMappings() {
        Path mappingsPath = dataFolder.resolve(MAPPINGS_FILE);
        if (!Files.exists(mappingsPath)) {
            try (Writer writer = Files.newBufferedWriter(mappingsPath)) {
                writer.write("{}");
                logger.info("Created empty mappings.json.");
            } catch (IOException e) {
                throw new RuntimeException("Failed to create mappings.json", e);
            }
        }
    }
}
