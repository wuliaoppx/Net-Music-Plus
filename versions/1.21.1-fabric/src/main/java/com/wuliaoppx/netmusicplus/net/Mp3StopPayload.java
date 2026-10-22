package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TYPE, STREAM_CODEC);
    }
}
