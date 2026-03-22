package de.thomasuebel.mc.ts3bridge.teamspeak;

import java.util.List;

public class TeamspeakService {

    private final TeamspeakGateway gateway;

    public TeamspeakService(TeamspeakGateway gateway) {
        this.gateway = gateway;
    }

    public boolean isConnected() {
        return gateway.isConnected();
    }

    /**
     * Returns the unique identifiers of all non-query clients currently online in TeamSpeak.
     */
    public List<String> getOnlineClientUids() {
        return gateway.getOnlineClientUids();
    }

    public List<TsClient> getOnlineClients() {
        return gateway.getOnlineClients();
    }

    /**
     * Returns all online TS clients whose display name matches the given name,
     * case-insensitively. Returns multiple results when names collide.
     */
    public List<TsClient> findOnlineClientsByNickname(String nickname) {
        return gateway.getOnlineClients().stream()
                .filter(c -> c.nickname().equalsIgnoreCase(nickname))
                .toList();
    }

    /**
     * Sends a message to the TeamSpeak virtual server (visible in all channels).
     * Used for the MC→TS chat bridge.
     */
    public void sendChannelMessage(String message) {
        gateway.sendServerMessage(message);
    }
}
