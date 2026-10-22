package com.wuliaoppx.netmusicplus;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.wuliaoppx.netmusicplus.net.Mp3ControlPayload;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.wuliaoppx.netmusicplus.net.Mp3StopPayload;
import com.wuliaoppx.netmusicplus.net.Net;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server side playback driver.
 * <p>
 * The server owns the timeline: it decides when a track starts, when it is over and which track comes next (loop,
 * single or shuffle), writes the result back into the item's {@link Mp3State} and tells the clients what to play. The
 * clients only stream the audio, so two players can never drift apart on what "the next track" means.
 */
public final class Mp3ServerPlayback {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private record Session(int index, long start, int duration, boolean external, float radius) {
    }

    private Mp3ServerPlayback() {
    }

    /** Subscribed to the forge event bus from the mod constructor. */
    public static final class Events {
        private Events() {
        }

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                tick(player);
            }
        }

        @SubscribeEvent
        public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Session session = SESSIONS.remove(player.getUUID());
                if (session != null) {
                    stop(player, session);
                }
            }
        }

        @SubscribeEvent
        public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                Session session = SESSIONS.remove(player.getUUID());
                if (session != null) {
                    stop(player, session);
                }
            }
        }
    }

    private static void tick(ServerPlayer player) {
        UUID id = player.getUUID();
        ItemStack mp3 = Mp3Support.findActive(player);
        Session session = SESSIONS.get(id);
        Mp3State state = mp3.isEmpty() ? null : Mp3Support.state(mp3);

        if (state == null || !state.playing() || !state.hasDiscs()) {
            if (session != null) {
                SESSIONS.remove(id);
                stop(player, session);
            }
            return;
        }

        int index = state.playableIndex();
        if (index < 0) {
            if (session != null) {
                SESSIONS.remove(id);
                stop(player, session);
            }
            return;
        }

        if (session != null && (session.index() != index || session.external() != state.external())) {
            SESSIONS.remove(id);
            stop(player, session);
            session = null;
        }

        long now = player.level().getGameTime();
        if (session == null) {
            start(player, mp3, state, index, now);
            return;
        }
        if (now - session.start() >= session.duration()) {
            int next = state.nextIndex(1, true, player.getRandom());
            Mp3State updated = state.withIndex(next);
            Mp3Support.setState(mp3, updated);
            player.getInventory().setChanged();
            start(player, mp3, updated, next, now);
        }
    }

    private static void start(ServerPlayer player, ItemStack mp3, Mp3State state, int index, long now) {
        ItemStack disc = index >= 0 && index < state.discs().size() ? state.discs().get(index) : ItemStack.EMPTY;
        ItemMusicCD.SongInfo info = Mp3Support.songInfo(disc);
        if (info == null || info.songUrl == null || info.songUrl.isBlank()) {
            return;
        }
        int duration = Math.max(20, Mp3Support.durationTicks(disc));
        float radius = radius(mp3);
        float volume = Math.max(1.0F, radius / 16.0F);
        boolean external = state.external();
        Mp3PlayPayload payload = new Mp3PlayPayload(player.getUUID(), player.getX(), player.getY(), player.getZ(),
                info.songUrl, Mp3Support.trackName(disc), duration, !external, volume);
        if (external) {
            Net.sendToNear(player.level(), player.getX(), player.getY(), player.getZ(), radius, payload);
        } else {
            Net.sendTo(player, payload);
        }
        SESSIONS.put(player.getUUID(), new Session(index, now, duration, external, radius));
        refreshOpenMenu(player);
    }

    private static void stop(ServerPlayer player, Session session) {
        Mp3StopPayload payload = new Mp3StopPayload(player.getUUID());
        if (session.external()) {
            // The same radius the track started with, otherwise listeners at the far edge never receive the stop.
            Net.sendToNear(player.level(), player.getX(), player.getY(), player.getZ(), session.radius(), payload);
        } else {
            Net.sendTo(player, payload);
        }
        refreshOpenMenu(player);
    }

    /** Out loud radius of the MP3 player: the configured base, scaled by the amplifier enchantment on the item. */
    private static float radius(ItemStack mp3) {
        float multiplier = mp3.isEmpty() ? 1.0F : AmplifierData.multiplier(AmplifierData.levelFromStack(mp3));
        return Math.max(16.0F, NetMusicPlusConfig.outLoudRadius() * multiplier);
    }

    private static void refreshOpenMenu(ServerPlayer player) {
        if (player.containerMenu instanceof Mp3Menu menu) {
            menu.refresh();
        }
    }

    /** Applies a transport keybind (toggle / next / previous) or opens the GUI when the player asked for it. */
    public static void handleControl(ServerPlayer player, int action) {
        ItemStack mp3 = Mp3Support.findActive(player);
        if (mp3.isEmpty()) {
            return;
        }
        if (action == Mp3ControlPayload.ACTION_OPEN) {
            Mp3Menu.open(player);
            return;
        }
        Mp3State state = Mp3Support.state(mp3);
        state = switch (action) {
            case Mp3ControlPayload.ACTION_TOGGLE -> state.withPlaying(!state.playing());
            case Mp3ControlPayload.ACTION_NEXT -> state.withIndex(state.nextIndex(1, false, player.getRandom()));
            case Mp3ControlPayload.ACTION_PREVIOUS -> state.withIndex(state.nextIndex(-1, false, player.getRandom()));
            default -> state;
        };
        Mp3Support.setState(mp3, state);
        player.getInventory().setChanged();
        refreshOpenMenu(player);
    }
}
