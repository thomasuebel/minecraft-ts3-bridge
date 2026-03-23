package de.thomasuebel.mc.ts3bridge.teamspeak;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction over the TeamSpeak 3 ServerQuery connection.
 * Keeps the rest of the codebase decoupled from HolyWaffle's API directly.
 */
public interface TeamspeakGateway {

    boolean isConnected();

    /**
     * Returns the unique identifiers of all non-query clients currently online.
     */
    List<String> getOnlineClientUids();

    /**
     * Returns all non-query clients currently online, with UID and display name.
     */
    List<TsClient> getOnlineClients();

    /**
     * Sends a message to the virtual server (visible in all channels).
     */
    void sendServerMessage(String message);

    /**
     * Sends a message to the channel the ServerQuery client is currently in.
     */
    void sendChannelMessage(String message);

    void registerAllEvents();

    String getSelfUniqueId();

    Optional<TsClient> getClientInfo(int clid);

    void registerBridge(TsToMcBridge bridge);
}
