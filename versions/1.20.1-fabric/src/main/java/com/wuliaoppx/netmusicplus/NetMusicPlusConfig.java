package com.wuliaoppx.netmusicplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common config: the amplifier multipliers and the MP3 player's out-loud range.
 * <p>
 * Every multiplier is relative to NetMusic's built-in range of 64 blocks, which comes from its hardcoded
 * {@code volume = 4.0F} times the vanilla attenuation distance of 16 (see {@link AmplifierData}).
 * <p>
 * Fabric has no config API in the loader, so this is a hand rolled {@code config/netmusicplus.json} with the same keys
 * the NeoForge build exposes through {@code ModConfigSpec}.
 */
public final class NetMusicPlusConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("netmusicplus.json");

    private static final double DEFAULT_LEVEL_1 = 2.0D;
    private static final double DEFAULT_LEVEL_2 = 3.0D;
    private static final double DEFAULT_LEVEL_3 = 4.0D;
    private static final double DEFAULT_OUT_LOUD_RADIUS = 64.0D;

    private static double level1 = DEFAULT_LEVEL_1;
    private static double level2 = DEFAULT_LEVEL_2;
    private static double level3 = DEFAULT_LEVEL_3;
    private static double outLoudRadius = DEFAULT_OUT_LOUD_RADIUS;
    private static boolean loaded;

    private NetMusicPlusConfig() {
    }

    /** Reads (or creates) the config file. Called from the mod initialiser. */
    public static void load() {
        if (Files.isRegularFile(PATH)) {
            try {
                JsonObject json = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), JsonObject.class);
                if (json != null) {
                    level1 = read(json, "level_1_multiplier", DEFAULT_LEVEL_1, 1.0D, 64.0D);
                    level2 = read(json, "level_2_multiplier", DEFAULT_LEVEL_2, 1.0D, 64.0D);
                    level3 = read(json, "level_3_multiplier", DEFAULT_LEVEL_3, 1.0D, 64.0D);
                    outLoudRadius = read(json, "out_loud_radius", DEFAULT_OUT_LOUD_RADIUS, 1.0D, 1024.0D);
                }
            } catch (Exception exception) {
                NetMusicPlus.LOGGER.warn("Cannot read {}", PATH, exception);
            }
        }
        loaded = true;
        save();
    }

    private static double read(JsonObject json, String key, double fallback, double min, double max) {
        if (!json.has(key)) {
            return fallback;
        }
        double value = json.get(key).getAsDouble();
        return Math.max(min, Math.min(max, value));
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("level_1_multiplier", level1);
        json.addProperty("level_2_multiplier", level2);
        json.addProperty("level_3_multiplier", level3);
        json.addProperty("out_loud_radius", outLoudRadius);
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            NetMusicPlus.LOGGER.warn("Cannot write {}", PATH, exception);
        }
    }

    public static float multiplier(int tier) {
        if (!loaded) {
            // The sound engine can ask for a radius before the configs are read; fall back to the defaults.
            return switch (tier) {
                case 1 -> 2.0F;
                case 2 -> 3.0F;
                case 3 -> 4.0F;
                default -> 1.0F;
            };
        }
        double value = switch (tier) {
            case 1 -> level1;
            case 2 -> level2;
            case 3 -> level3;
            default -> 1.0D;
        };
        return (float) value;
    }

    public static float outLoudRadius() {
        return loaded ? (float) outLoudRadius : (float) DEFAULT_OUT_LOUD_RADIUS;
    }
}
