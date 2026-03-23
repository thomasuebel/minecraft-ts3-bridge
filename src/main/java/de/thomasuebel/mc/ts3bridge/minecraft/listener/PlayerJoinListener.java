package de.thomasuebel.mc.ts3bridge.minecraft.listener;

import de.thomasuebel.mc.ts3bridge.chat.ChatBridgeService;
import de.thomasuebel.mc.ts3bridge.minecraft.AdvertisementService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Logger;

public class PlayerJoinListener implements Listener {

    private final AdvertisementService advertisementService;
    private final ChatBridgeService chatBridgeService;
    private final boolean chatBridgeEnabled;
    private final Logger logger;

    public PlayerJoinListener(AdvertisementService advertisementService,
                              ChatBridgeService chatBridgeService,
                              boolean chatBridgeEnabled,
                              Logger logger) {
        this.advertisementService = advertisementService;
        this.chatBridgeService = chatBridgeService;
        this.chatBridgeEnabled = chatBridgeEnabled;
        this.logger = logger;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();

        if (chatBridgeEnabled) {
            chatBridgeService.relayPlayerJoinedToTeamspeak(player.getName());
        }

        if (!advertisementService.shouldAdvertise(player.getUniqueId())) {
            logger.info("Player '" + player.getName() + "' joined — skipping TS advertisement (account is linked).");
            return;
        }

        player.sendMessage(Component.text(advertisementService.buildAdvertisementMessage()));
        advertisementService.markAdvertised(player.getUniqueId());
        logger.info("Sent TeamSpeak advertisement to player '" + player.getName() + "'.");
    }
}
