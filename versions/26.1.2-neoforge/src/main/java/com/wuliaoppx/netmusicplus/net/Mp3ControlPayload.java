package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.Mp3ServerPlayback;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: a transport button was pressed (the keybind), so the server can update the item's state and
 * re-broadcast the sound.
 */
public record Mp3ControlPayload(int action) implements CustomPacketPayload {
    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_NEXT = 1;
    public static final int ACTION_PREVIOUS = 2;
    /** Opens the MP3 player GUI for the player's carried (or equipped) player. */
    public static final int ACTION_OPEN = 3;

    public static final Type<Mp3ControlPayload> TYPE = new Type<>(NetMusicPlus.id("mp3_control"));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3ControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.action),
            buf -> new Mp3ControlPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Mp3ControlPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> Mp3ServerPlayback.handleControl(player, payload.action()));
        }
    }
}
