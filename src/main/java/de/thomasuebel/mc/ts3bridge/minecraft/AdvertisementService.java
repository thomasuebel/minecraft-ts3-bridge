package de.thomasuebel.mc.ts3bridge.minecraft;

import de.thomasuebel.mc.ts3bridge.configuration.PluginConfig;
import de.thomasuebel.mc.ts3bridge.user.MappingsRepository;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdvertisementService {

    private final PluginConfig config;
    private final MappingsRepository repository;
    private final Set<UUID> advertisedThisSession = new HashSet<>();

    public AdvertisementService(PluginConfig config, MappingsRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    /**
     * Returns true if the player should receive the TS advertisement on join.
     * Linked players are skipped — they already know about TeamSpeak.
     * Players who have already been advertised this session are skipped.
     */
    public boolean shouldAdvertise(UUID playerUuid) {
        if (repository.isLinked(playerUuid)) return false;
        if (advertisedThisSession.contains(playerUuid)) return false;
        return true;
    }

    public void markAdvertised(UUID playerUuid) {
        advertisedThisSession.add(playerUuid);
    }

    public String buildAdvertisementMessage() {
        return config.getAdvertisementMessage().replace("{address}", config.getTsServerAddress());
    }
}
