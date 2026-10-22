package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import com.wuliaoppx.netmusicplus.client.Mp3Client;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

    /** Runs on the client only; the dedicated server never plays this sound. */
    public static void handle(Mp3PlayPayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> Mp3Client.startSound(payload));
    }
}
