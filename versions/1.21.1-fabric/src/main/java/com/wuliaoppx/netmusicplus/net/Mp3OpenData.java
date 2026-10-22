package com.wuliaoppx.netmusicplus.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The data Fabric sends along with the MP3 player's screen opening packet: which inventory slot holds the player and
 * how many disc slots the menu must have.
 * <p>
 * The capacity has to come from the server: the client used to derive it from its own (possibly stale) copy of the
 * item, which produced a menu with a different slot count and kicked the player with a protocol error.
 */
public record Mp3OpenData(int playerSlot, int capacity) {
    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3OpenData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Mp3OpenData::playerSlot,
            ByteBufCodecs.VAR_INT, Mp3OpenData::capacity,
            Mp3OpenData::new);
}
