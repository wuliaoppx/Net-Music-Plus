package com.wuliaoppx.netmusicplus.net;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

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
                             int durationTicks, boolean personal, float volume) {
    public static final ResourceLocation ID = NetMusicPlus.id("mp3_play");

    public void write(FriendlyByteBuf buf) {
        buf.writeUUID(this.playerId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeUtf(this.url, 32767);
        buf.writeUtf(this.name, 32767);
        buf.writeVarInt(this.durationTicks);
        buf.writeBoolean(this.personal);
        buf.writeFloat(this.volume);
    }

    public static Mp3PlayPayload read(FriendlyByteBuf buf) {
        return new Mp3PlayPayload(buf.readUUID(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readUtf(32767), buf.readUtf(32767), buf.readVarInt(), buf.readBoolean(), buf.readFloat());
    }
}
