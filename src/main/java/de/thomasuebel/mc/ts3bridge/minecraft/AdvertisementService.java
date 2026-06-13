package de.thomasuebel.mc.ts3bridge.minecraft;

import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;

import java.util.UUID;

public class AdvertisementService {

    private final PluginConfig config;
    private final MappingsRepository repository;

    public AdvertisementService(PluginConfig config, MappingsRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    /**
     * Returns true if the player should receive the TS advertisement on join.
     * Linked players are skipped — they already know about TeamSpeak.
     */
    public boolean shouldAdvertise(UUID playerUuid) {
        return !repository.isLinked(playerUuid);
    }

    public String buildAdvertisementMessage() {
        return config.getAdvertisementMessage().replace("{address}", config.getTsServerAddress());
    }
}
