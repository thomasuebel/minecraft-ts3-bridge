package de.thomasuebel.mc.ts3bridge.configuration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path tempDir;

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = new ConfigManager(tempDir, LOGGER);
    }

    @Test
    void configFileIsCreatedWithDefaultsWhenMissing() throws IOException {
        configManager.load();

        assertTrue(Files.exists(tempDir.resolve("config.json")));
    }

    @Test
    void mappingsFileIsCreatedEmptyWhenMissing() throws IOException {
        configManager.load();

        assertTrue(Files.exists(tempDir.resolve("mappings.json")));
    }

    @Test
    void defaultConfigHasExpectedValues() {
        configManager.load();

        PluginConfig config = configManager.getConfig();
        assertEquals("localhost", config.getTsHost());
        assertEquals(10011, config.getTsQueryPort());
        assertEquals(1, config.getTsVirtualServerId());
        assertTrue(config.isChatBridgeEnabled());
        assertEquals("MinecraftTS", config.getTsQueryNickname());
        assertEquals(0, config.getTsBridgeChannelId());
    }

    @Test
    void configIsPersisted() throws IOException {
        configManager.load();
        PluginConfig config = configManager.getConfig();
        config.setTsHost("ts.example.com");
        config.setTsQueryPort(9987);
        configManager.save();

        ConfigManager reloaded = new ConfigManager(tempDir, LOGGER);
        reloaded.load();
        assertEquals("ts.example.com", reloaded.getConfig().getTsHost());
        assertEquals(9987, reloaded.getConfig().getTsQueryPort());
    }

    @Test
    void existingConfigIsLoadedNotOverwritten() throws IOException {
        configManager.load();
        configManager.getConfig().setTsHost("ts.example.com");
        configManager.save();

        ConfigManager second = new ConfigManager(tempDir, LOGGER);
        second.load();

        assertEquals("ts.example.com", second.getConfig().getTsHost());
    }
}
