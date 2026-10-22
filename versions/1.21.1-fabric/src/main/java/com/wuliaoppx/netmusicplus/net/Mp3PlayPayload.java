package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

/**
 * Server to client: start playing a track for a player.
 *
 * @param playerId      whose MP3 player this is; the client keys its sound instances by it
 * @param x             source position, ignored when {@code personal} is set
 * @param durationTicks how long the track lasts; the server advances to the next one on its own
 * @param personal      {@code true} when only the owner should hear it (played without attenuation)
 * @param volume        {@code radius / 16}; the sound engine turns the volume into the audible radius
 */
public record Mp3PlayPayload(UUID playerId, double x, double y, double z, String url, String name,
                             int durationTicks, boolean personal, float volume) implements CustomPacketPayload {
    public static final Type<Mp3PlayPayload> TYPE = new Type<>(NetMusicPlus.id("mp3_play"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3PlayPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUUID(payload.playerId);
                buf.writeDouble(payload.x);
                buf.writeDouble(payload.y);
                buf.writeDouble(payload.z);
                buf.writeUtf(payload.url, 32767);
                buf.writeUtf(payload.name, 32767);
                buf.writeVarInt(payload.durationTicks);
                buf.writeBoolean(payload.personal);
                buf.writeFloat(payload.volume);
            },
            buf -> new Mp3PlayPayload(buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readUtf(32767), buf.readUtf(32767), buf.readVarInt(), buf.readBoolean(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Registered on both sides so the type is known to the payload registry. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, STREAM_CODEC);
    }
}
