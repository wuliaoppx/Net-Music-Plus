package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: a transport button was pressed (the keybind), so the server can update the item's state and
 * re-broadcast the sound.
 * <p>
 * 1.20.1 has no {@code CustomPacketPayload}; the channel id plus a plain {@link FriendlyByteBuf} is the whole protocol.
 */
public record Mp3ControlPayload(int action) {
    public static final int ACTION_TOGGLE = 0;
    public static final int ACTION_NEXT = 1;
    public static final int ACTION_PREVIOUS = 2;
    /** Opens the MP3 player GUI for the player's carried (or equipped) player. */
    public static final int ACTION_OPEN = 3;

    public static final ResourceLocation ID = NetMusicPlus.id("mp3_control");

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
    }

    public static Mp3ControlPayload read(FriendlyByteBuf buf) {
        return new Mp3ControlPayload(buf.readVarInt());
    }
}
