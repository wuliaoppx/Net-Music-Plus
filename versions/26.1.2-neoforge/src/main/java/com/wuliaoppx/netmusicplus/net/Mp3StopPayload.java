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

/** Server to client: stop whatever {@link Mp3PlayPayload} started for this player. */
public record Mp3StopPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<Mp3StopPayload> TYPE = new Type<>(NetMusicPlus.id("mp3_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3StopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUUID(payload.playerId),
            buf -> new Mp3StopPayload(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Runs on the client only; the dedicated server never plays this sound. */
    public static void handle(Mp3StopPayload payload, IPayloadContext context) {
        if (FMLEnvironment.getDist() != Dist.CLIENT) {
            return;
        }
        context.enqueueWork(() -> Mp3Client.stopSound(payload.playerId()));
    }
}
