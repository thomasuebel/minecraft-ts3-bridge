package de.thomasuebel.mc.ts3bridge.minecraft;

import java.util.UUID;

/**
 * Test double for AdvertisementService.
 * Concrete classes cannot be mocked on Java 25 without the inline mock maker.
 */
public class FakeAdvertisementService extends AdvertisementService {

    private boolean shouldAdvertise = true;

    public FakeAdvertisementService() {
        super(null, null);
    }

    public void setShouldAdvertise(boolean shouldAdvertise) {
        this.shouldAdvertise = shouldAdvertise;
    }

    @Override
    public boolean shouldAdvertise(UUID playerUuid) {
        return shouldAdvertise;
    }

    @Override
    public String buildAdvertisementMessage() {
        return "Join our TeamSpeak server: ts.example.com";
    }
}
