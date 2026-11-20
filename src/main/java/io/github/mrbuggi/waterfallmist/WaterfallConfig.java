package io.github.mrbuggi.waterfallmist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WaterfallConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("waterfall-mist.json");

    private static WaterfallConfig INSTANCE = new WaterfallConfig();

    public float mistOpacity = 0.60F;
    public float mistDensity = 1.0F;
    public int minFallHeight = 3;
    public boolean enableMist = true;
    public boolean enableSplashes = true;
    public boolean enableBubbles = true;
    public boolean enableSounds = true;
    public float soundVolume = 1.0F;
    public int searchRadius = 24;

    public static WaterfallConfig get() {
        return INSTANCE;
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            WaterfallConfig config = GSON.fromJson(reader, WaterfallConfig.class);
            if (config != null) {
                config.sanitize();
                INSTANCE = config;
            }
        } catch (Exception e) {
            WaterfallMistClient.LOGGER.error("Failed to load waterfall-mist config, using defaults", e);
            INSTANCE = new WaterfallConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            WaterfallMistClient.LOGGER.error("Failed to save waterfall-mist config", e);
        }
    }

    private void sanitize() {
        mistOpacity = Math.max(0.0F, Math.min(1.0F, mistOpacity));
        mistDensity = Math.max(0.1F, Math.min(3.0F, mistDensity));
        minFallHeight = Math.max(1, Math.min(16, minFallHeight));
        soundVolume = Math.max(0.0F, Math.min(2.0F, soundVolume));
        searchRadius = Math.max(8, Math.min(48, searchRadius));
    }
}
