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

        assertTrue(Files.exists(tempDir.resolve("config.yml")));
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
        assertEquals("TS3Bridge", config.getTsQueryNickname());
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

    @Test
    void migratesLegacyConfigJsonToYaml() throws IOException {
        String json = "{\"tsHost\":\"old-host\",\"tsQueryPort\":9999}";
        Files.writeString(tempDir.resolve("config.json"), json);

        ConfigManager manager = new ConfigManager(tempDir, LOGGER);
        manager.load();

        assertTrue(Files.exists(tempDir.resolve("config.yml")));
        assertTrue(Files.exists(tempDir.resolve("config.json.bak")));
        assertFalse(Files.exists(tempDir.resolve("config.json")));
        assertEquals("old-host", manager.getConfig().getTsHost());
        assertEquals(9999, manager.getConfig().getTsQueryPort());
    }

    @Test
    void loadsDefaultsWhenNoConfigExists() {
        ConfigManager manager = new ConfigManager(tempDir, LOGGER);
        manager.load();

        assertEquals("localhost", manager.getConfig().getTsHost());
        assertEquals(10011, manager.getConfig().getTsQueryPort());
        assertTrue(Files.exists(tempDir.resolve("config.yml")));
    }

    @Test
    void loadsExistingYamlConfig() throws IOException {
        String yaml = "tsHost: 'ts.example.com'\ntsQueryPort: 10022\ntsQueryProtocol: 'SSH'\n";
        Files.writeString(tempDir.resolve("config.yml"), yaml);

        ConfigManager manager = new ConfigManager(tempDir, LOGGER);
        manager.load();

        assertEquals("ts.example.com", manager.getConfig().getTsHost());
        assertEquals(10022, manager.getConfig().getTsQueryPort());
        assertEquals("SSH", manager.getConfig().getTsQueryProtocol());
    }

    @Test
    void ignoresJsonWhenYamlAlreadyExists() throws IOException {
        Files.writeString(tempDir.resolve("config.yml"), "tsHost: 'yaml-host'\n");
        Files.writeString(tempDir.resolve("config.json"), "{\"tsHost\":\"json-host\"}");

        ConfigManager manager = new ConfigManager(tempDir, LOGGER);
        manager.load();

        assertEquals("yaml-host", manager.getConfig().getTsHost());
    }
}
