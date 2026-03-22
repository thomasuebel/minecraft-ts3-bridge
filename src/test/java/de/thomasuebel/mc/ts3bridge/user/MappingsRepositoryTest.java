package de.thomasuebel.mc.ts3bridge.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class MappingsRepositoryTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path tempDir;

    private MappingsRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        // Pre-create mappings.json as ConfigManager would on first start
        Files.writeString(tempDir.resolve("mappings.json"), "{}");
        repository = new MappingsRepository(tempDir, LOGGER);
        repository.load();
    }

    @Test
    void noMappingFoundForUnlinkedPlayer() {
        UUID uuid = UUID.randomUUID();
        assertTrue(repository.findTsUidByMinecraftUuid(uuid).isEmpty());
    }

    @Test
    void mappingIsStoredAndRetrieved() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-abc");

        Optional<String> result = repository.findTsUidByMinecraftUuid(uuid);
        assertTrue(result.isPresent());
        assertEquals("ts-uid-abc", result.get());
    }

    @Test
    void unlinkRemovesMapping() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-abc");
        repository.unlink(uuid);

        assertTrue(repository.findTsUidByMinecraftUuid(uuid).isEmpty());
    }

    @Test
    void unlinkingNonExistentPlayerDoesNotThrow() {
        UUID uuid = UUID.randomUUID();
        assertDoesNotThrow(() -> repository.unlink(uuid));
    }

    @Test
    void mappingsArePersistedToDisk() throws IOException {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-persist");
        repository.save();

        MappingsRepository reloaded = new MappingsRepository(tempDir, LOGGER);
        reloaded.load();

        assertEquals("ts-uid-persist", reloaded.findTsUidByMinecraftUuid(uuid).orElse(null));
    }

    @Test
    void isLinkedReturnsTrueForLinkedPlayer() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-abc");

        assertTrue(repository.isLinked(uuid));
    }

    @Test
    void isLinkedReturnsFalseForUnlinkedPlayer() {
        assertFalse(repository.isLinked(UUID.randomUUID()));
    }

    @Test
    void findMinecraftUuidByTsUid() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-lookup");

        Optional<UUID> result = repository.findMinecraftUuidByTsUid("ts-uid-lookup");
        assertTrue(result.isPresent());
        assertEquals(uuid, result.get());
    }
}
