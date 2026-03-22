package de.thomasuebel.mc.ts3bridge.teamspeak;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory test double for TeamspeakGateway.
 * Used across test suites to avoid mocking concrete HolyWaffle classes.
 */
public class FakeTeamspeakGateway implements TeamspeakGateway {

    private boolean connected = false;
    private final List<TsClient> onlineClients = new ArrayList<>();
    private final List<String> sentMessages = new ArrayList<>();
    private final List<String> sentChannelMessages = new ArrayList<>();

    public void setConnected(boolean connected) {
        this.connected = connected;
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
}
