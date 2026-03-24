package de.thomasuebel.mc.ts3bridge.teamspeak;

import com.github.theholywaffle.teamspeak3.TS3Query;
import com.github.theholywaffle.teamspeak3.api.exception.TS3CommandFailedException;
import com.github.theholywaffle.teamspeak3.api.wrapper.QueryError;
import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeamspeakConnectionTest {

    @Test
    void isNotConnectedByDefault() {
        // TeamspeakConnection requires a live TS3 server to fully test.
        // Integration-level behaviour (connect/disconnect) is not unit-tested here.
        // This test documents the initial state contract.
        FakeTeamspeakGateway gateway = new FakeTeamspeakGateway();
        assertFalse(gateway.isConnected());
    }

    @Test
    void getOnlineClientUidsReturnsEmptyWhenDisconnected() {
        FakeTeamspeakGateway gateway = new FakeTeamspeakGateway();

        assertTrue(gateway.getOnlineClientUids().isEmpty());
    }

    @Test
    void sendServerMessageDoesNothingWhenDisconnected() {
        FakeTeamspeakGateway gateway = new FakeTeamspeakGateway();

        // should not throw
        assertDoesNotThrow(() -> gateway.sendServerMessage("test"));
    }

    // --- ConnectionSetup.from() tests ---

    @Test
    void connectionSetupSshEmbedsCredentials() {
        PluginConfig config = new PluginConfig();
        config.setTsQueryProtocol("SSH");
        config.setTsQueryUsername("ts3bridge");
        config.setTsQueryPassword("secret");

        TeamspeakConnection.ConnectionSetup setup = TeamspeakConnection.ConnectionSetup.from(config);

        assertEquals(TS3Query.Protocol.SSH, setup.protocol());
        assertTrue(setup.embedCredentials());
        assertEquals("ts3bridge", setup.username());
        assertEquals("secret", setup.password());
    }

    @Test
    void connectionSetupSshIsCaseInsensitive() {
        PluginConfig config = new PluginConfig();
        config.setTsQueryProtocol("ssh");

        TeamspeakConnection.ConnectionSetup setup = TeamspeakConnection.ConnectionSetup.from(config);

        assertEquals(TS3Query.Protocol.SSH, setup.protocol());
        assertTrue(setup.embedCredentials());
    }

    @Test
    void connectionSetupRawDoesNotEmbedCredentials() {
        PluginConfig config = new PluginConfig();
        config.setTsQueryProtocol("RAW");
        config.setTsQueryUsername("user");
        config.setTsQueryPassword("pass");

        TeamspeakConnection.ConnectionSetup setup = TeamspeakConnection.ConnectionSetup.from(config);

        assertEquals(TS3Query.Protocol.RAW, setup.protocol());
        assertFalse(setup.embedCredentials());
    }

    @Test
    void connectionSetupDefaultProtocolIsRaw() {
        PluginConfig config = new PluginConfig(); // default protocol is "RAW"

        TeamspeakConnection.ConnectionSetup setup = TeamspeakConnection.ConnectionSetup.from(config);

        assertEquals(TS3Query.Protocol.RAW, setup.protocol());
        assertFalse(setup.embedCredentials());
    }

    // --- isAlreadyInChannelError tests ---

    @Test
    void isAlreadyInChannelErrorReturnsTrueFor770() {
        assertTrue(TeamspeakConnection.isAlreadyInChannelError(makeCommandFailedException(770)));
    }

    @Test
    void isAlreadyInChannelErrorReturnsFalseForOtherTs3ErrorCodes() {
        assertFalse(TeamspeakConnection.isAlreadyInChannelError(makeCommandFailedException(768)));
    }

    @Test
    void isAlreadyInChannelErrorReturnsFalseForNonTs3Exceptions() {
        assertFalse(TeamspeakConnection.isAlreadyInChannelError(new RuntimeException("network error")));
    }

    private static TS3CommandFailedException makeCommandFailedException(int errorId) {
        QueryError error = new QueryError(Map.of("id", String.valueOf(errorId), "msg", "error"));
        return new TS3CommandFailedException(error, "clientmove");
    }
}
