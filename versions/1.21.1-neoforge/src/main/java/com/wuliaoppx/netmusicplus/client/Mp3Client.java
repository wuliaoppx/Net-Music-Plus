package com.wuliaoppx.netmusicplus.client;

import com.wuliaoppx.netmusicplus.Mp3Menu;
import com.wuliaoppx.netmusicplus.Mp3Support;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import com.wuliaoppx.netmusicplus.net.Mp3ControlPayload;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client side glue: the sound instances, the screen, the keybinds and the data the HUD needs.
 * <p>
 * Keybind gestures: hold to pause/resume, tap once for the next track, double tap for the previous one. The server is
 * told about every gesture so the item's state (and therefore every other client) stays in sync.
 */
public final class Mp3Client {
    private static final Map<UUID, Mp3Sound> SOUNDS = new HashMap<>();
    private static final int LONG_PRESS_TICKS = 10;
    private static final int DOUBLE_TAP_TICKS = 7;

    public static final KeyMapping TOGGLE = new KeyMapping("key.netmusicplus.mp3",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.netmusicplus");
    /** Opens the disc tray of the MP3 player in the inventory (or the curio slot). Unbound by default. */
    public static final KeyMapping OPEN = new KeyMapping("key.netmusicplus.mp3.open",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.netmusicplus");

    /** What the HUD shows about the local player's own track. */
    public record Track(String name, String url, long startMillis, int durationTicks) {
    }

    private static Track track;

    private static int heldTicks = -1;
    private static int pendingNext = -1;
    private static int taps;

    /** Short "what just happened" line shown above the hotbar after a keybind gesture. */
    private static Component actionText;
    private static long actionStart;
    private static long actionEnd;

    private static final int ACTION_MILLIS = 1400;

    private Mp3Client() {
    }

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
        event.register(OPEN);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(NetMusicPlus.MP3_MENU.get(), Mp3Screen::new);
    }

    /** The track the local player's own MP3 player is playing, or {@code null} when nothing is playing. */
    public static Track currentTrack() {
        return track;
    }

    public static long elapsedMillis(Track track) {
        return Util.getMillis() - track.startMillis();
    }

    public static void startSound(Mp3PlayPayload payload) {
        stopSound(payload.playerId());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && payload.playerId().equals(minecraft.player.getUUID())) {
            track = new Track(payload.name(), payload.url(), Util.getMillis(), payload.durationTicks());
            // Always fetch: the overlay can be switched on halfway through the song and must not stay empty.
            Mp3Lyrics.prefetch(Mp3Lyrics.songId(payload.url()));
        }
        try {
            Mp3Sound sound = new Mp3Sound(payload.playerId(), URI.create(payload.url()).toURL(),
                    payload.x(), payload.y(), payload.z(), payload.volume(), payload.personal(),
                    payload.durationTicks());
            SOUNDS.put(payload.playerId(), sound);
            minecraft.getSoundManager().play(sound);
        } catch (Exception exception) {
            NetMusicPlus.LOGGER.error("Cannot play MP3 track {}", payload.url(), exception);
        }
    }

    public static void stopSound(UUID playerId) {
        Mp3Sound sound = SOUNDS.remove(playerId);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().stop(sound);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && playerId.equals(minecraft.player.getUUID())) {
            track = null;
        }
    }

    /** The gesture feedback line, or {@code null} once it has faded out. */
    public static Component actionText() {
        return Util.getMillis() < actionEnd ? actionText : null;
    }

    /** 0-255 alpha for the gesture feedback line; it fades out over the last third of its life. */
    public static int actionAlpha() {
        long left = actionEnd - Util.getMillis();
        if (left <= 0) {
            return 0;
        }
        long fade = ACTION_MILLIS / 3L;
        return (int) (255L * Math.min(1.0D, left / (double) fade));
    }

    private static void showAction(String key) {
        actionText = Component.translatable(key);
        actionStart = Util.getMillis();
        actionEnd = actionStart + ACTION_MILLIS;
    }

    /** Whether the local player's own MP3 player is currently playing (read from the item, client side). */
    private static boolean isPlaying() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        ItemStack stack = Mp3Support.findActive(minecraft.player);
        return !stack.isEmpty() && Mp3Support.state(stack).playing();
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            heldTicks = -1;
            return;
        }
        while (OPEN.consumeClick()) {
            send(Mp3ControlPayload.ACTION_OPEN);
        }
        boolean down = minecraft.screen == null && TOGGLE.isDown();
        if (down) {
            heldTicks = heldTicks < 0 ? 1 : heldTicks + 1;
        } else if (heldTicks >= 0) {
            int held = heldTicks;
            heldTicks = -1;
            if (held >= LONG_PRESS_TICKS) {
                taps = 0;
                pendingNext = -1;
                showAction(isPlaying() ? "hud.netmusicplus.action.pause" : "hud.netmusicplus.action.resume");
                send(Mp3ControlPayload.ACTION_TOGGLE);
            } else if (++taps >= 2) {
                taps = 0;
                pendingNext = -1;
                showAction("hud.netmusicplus.action.previous");
                send(Mp3ControlPayload.ACTION_PREVIOUS);
            } else {
                pendingNext = DOUBLE_TAP_TICKS;
            }
        }
        if (pendingNext > 0 && --pendingNext == 0) {
            taps = 0;
            showAction("hud.netmusicplus.action.next");
            send(Mp3ControlPayload.ACTION_NEXT);
        }
    }

    private static void send(int action) {
        PacketDistributor.sendToServer(new Mp3ControlPayload(action));
    }
}
