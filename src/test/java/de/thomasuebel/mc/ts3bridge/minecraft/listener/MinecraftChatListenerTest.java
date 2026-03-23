package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinecraftChatListenerTest {

    @Mock Player player;
    @Mock AsyncChatEvent event;

    private FakeTeamspeakGateway gateway;
    private ChatBridgeService chatBridgeService;

    @BeforeEach
    void setUp() {
        gateway = new FakeTeamspeakGateway();
        gateway.setConnected(true);
        chatBridgeService = new ChatBridgeService(gateway, 0);
    }

    @Test
    void chatIsRelayedToTeamspeakWhenEnabled() {
        when(player.getName()).thenReturn("Alice");
        when(event.getPlayer()).thenReturn(player);
        when(event.message()).thenReturn(Component.text("hello world"));

        new MinecraftChatListener(chatBridgeService, true).onChat(event);

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Alice: hello world", gateway.getSentMessages().get(0));
    }

    @Test
    void chatIsNotRelayedWhenDisabled() {
        new MinecraftChatListener(chatBridgeService, false).onChat(event);

        assertTrue(gateway.getSentMessages().isEmpty());
    }
}
