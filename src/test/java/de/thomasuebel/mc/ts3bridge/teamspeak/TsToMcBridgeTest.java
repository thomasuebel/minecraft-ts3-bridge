package de.thomasuebel.mc.ts3bridge.teamspeak;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class TsToMcBridgeTest {

    private static final Logger LOGGER = Logger.getLogger("test");
    private static final String SELF_UID = "self-uid-abc";

    private FakeTeamspeakGateway gateway;
    private ChatBridgeService chatBridgeService;
    private List<String> broadcasts;
    private TsToMcBridge bridge;

    @BeforeEach
    void setUp() {
        gateway = new FakeTeamspeakGateway();
        gateway.setConnected(true);
        gateway.setSelfUniqueId(SELF_UID);
        chatBridgeService = new ChatBridgeService(gateway, 0);
        broadcasts = new ArrayList<>();
        bridge = new TsToMcBridge(gateway, chatBridgeService, false, LOGGER, broadcasts::add);
    }

    @Test
    void registerCallsRegisterAllEventsAndStoresSelfUid() {
        bridge.register();
        assertTrue(gateway.wasRegisterAllEventsCalled());
    }

    @Test
    void textMessageFromOtherUserIsRelayed() {
        bridge.register();
        gateway.simulateTextMessage("other-uid", 3, 0, "Alice", "hello world");
        assertEquals(1, broadcasts.size());
        assertEquals("[TS] Alice: hello world", broadcasts.get(0));
    }

    @Test
    void textMessageFromSelfIsFiltered() {
        bridge.register();
        gateway.simulateTextMessage(SELF_UID, 3, 0, "BotName", "echo message");
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void privateTextMessageIsFiltered() {
        bridge.register();
        gateway.simulateTextMessage("other-uid", 1, 0, "Alice", "private msg");
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void clientJoinIsRelayedInServerWideMode() {
        bridge.register();
        gateway.simulateClientJoin(0, "user-uid", 42, "Bob", 5);
        assertEquals(1, broadcasts.size());
        assertEquals("[TS] Bob joined", broadcasts.get(0));
    }

    @Test
    void queryClientJoinIsFiltered() {
        bridge.register();
        gateway.simulateClientJoin(1, "query-uid", 99, "ServerQuery", 5);
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void selfJoinIsFiltered() {
        bridge.register();
        gateway.simulateClientJoin(0, SELF_UID, 1, "BotName", 5);
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void clientLeaveIsRelayedAfterJoin() {
        bridge.register();
        gateway.simulateClientJoin(0, "user-uid", 42, "Carol", 5);
        broadcasts.clear();
        gateway.simulateClientLeave(42, 5);
        assertEquals(1, broadcasts.size());
        assertEquals("[TS] Carol left", broadcasts.get(0));
    }

    @Test
    void clientLeaveIsIgnoredIfNeverJoined() {
        bridge.register();
        gateway.simulateClientLeave(99, 5);
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void clientMoveIntoServerWideRelaysJoined() {
        gateway.addClientInfo(77, "move-uid", "Dave");
        bridge.register();
        gateway.simulateClientMoved(77, 3, 7);
        assertEquals(1, broadcasts.size());
        assertEquals("[TS] Dave joined", broadcasts.get(0));
    }

    @Test
    void clientMoveForUnknownClientIsIgnored() {
        bridge.register();
        gateway.simulateClientMoved(999, 3, 7);
        assertTrue(broadcasts.isEmpty());
    }

    @Test
    void channelModeFiltersEventsFromOtherChannels() {
        ChatBridgeService channelBridge = new ChatBridgeService(gateway, 10);
        List<String> channelBroadcasts = new ArrayList<>();
        TsToMcBridge channelTsBridge = new TsToMcBridge(gateway, channelBridge, false, LOGGER, channelBroadcasts::add);
        channelTsBridge.register();

        gateway.simulateClientJoin(0, "user-uid", 42, "Eve", 99);
        assertTrue(channelBroadcasts.isEmpty());
    }

    @Test
    void channelModeRelaysEventsForConfiguredChannel() {
        ChatBridgeService channelBridge = new ChatBridgeService(gateway, 10);
        List<String> channelBroadcasts = new ArrayList<>();
        TsToMcBridge channelTsBridge = new TsToMcBridge(gateway, channelBridge, false, LOGGER, channelBroadcasts::add);
        channelTsBridge.register();

        gateway.simulateClientJoin(0, "user-uid", 42, "Eve", 10);
        assertEquals(1, channelBroadcasts.size());
        assertEquals("[TS] Eve joined", channelBroadcasts.get(0));
    }
}
