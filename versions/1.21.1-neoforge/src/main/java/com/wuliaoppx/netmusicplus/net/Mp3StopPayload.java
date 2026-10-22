package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import com.wuliaoppx.netmusicplus.client.Mp3Client;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/** Server to client: stop whatever {@link Mp3PlayPayload} started for this player. */
public record Mp3StopPayload(UUID playerId) implements CustomPacketPayload {
    public static final Type<Mp3StopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NetMusicPlus.MOD_ID, "mp3_stop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3StopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUUID(payload.playerId),
            buf -> new Mp3StopPayload(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Mp3StopPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Mp3Client.stopSound(payload.playerId()));
    }
}
