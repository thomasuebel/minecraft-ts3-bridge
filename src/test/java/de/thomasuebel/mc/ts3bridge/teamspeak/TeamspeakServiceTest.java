package de.thomasuebel.mc.ts3bridge.teamspeak;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamspeakServiceTest {

    @Mock
    private TeamspeakGateway gateway;

    private TeamspeakService service;

    @BeforeEach
    void setUp() {
        service = new TeamspeakService(gateway);
    }

    @Test
    void isConnectedDelegatesToGateway() {
        when(gateway.isConnected()).thenReturn(true);

        assertTrue(service.isConnected());
    }

    @Test
    void isConnectedReturnsFalseWhenDisconnected() {
        when(gateway.isConnected()).thenReturn(false);

        assertFalse(service.isConnected());
    }

    @Test
    void getOnlineClientUidsDelegatesToGateway() {
        when(gateway.getOnlineClientUids()).thenReturn(List.of("uid-abc", "uid-def"));

        List<String> uids = service.getOnlineClientUids();

        assertEquals(List.of("uid-abc", "uid-def"), uids);
    }

    @Test
    void getOnlineClientUidsReturnsEmptyListWhenNoneOnline() {
        when(gateway.getOnlineClientUids()).thenReturn(Collections.emptyList());

        assertTrue(service.getOnlineClientUids().isEmpty());
    }

    @Test
    void sendChannelMessageDelegatesToGateway() {
        service.sendChannelMessage("Hello from Minecraft!");

        verify(gateway).sendServerMessage("Hello from Minecraft!");
    }

    @Test
    void getOnlineClientsDelegatesToGateway() {
        var clients = List.of(new TsClient("uid-1", "Alice"), new TsClient("uid-2", "Bob"));
        when(gateway.getOnlineClients()).thenReturn(clients);

        assertEquals(clients, service.getOnlineClients());
    }

    @Test
    void findOnlineClientsByNicknameReturnsExactCaseInsensitiveMatch() {
        when(gateway.getOnlineClients()).thenReturn(List.of(
                new TsClient("uid-1", "Alice"),
                new TsClient("uid-2", "Bob")));

        var result = service.findOnlineClientsByNickname("alice");

        assertEquals(1, result.size());
        assertEquals("uid-1", result.get(0).uid());
    }

    @Test
    void findOnlineClientsByNicknameReturnsEmptyWhenNoMatch() {
        when(gateway.getOnlineClients()).thenReturn(List.of(new TsClient("uid-1", "Alice")));

        assertTrue(service.findOnlineClientsByNickname("Charlie").isEmpty());
    }

    @Test
    void findOnlineClientsByNicknameReturnsAllMatchesWhenAmbiguous() {
        when(gateway.getOnlineClients()).thenReturn(List.of(
                new TsClient("uid-1", "Steve"),
                new TsClient("uid-2", "steve"),
                new TsClient("uid-3", "Bob")));

        var result = service.findOnlineClientsByNickname("Steve");

        assertEquals(2, result.size());
    }
}
