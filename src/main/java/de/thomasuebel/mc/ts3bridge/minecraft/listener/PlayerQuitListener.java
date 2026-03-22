package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final ChatBridgeService chatBridgeService;
    private final boolean chatBridgeEnabled;

    public PlayerQuitListener(ChatBridgeService chatBridgeService, boolean chatBridgeEnabled) {
        this.chatBridgeService = chatBridgeService;
        this.chatBridgeEnabled = chatBridgeEnabled;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!chatBridgeEnabled) {
            return;
        }
        chatBridgeService.relayPlayerLeftToTeamspeak(event.getPlayer().getName());
    }
}
