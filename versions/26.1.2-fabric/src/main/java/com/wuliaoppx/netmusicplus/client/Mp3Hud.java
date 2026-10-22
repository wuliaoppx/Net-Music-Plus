package com.wuliaoppx.netmusicplus.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.wuliaoppx.netmusicplus.NetMusicPlus;

import java.util.List;

/**
 * The "now playing" overlay above the hotbar: the track name and, optionally, the lyric line that matches the current
 * playback position. Both can be switched off in the MP3 player GUI, and the position is dragged in place inside that
 * same GUI (see {@code Mp3Screen}), so no second screen is needed.
 */
public final class Mp3Hud {
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int LYRIC_COLOR = 0xFFC8C8C8;
    /** How long the track name stays on screen after a track change, and how long it takes to fade out. */
    private static final int NAME_MILLIS = 1000;
    private static final int NAME_FADE_MILLIS = 300;

    private static final Mp3Hud INSTANCE = new Mp3Hud();

    private Mp3Hud() {
    }

    public static void register() {
        HudElementRegistry.addLast(NetMusicPlus.id("mp3_hud"), INSTANCE::render);
    }

    /** Fabric calls this from the HUD render callback; the delta is unused, the HUD only shows absolute positions. */
    private void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        this.renderAction(graphics, minecraft);
        if (!Mp3HudConfig.showNowPlaying() && !Mp3HudConfig.showLyrics()) {
            return;
        }
        Mp3Client.Track track = Mp3Client.currentTrack();
        if (track == null) {
            return;
        }
        draw(graphics, track, graphics.guiWidth() / 2 + Mp3HudConfig.offsetX(),
                graphics.guiHeight() - 55 + Mp3HudConfig.offsetY());
    }

    /** Feedback for the keybind gestures ("paused", "next track", ...), independent of the overlay settings. */
    private void renderAction(GuiGraphicsExtractor graphics, Minecraft minecraft) {
        Component action = Mp3Client.actionText();
        if (action == null) {
            return;
        }
        int alpha = Mp3Client.actionAlpha();
        if (alpha <= 0) {
            return;
        }
        int width = minecraft.font.width(action);
        int x = graphics.guiWidth() / 2 - width / 2;
        int y = graphics.guiHeight() - 72;
        graphics.fill(x - 5, y - 4, x + width + 5, y + 12, ((alpha * 0x66 / 255) & 0xFF) << 24);
        graphics.text(minecraft.font, action, x, y, (alpha << 24) | 0xFFFFFF, true);
    }

    /**
     * Draws the overlay centred on {@code centerX} with its first line at {@code y}. The track name only shows for a
     * second after a track change and then fades out; its line stays reserved so the lyrics never jump.
     */
    public static void draw(GuiGraphicsExtractor graphics, Mp3Client.Track track, int centerX, int y) {
        long elapsed = Mp3Client.elapsedMillis(track);
        String lyric = Mp3HudConfig.showLyrics() ? currentLyric(track) : null;
        Component name = null;
        int nameAlpha = 0;
        if (Mp3HudConfig.showNowPlaying()) {
            nameAlpha = nameAlphaFor(elapsed);
            if (nameAlpha > 0) {
                name = Component.translatable("hud.netmusicplus.now_playing", track.name());
            }
        }
        drawLines(graphics, name, nameAlpha, lyric, centerX, y);
    }

    /** 255 while the name is fully visible, fading to 0 over the last {@link #NAME_FADE_MILLIS} milliseconds. */
    private static int nameAlphaFor(long elapsed) {
        if (elapsed >= NAME_MILLIS) {
            return 0;
        }
        long left = NAME_MILLIS - elapsed;
        return left >= NAME_FADE_MILLIS ? 255 : (int) (255L * left / NAME_FADE_MILLIS);
    }

    /** The same overlay with fixed text, used by the position editor. */
    public static void drawPreview(GuiGraphicsExtractor graphics, Component name, String lyric, int centerX, int y) {
        drawLines(graphics, name, name != null ? 255 : 0, lyric, centerX, y);
    }

    private static void drawLines(GuiGraphicsExtractor graphics, Component name, int nameAlpha, String lyric, int centerX, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        int line = y;
        if (Mp3HudConfig.showNowPlaying()) {
            if (name != null && nameAlpha > 0) {
                drawLine(graphics, minecraft, name, centerX, line, NAME_COLOR, nameAlpha);
            }
            // keep the slot even when the name is hidden, otherwise the lyric would jump up a line
            line += 11;
        }
        if (lyric != null && !lyric.isBlank()) {
            drawLine(graphics, minecraft, Component.literal(lyric), centerX, line, LYRIC_COLOR, 255);
        }
    }

    private static int drawLine(GuiGraphicsExtractor graphics, Minecraft minecraft, Component text, int centerX, int y,
                                int color, int alpha) {
        int width = minecraft.font.width(text);
        int x = centerX - width / 2;
        graphics.fill(x - 2, y - 1, x + width + 2, y + 9, ((alpha * 0x66 / 255) & 0xFF) << 24);
        graphics.text(minecraft.font, text, x, y, (alpha << 24) | (color & 0xFFFFFF), true);
        return y + 11;
    }

    /** The lyric line that should be on screen for the track's current playback position. */
    private static String currentLyric(Mp3Client.Track track) {
        long songId = Mp3Lyrics.songId(track.url());
        if (songId < 0) {
            return null;
        }
        List<Mp3Lyrics.Line> lines = Mp3Lyrics.lines(songId);
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        int elapsed = (int) Mth.clamp(Mp3Client.elapsedMillis(track), 0, Integer.MAX_VALUE);
        Mp3Lyrics.Line found = null;
        for (Mp3Lyrics.Line candidate : lines) {
            if (candidate.millis() > elapsed) {
                break;
            }
            found = candidate;
        }
        if (found == null) {
            return null;
        }
        return found.translation().isBlank() ? found.text() : found.text() + " / " + found.translation();
    }
}
