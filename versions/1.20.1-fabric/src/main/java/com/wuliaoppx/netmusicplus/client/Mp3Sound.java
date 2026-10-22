package com.wuliaoppx.netmusicplus.client;

import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
import com.github.tartaricacid.netmusic.init.InitSounds;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The MP3 player's sound: NetMusic's streamed audio, but owned by a player instead of a block.
 * <p>
 * The audible range comes from the sound engine: {@code max(volume, 1) * attenuationDistance}, with the vanilla
 * attenuation distance of 16 for NetMusic's sound event. So a volume of {@code 4.0} is the familiar 64 block range.
 * Personal playback uses no attenuation at all, which makes the sound equally loud everywhere.
 * <p>
 * On Fabric the URL is streamed through {@link FabricSoundInstance#getAudioStream}: vanilla has no hook on
 * {@code SoundInstance} for a custom stream (that one is a NeoForge patch), Fabric API adds it.
 */
public class Mp3Sound extends AbstractTickableSoundInstance implements FabricSoundInstance {
    private final URL url;
    private final UUID owner;
    private final boolean personal;
    private final int durationTicks;
    private int ticks;

    public Mp3Sound(UUID owner, URL url, double x, double y, double z, float volume, boolean personal, int durationTicks) {
        super(InitSounds.NET_MUSIC, SoundSource.RECORDS, RandomSource.create());
        this.owner = owner;
        this.url = url;
        this.personal = personal;
        this.durationTicks = Math.max(20, durationTicks);
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = 1.0F;
        this.volume = personal ? 1.0F : volume;
        this.attenuation = personal ? SoundInstance.Attenuation.NONE : SoundInstance.Attenuation.LINEAR;
    }

    @Override
    public void tick() {
        this.ticks++;
        if (!this.personal) {
            // Follow the owner so the music does not stay behind when they walk away.
            if (Minecraft.getInstance().level != null) {
                Player player = Minecraft.getInstance().level.getPlayerByUUID(this.owner);
                if (player != null) {
                    this.x = player.getX();
                    this.y = player.getY() + 1.0D;
                    this.z = player.getZ();
                }
            }
        }
        if (this.ticks > this.durationTicks + 40) {
            this.stop();
        }
    }

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, ResourceLocation soundId,
                                                         boolean looping) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return new NetMusicAudioStream(this.url);
            } catch (IOException | UnsupportedAudioFileException exception) {
                NetMusicPlus.LOGGER.error("Failed to open the audio stream for {}", this.url, exception);
                // Failing the future makes the sound engine skip playback instead of crashing.
                throw new CompletionException(exception);
            }
        }, Util.backgroundExecutor());
    }
}
