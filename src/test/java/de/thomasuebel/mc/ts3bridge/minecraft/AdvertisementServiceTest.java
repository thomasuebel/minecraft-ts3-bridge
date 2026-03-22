package de.thomasuebel.mc.ts3bridge.minecraft;

import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class AdvertisementServiceTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path tempDir;

    private PluginConfig config;
    private MappingsRepository repository;
    private AdvertisementService service;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(tempDir.resolve("mappings.json"), "{}");
        repository = new MappingsRepository(tempDir, LOGGER);
        repository.load();

        config = new PluginConfig();
        config.setTsServerAddress("ts.example.com");

        service = new AdvertisementService(config, repository);
    }

    @Test
    void advertisementMessageContainsTsAddress() {
        assertTrue(service.buildAdvertisementMessage().contains("ts.example.com"));
    }

    @Test
    void advertisementMessageRespectsTemplate() {
        config.setAdvertisementMessage("Connect to TS at {address}!");
        assertEquals("Connect to TS at ts.example.com!", service.buildAdvertisementMessage());
    }

    @Test
    void shouldAdvertiseReturnsTrueForUnlinkedPlayer() {
        assertTrue(service.shouldAdvertise(UUID.randomUUID()));
    }

    @Test
    void shouldAdvertiseReturnsFalseForLinkedPlayer() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-abc");

        assertFalse(service.shouldAdvertise(uuid));
    }
}
