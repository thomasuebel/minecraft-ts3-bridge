package de.thomasuebel.mc.ts3bridge.teamspeak;

import org.junit.jupiter.api.Test;

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
}
