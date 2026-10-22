package com.wuliaoppx.netmusicplus.net;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The data Fabric sends along with the MP3 player's screen opening packet: which inventory slot holds the player and
 * how many disc slots the menu must have.
 * <p>
 * The capacity has to come from the server: the client used to derive it from its own (possibly stale) copy of the
 * item, which produced a menu with a different slot count and kicked the player with a protocol error.
 */
public record Mp3OpenData(int playerSlot, int capacity) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.playerSlot);
        buf.writeVarInt(this.capacity);
    }

    public static Mp3OpenData read(FriendlyByteBuf buf) {
        return new Mp3OpenData(buf.readVarInt(), buf.readVarInt());
    }
}
