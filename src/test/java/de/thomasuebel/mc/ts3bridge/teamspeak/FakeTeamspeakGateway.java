package de.thomasuebel.mc.ts3bridge.teamspeak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory test double for TeamspeakGateway.
 * Used across test suites to avoid mocking concrete HolyWaffle classes.
 */
public class FakeTeamspeakGateway implements TeamspeakGateway {

    private boolean connected = false;
    private final List<TsClient> onlineClients = new ArrayList<>();
    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> sentChannelMessages = new ArrayList<>();
    private String selfUniqueId = "fake-self-uid";
    private final Map<Integer, TsClient> clientInfoMap = new HashMap<>();
    private TsToMcBridge registeredBridge = null;
    private boolean registerAllEventsCalled = false;

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setSelfUniqueId(String uid) { this.selfUniqueId = uid; }

    public void addClientInfo(int clid, String uid, String nickname) {
        clientInfoMap.put(clid, new TsClient(uid, nickname));
    }

    public boolean wasRegisterAllEventsCalled() { return registerAllEventsCalled; }

    public void simulateTextMessage(String invokerUid, int targetMode, int targetId, String name, String msg) {
        if (registeredBridge != null) registeredBridge.handleTextMessage(invokerUid, targetMode, targetId, name, msg);
    }

    public void simulateClientJoin(int clientType, String uid, int clid, String nickname, int channelId) {
        if (registeredBridge != null) registeredBridge.handleClientJoin(clientType, uid, clid, nickname, channelId);
    }

    public void simulateClientLeave(int clid, int fromChannelId) {
        if (registeredBridge != null) registeredBridge.handleClientLeave(clid, fromChannelId);
    }

    public void simulateClientMoved(int clid, int fromChannelId, int toChannelId) {
        if (registeredBridge != null) registeredBridge.handleClientMoved(clid, fromChannelId, toChannelId);
    }

    /** Adds a client with the given UID, using the UID as the display name. */
    public void addOnlineUid(String uid) {
        onlineClients.add(new TsClient(uid, uid));
    }

    /** Adds a client with an explicit UID and display name. */
    public void addOnlineClient(String uid, String nickname) {
        onlineClients.add(new TsClient(uid, nickname));
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }

    public List<String> getSentChannelMessages() {
        return Collections.unmodifiableList(sentChannelMessages);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public List<String> getOnlineClientUids() {
        if (!connected) return Collections.emptyList();
        return onlineClients.stream().map(TsClient::uid).toList();
    }

    @Override
    public List<TsClient> getOnlineClients() {
        if (!connected) return Collections.emptyList();
        return Collections.unmodifiableList(onlineClients);
    }

    @Override
    public void sendServerMessage(String message) {
        if (!connected) return;
        sentMessages.add(message);
    }

    @Override
    public void sendChannelMessage(String message) {
        if (!connected) return;
        sentChannelMessages.add(message);
    }

    @Override
    public void registerAllEvents() { registerAllEventsCalled = true; }

    @Override
    public String getSelfUniqueId() { return selfUniqueId; }

    @Override
    public Optional<TsClient> getClientInfo(int clid) {
        return Optional.ofNullable(clientInfoMap.get(clid));
    }

    @Override
    public void registerBridge(TsToMcBridge bridge) { this.registeredBridge = bridge; }
}
