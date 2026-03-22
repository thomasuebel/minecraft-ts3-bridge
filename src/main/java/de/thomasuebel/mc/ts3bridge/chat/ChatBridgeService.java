package de.thomasuebel.mc.ts3bridge.chat;

import de.thomasuebel.mc.ts3bridge.teamspeak.TeamspeakGateway;

public class ChatBridgeService {

    private static final String MC_PREFIX = "[MC] ";
    private static final String TS_PREFIX = "[TS] ";

    private static final int TARGET_MODE_CHANNEL = 2;
    private static final int TARGET_MODE_SERVER = 3;

    private final TeamspeakGateway gateway;
    /** 0 = no channel filter; > 0 = restrict bridge to this TS channel ID. */
    private final int channelId;

    public ChatBridgeService(TeamspeakGateway gateway, int channelId) {
        this.gateway = gateway;
        this.channelId = channelId;
    }

    public int getChannelId() {
        return channelId;
    }

    /**
     * Relays a Minecraft chat message to TeamSpeak.
     * Uses a channel message when a channel is configured, otherwise a server-wide message.
     *
     * @param playerName the Minecraft player's display name
     * @param message    the chat message content
     */
    public void relayToTeamspeak(String playerName, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        String formatted = MC_PREFIX + playerName + ": " + message;
        if (channelId > 0) {
            gateway.sendChannelMessage(formatted);
        } else {
            gateway.sendServerMessage(formatted);
        }
    }

    /**
     * Relays a Minecraft player join event to TeamSpeak.
     */
    public void relayPlayerJoinedToTeamspeak(String playerName) {
        String message = MC_PREFIX + playerName + " joined the server";
        if (channelId > 0) {
            gateway.sendChannelMessage(message);
        } else {
            gateway.sendServerMessage(message);
        }
    }

    /**
     * Relays a Minecraft player quit event to TeamSpeak.
     */
    public void relayPlayerLeftToTeamspeak(String playerName) {
        String message = MC_PREFIX + playerName + " left the server";
        if (channelId > 0) {
            gateway.sendChannelMessage(message);
        } else {
            gateway.sendServerMessage(message);
        }
    }

    /** Formats a TS client join event for display in Minecraft chat. */
    public String formatTsClientJoined(String nickname) {
        return TS_PREFIX + nickname + " joined";
    }

    /** Formats a TS client leave event for display in Minecraft chat. */
    public String formatTsClientLeft(String nickname) {
        return TS_PREFIX + nickname + " left";
    }

    /**
     * Returns true if a TeamSpeak status event (join/leave/move) for the given channel
     * should be relayed to Minecraft.
     * When no channel is configured, all events pass through.
     *
     * @param eventChannelId the TS channel involved in the event
     */
    public boolean shouldRelayTsStatusEvent(int eventChannelId) {
        if (channelId == 0) {
            return true;
        }
        return eventChannelId == channelId;
    }

    /**
     * Formats an incoming TeamSpeak message for display in Minecraft chat.
     *
     * @param tsNickname the TeamSpeak client's nickname
     * @param message    the message content
     * @return formatted string ready for broadcast in Minecraft
     */
    public String formatTeamspeakMessage(String tsNickname, String message) {
        return TS_PREFIX + tsNickname + ": " + message;
    }

    /**
     * Returns true if a TeamSpeak text message event should be relayed to Minecraft.
     * When no channel is configured, all messages pass through.
     * When a channel is configured, only channel messages targeted at that channel pass through.
     *
     * @param targetMode   the TS3 targetmode value (2 = channel, 3 = server, 1 = private)
     * @param targetId     the target ID from the event (channel ID for channel messages)
     */
    public boolean shouldRelayFromTeamspeak(int targetMode, int targetId) {
        if (channelId == 0) {
            // Server-wide mode: relay channel and server-wide messages.
            // Private messages (targetMode=1) are never broadcast to Minecraft.
            return targetMode == TARGET_MODE_CHANNEL || targetMode == TARGET_MODE_SERVER;
        }
        // Channel mode: the query client is subscribed to textchannel events for the bridge
        // channel only. TS3 does not reliably include the channel ID in the target field —
        // receiving a targetMode=2 event means it is from the bridge channel by definition.
        return targetMode == TARGET_MODE_CHANNEL;
    }
}
