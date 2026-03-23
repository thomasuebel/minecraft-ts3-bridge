package de.thomasuebel.mc.ts3bridge.teamspeak;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TsToMcBridge {

    private final TeamspeakGateway gateway;
    private final ChatBridgeService chatBridgeService;
    private final boolean debugLogging;
    private final Logger logger;
    private final Consumer<String> broadcast;

    private final ConcurrentHashMap<Integer, String> clientNicknames = new ConcurrentHashMap<>();
    private String selfUid = "";

    public TsToMcBridge(TeamspeakGateway gateway,
                        ChatBridgeService chatBridgeService,
                        boolean debugLogging,
                        Logger logger,
                        Consumer<String> broadcast) {
        this.gateway = gateway;
        this.chatBridgeService = chatBridgeService;
        this.debugLogging = debugLogging;
        this.logger = logger;
        this.broadcast = broadcast;
    }

    public void register() {
        gateway.registerAllEvents();
        selfUid = gateway.getSelfUniqueId();
        gateway.registerBridge(this);
        logger.info("TS→MC chat bridge registered — TeamSpeak messages will appear in Minecraft chat.");
    }

    void handleTextMessage(String invokerUniqueId, int targetMode, int targetId,
                           String invokerName, String message) {
        try {
            if (debugLogging) logger.fine("[TS Bridge] onTextMessage: invoker=" + invokerUniqueId
                    + " targetMode=" + targetMode + " targetId=" + targetId);
            if (invokerUniqueId.equals(selfUid)) return;
            if (!chatBridgeService.shouldRelayFromTeamspeak(targetMode, targetId)) {
                if (debugLogging) logger.fine("[TS Bridge] Message filtered by shouldRelayFromTeamspeak.");
                return;
            }
            broadcast.accept(chatBridgeService.formatTeamspeakMessage(invokerName, message));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected error while processing incoming TeamSpeak message.", ex);
        }
    }

    void handleClientJoin(int clientType, String uniqueClientIdentifier,
                          int clid, String nickname, int targetChannelId) {
        try {
            if (debugLogging) logger.fine("[TS Bridge] onClientJoin: clid=" + clid + " nick=" + nickname);
            if (clientType == 1) return;
            if (selfUid.equals(uniqueClientIdentifier)) return;
            clientNicknames.put(clid, nickname);
            if (!chatBridgeService.shouldRelayTsStatusEvent(targetChannelId)) return;
            broadcast.accept(chatBridgeService.formatTsClientJoined(nickname));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected error while processing TeamSpeak client join.", ex);
        }
    }

    void handleClientLeave(int clid, int clientFromId) {
        try {
            if (debugLogging) logger.fine("[TS Bridge] onClientLeave: clid=" + clid);
            String nickname = clientNicknames.remove(clid);
            if (nickname == null) return;
            if (!chatBridgeService.shouldRelayTsStatusEvent(clientFromId)) return;
            broadcast.accept(chatBridgeService.formatTsClientLeft(nickname));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected error while processing TeamSpeak client leave.", ex);
        }
    }

    void handleClientMoved(int clid, int fromChannelId, int targetChannelId) {
        try {
            if (debugLogging) logger.fine("[TS Bridge] onClientMoved: clid=" + clid
                    + " from=" + fromChannelId + " to=" + targetChannelId);
            gateway.getClientInfo(clid).ifPresent(client -> {
                if (selfUid.equals(client.uid())) return;
                clientNicknames.put(clid, client.nickname());
                if (chatBridgeService.shouldRelayTsStatusEvent(targetChannelId)) {
                    broadcast.accept(chatBridgeService.formatTsClientJoined(client.nickname()));
                } else if (chatBridgeService.shouldRelayTsStatusEvent(fromChannelId)) {
                    broadcast.accept(chatBridgeService.formatTsClientLeft(client.nickname()));
                }
            });
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Unexpected error while processing TeamSpeak client move.", ex);
        }
    }
}
