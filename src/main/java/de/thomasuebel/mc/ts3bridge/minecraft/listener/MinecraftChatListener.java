package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class MinecraftChatListener implements Listener {

    private final ChatBridgeService chatBridgeService;
    private final boolean enabled;

    public MinecraftChatListener(ChatBridgeService chatBridgeService, boolean enabled) {
        this.chatBridgeService = chatBridgeService;
        this.enabled = enabled;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!enabled) {
            return;
        }
        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        chatBridgeService.relayToTeamspeak(playerName, message);
    }
}
