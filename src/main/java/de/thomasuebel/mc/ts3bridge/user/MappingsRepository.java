package de.thomasuebel.mc.ts3bridge.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MappingsRepository {

    private static final String MAPPINGS_FILE = "mappings.json";
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Path dataFolder;
    private final Gson gson;
    private final Logger logger;

    // mc-uuid (string) → ts-uid
    private Map<String, String> mappings = new HashMap<>();

    public MappingsRepository(Path dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void load() {
        Path path = dataFolder.resolve(MAPPINGS_FILE);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                Map<String, String> loaded = gson.fromJson(reader, MAP_TYPE);
                mappings = loaded != null ? loaded : new HashMap<>();
                logger.info("Loaded " + mappings.size() + " player mapping(s) from mappings.json.");
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        "Failed to read mappings.json — starting with empty mappings. Fix or delete the file and restart to restore.",
                        e);
                mappings = new HashMap<>();
            }
        }
    }

    public void save() {
        Path path = dataFolder.resolve(MAPPINGS_FILE);
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(mappings, writer);
            logger.info("Saved " + mappings.size() + " player mapping(s) to mappings.json.");
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Failed to save mappings.json — player link data may be lost on next restart!",
                    e);
            throw new RuntimeException("Failed to save mappings.json", e);
        }
    }

    public void link(UUID minecraftUuid, String tsUid) {
        mappings.put(minecraftUuid.toString(), tsUid);
    }

    public void unlink(UUID minecraftUuid) {
        mappings.remove(minecraftUuid.toString());
    }

    public boolean isLinked(UUID minecraftUuid) {
        return mappings.containsKey(minecraftUuid.toString());
    }

    public Optional<String> findTsUidByMinecraftUuid(UUID minecraftUuid) {
        return Optional.ofNullable(mappings.get(minecraftUuid.toString()));
    }

    public Optional<UUID> findMinecraftUuidByTsUid(String tsUid) {
        return mappings.entrySet().stream()
                .filter(entry -> entry.getValue().equals(tsUid))
                .map(entry -> UUID.fromString(entry.getKey()))
                .findFirst();
    }
}
