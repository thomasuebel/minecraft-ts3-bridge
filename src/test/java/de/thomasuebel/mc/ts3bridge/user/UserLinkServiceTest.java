package de.thomasuebel.mc.ts3bridge.user;

import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UserLinkServiceTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path tempDir;

    private MappingsRepository repository;
    private FakeTeamspeakGateway gateway;
    private UserLinkService service;

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(tempDir.resolve("mappings.json"), "{}");
        repository = new MappingsRepository(tempDir, LOGGER);
        repository.load();
        gateway = new FakeTeamspeakGateway();
        service = new UserLinkService(repository, gateway, LOGGER);
    }

    @Test
    void linkSucceedsWhenTsUidIsOnline() {
        UUID uuid = UUID.randomUUID();
        gateway.setConnected(true);
        gateway.addOnlineUid("ts-uid-online");

        LinkResult result = service.link(uuid, "ts-uid-online");

        assertEquals(LinkResult.SUCCESS, result);
        assertTrue(repository.isLinked(uuid));
    }

    @Test
    void linkFailsWhenTsUidIsNotOnline() {
        UUID uuid = UUID.randomUUID();
        gateway.setConnected(true);
        // no uids added — nobody is online

        LinkResult result = service.link(uuid, "ts-uid-nobody");

        assertEquals(LinkResult.TS_UID_NOT_ONLINE, result);
        assertFalse(repository.isLinked(uuid));
    }

    @Test
    void linkFailsWhenAlreadyLinked() {
        UUID uuid = UUID.randomUUID();
        gateway.setConnected(true);
        gateway.addOnlineUid("ts-uid-online");
        repository.link(uuid, "ts-uid-online");

        LinkResult result = service.link(uuid, "ts-uid-online");

        assertEquals(LinkResult.ALREADY_LINKED, result);
    }

    @Test
    void linkFailsWhenTeamspeakIsDisconnected() {
        UUID uuid = UUID.randomUUID();
        gateway.setConnected(false);

        LinkResult result = service.link(uuid, "ts-uid-online");

        assertEquals(LinkResult.TEAMSPEAK_UNAVAILABLE, result);
        assertFalse(repository.isLinked(uuid));
    }

    @Test
    void unlinkSucceedsWhenLinked() {
        UUID uuid = UUID.randomUUID();
        repository.link(uuid, "ts-uid-abc");

        UnlinkResult result = service.unlink(uuid);

        assertEquals(UnlinkResult.SUCCESS, result);
        assertFalse(repository.isLinked(uuid));
    }

    @Test
    void unlinkFailsWhenNotLinked() {
        UUID uuid = UUID.randomUUID();

        UnlinkResult result = service.unlink(uuid);

        assertEquals(UnlinkResult.NOT_LINKED, result);
    }
}
