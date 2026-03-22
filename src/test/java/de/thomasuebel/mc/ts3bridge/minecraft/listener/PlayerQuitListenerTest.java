package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerQuitListenerTest {

    @Mock Player player;

    private FakeTeamspeakGateway gateway;
    private ChatBridgeService chatBridgeService;

    @BeforeEach
    void setUp() {
        gateway = new FakeTeamspeakGateway();
        gateway.setConnected(true);
        chatBridgeService = new ChatBridgeService(gateway, 0);
    }

    @Test
    void quitIsRelayedToTeamspeakWhenBridgeEnabled() {
        when(player.getName()).thenReturn("Steve");
        var listener = new PlayerQuitListener(chatBridgeService, true);
        var event = new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null);

        listener.onPlayerQuit(event);

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Steve left the server", gateway.getSentMessages().get(0));
    }

    @Test
    void quitIsNotRelayedToTeamspeakWhenBridgeDisabled() {
        var listener = new PlayerQuitListener(chatBridgeService, false);
        var event = new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null);

        listener.onPlayerQuit(event);

        assertTrue(gateway.getSentMessages().isEmpty());
    }
}
