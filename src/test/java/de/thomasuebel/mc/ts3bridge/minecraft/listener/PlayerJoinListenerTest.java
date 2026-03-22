package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import de.thomasuebel.mc.ts3bridge.minecraft.FakeAdvertisementService;
import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerJoinListenerTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @Mock Player player;

    private FakeAdvertisementService advertisementService;
    private FakeTeamspeakGateway gateway;
    private ChatBridgeService chatBridgeService;

    @BeforeEach
    void setUp() {
        advertisementService = new FakeAdvertisementService();
        advertisementService.setShouldAdvertise(false);

        gateway = new FakeTeamspeakGateway();
        gateway.setConnected(true);
        chatBridgeService = new ChatBridgeService(gateway, 0);

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Steve");
    }

    @Test
    void joinIsRelayedToTeamspeakWhenBridgeEnabled() {
        var listener = new PlayerJoinListener(advertisementService, chatBridgeService, true, LOGGER);
        var event = new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null);

        listener.onPlayerJoin(event);

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Steve joined the server", gateway.getSentMessages().get(0));
    }

    @Test
    void joinIsNotRelayedToTeamspeakWhenBridgeDisabled() {
        var listener = new PlayerJoinListener(advertisementService, chatBridgeService, false, LOGGER);
        var event = new PlayerJoinEvent(player, (net.kyori.adventure.text.Component) null);

        listener.onPlayerJoin(event);

        assertTrue(gateway.getSentMessages().isEmpty());
    }
}
