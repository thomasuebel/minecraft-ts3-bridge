package de.thomasuebel.mc.ts3bridge.chat;

import de.thomasuebel.mc.ts3bridge.teamspeak.FakeTeamspeakGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatBridgeServiceTest {

    private FakeTeamspeakGateway gateway;
    private ChatBridgeService service;

    @BeforeEach
    void setUp() {
        gateway = new FakeTeamspeakGateway();
        service = new ChatBridgeService(gateway, 0);
    }

    @Test
    void minecraftChatIsRelayedToTeamspeak() {
        gateway.setConnected(true);

        service.relayToTeamspeak("Steve", "Hello TeamSpeak!");

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Steve: Hello TeamSpeak!", gateway.getSentMessages().get(0));
    }

    @Test
    void minecraftChatIsNotRelayedWhenDisconnected() {
        gateway.setConnected(false);

        service.relayToTeamspeak("Steve", "Hello!");

        assertTrue(gateway.getSentMessages().isEmpty());
    }

    @Test
    void teamspeakChatFormatsCorrectly() {
        String formatted = service.formatTeamspeakMessage("TsNickname", "Hello Minecraft!");

        assertEquals("[TS] TsNickname: Hello Minecraft!", formatted);
    }

    @Test
    void emptyMessageIsNotRelayed() {
        gateway.setConnected(true);

        service.relayToTeamspeak("Steve", "");

        assertTrue(gateway.getSentMessages().isEmpty());
    }

    @Test
    void blankMessageIsNotRelayed() {
        gateway.setConnected(true);

        service.relayToTeamspeak("Steve", "   ");

        assertTrue(gateway.getSentMessages().isEmpty());
    }

    // --- Channel mode ---

    @Test
    void inChannelModeMcChatUsesChannelMessage() {
        gateway.setConnected(true);
        ChatBridgeService channelService = new ChatBridgeService(gateway, 42);

        channelService.relayToTeamspeak("Steve", "Hello channel!");

        assertTrue(gateway.getSentMessages().isEmpty(), "sendServerMessage must not be called in channel mode");
        assertEquals(1, gateway.getSentChannelMessages().size());
        assertEquals("[MC] Steve: Hello channel!", gateway.getSentChannelMessages().get(0));
    }

    // --- Status relay: MC→TS ---

    @Test
    void playerJoinIsRelayedToTeamspeak() {
        gateway.setConnected(true);

        service.relayPlayerJoinedToTeamspeak("Steve");

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Steve joined the server", gateway.getSentMessages().get(0));
    }

    @Test
    void playerQuitIsRelayedToTeamspeak() {
        gateway.setConnected(true);

        service.relayPlayerLeftToTeamspeak("Steve");

        assertEquals(1, gateway.getSentMessages().size());
        assertEquals("[MC] Steve left the server", gateway.getSentMessages().get(0));
    }

    @Test
    void playerJoinInChannelModeUsesChannelMessage() {
        gateway.setConnected(true);
        ChatBridgeService channelService = new ChatBridgeService(gateway, 42);

        channelService.relayPlayerJoinedToTeamspeak("Steve");

        assertTrue(gateway.getSentMessages().isEmpty());
        assertEquals(1, gateway.getSentChannelMessages().size());
        assertEquals("[MC] Steve joined the server", gateway.getSentChannelMessages().get(0));
    }

    // --- Status formatting: TS→MC ---

    @Test
    void formatTsClientJoined() {
        assertEquals("[TS] Spy98 joined", service.formatTsClientJoined("Spy98"));
    }

    @Test
    void formatTsClientLeft() {
        assertEquals("[TS] Spy98 left", service.formatTsClientLeft("Spy98"));
    }

    // --- shouldRelayTsStatusEvent ---

    @Test
    void withNoChannelFilterAllStatusEventsAreRelayed() {
        assertTrue(service.shouldRelayTsStatusEvent(1));
        assertTrue(service.shouldRelayTsStatusEvent(99));
    }

    @Test
    void withChannelFilterOnlyMatchingChannelStatusEventsAreRelayed() {
        ChatBridgeService channelService = new ChatBridgeService(gateway, 42);

        assertTrue(channelService.shouldRelayTsStatusEvent(42));
        assertFalse(channelService.shouldRelayTsStatusEvent(99));
    }

    // --- shouldRelayFromTeamspeak ---

    @Test
    void withNoChannelFilterChannelAndServerMessagesAreRelayed() {
        // targetMode 2 = channel, 3 = server-wide — both should pass
        assertTrue(service.shouldRelayFromTeamspeak(2, 0));
        assertTrue(service.shouldRelayFromTeamspeak(3, 0));
    }

    @Test
    void withNoChannelFilterPrivateMessagesAreNotRelayed() {
        // targetMode 1 = private — should never be broadcast to Minecraft
        assertFalse(service.shouldRelayFromTeamspeak(1, 5));
    }

    @Test
    void withChannelFilterAnyChannelMessageIsRelayed() {
        // The query client is subscribed to textchannel events for the bridge channel.
        // TS3 does not reliably include the channel ID in the target field of the event —
        // receiving a targetMode=2 event at all means it is from the bridge channel.
        ChatBridgeService channelService = new ChatBridgeService(gateway, 42);

        assertTrue(channelService.shouldRelayFromTeamspeak(2, 0));   // target=0 (typical TS3 event)
        assertTrue(channelService.shouldRelayFromTeamspeak(2, 42));  // target=channelId (also fine)
    }

    @Test
    void withChannelFilterNonChannelMessagesAreNotRelayed() {
        ChatBridgeService channelService = new ChatBridgeService(gateway, 42);

        assertFalse(channelService.shouldRelayFromTeamspeak(3, 0)); // server-wide
        assertFalse(channelService.shouldRelayFromTeamspeak(1, 5)); // private
    }
}
