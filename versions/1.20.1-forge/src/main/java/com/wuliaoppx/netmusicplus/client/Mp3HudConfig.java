package com.wuliaoppx.netmusicplus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client side look of the "now playing" overlay: whether the track name and the lyrics are shown, and where the text
 * sits. Kept in its own JSON file instead of the mod config so the in-game editor can write it back immediately
 * without touching the config system.
 */
public final class Mp3HudConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FMLPaths.CONFIGDIR.get().resolve("netmusicplus-hud.json");

    private static boolean showNowPlaying = true;
    private static boolean showLyrics = true;
    private static int offsetX = 0;
    private static int offsetY = 0;
    private static Mp3Theme theme = Mp3Theme.NIGHT;

    static {
        load();
    }

    private Mp3HudConfig() {
    }

    public static boolean showNowPlaying() {
        return showNowPlaying;
    }

    public static boolean showLyrics() {
        return showLyrics;
    }

    public static int offsetX() {
        return offsetX;
    }

    public static int offsetY() {
        return offsetY;
    }

    public static Mp3Theme theme() {
        return theme;
    }

    /** Cycles to the next GUI theme and remembers it. */
    public static Mp3Theme nextTheme() {
        Mp3Theme[] values = Mp3Theme.values();
        theme = values[(theme.ordinal() + 1) % values.length];
        save();
        return theme;
    }

    public static void toggleNowPlaying() {
        showNowPlaying = !showNowPlaying;
        save();
    }

    public static void toggleLyrics() {
        showLyrics = !showLyrics;
        save();
    }

    public static void setOffset(int x, int y) {
        offsetX = x;
        offsetY = y;
        save();
    }

    private static void load() {
        if (!Files.isRegularFile(PATH)) {
            return;
        }
        try {
            JsonObject json = GSON.fromJson(Files.readString(PATH, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                return;
            }
            if (json.has("show_now_playing")) {
                showNowPlaying = json.get("show_now_playing").getAsBoolean();
            }
            if (json.has("show_lyrics")) {
                showLyrics = json.get("show_lyrics").getAsBoolean();
            }
            if (json.has("offset_x")) {
                offsetX = json.get("offset_x").getAsInt();
            }
            if (json.has("offset_y")) {
                offsetY = json.get("offset_y").getAsInt();
            }
            if (json.has("theme")) {
                theme = Mp3Theme.byId(json.get("theme").getAsString());
            }
        } catch (Exception exception) {
            NetMusicPlus.LOGGER.warn("Cannot read {}", PATH, exception);
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("show_now_playing", showNowPlaying);
        json.addProperty("show_lyrics", showLyrics);
        json.addProperty("offset_x", offsetX);
        json.addProperty("offset_y", offsetY);
        json.addProperty("theme", theme.id());
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            NetMusicPlus.LOGGER.warn("Cannot write {}", PATH, exception);
        }
    }
}
