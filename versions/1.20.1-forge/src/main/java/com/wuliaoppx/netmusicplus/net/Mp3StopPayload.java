package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Server to client: stop whatever {@link Mp3PlayPayload} started for this player. */
public record Mp3StopPayload(UUID playerId) {
    public static final ResourceLocation ID = NetMusicPlus.id("mp3_stop");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
    }

    public static Mp3StopPayload read(FriendlyByteBuf buf) {
        return new Mp3StopPayload(buf.readUUID());
    }
}
