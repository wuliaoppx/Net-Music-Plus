package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.wuliaoppx.netmusicplus.AmplifierData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scales the volume that {@link SoundEngine#play(SoundInstance)} reads for NetMusic's music sounds.
 * <p>
 * In {@code SoundEngine#play} the volume is used twice: {@code attenuationDistance = max(volume, 1) * soundDistance}
 * (the fade-out radius) and {@code clamp(volume * categoryVolume, 0, 1)} (the actual loudness, already saturated at
 * 1.0 for anything above 1). So multiplying it only widens the audible radius; players next to the player hear no
 * change. NetMusic hardcodes {@code volume = 4.0F}, i.e. 4 * 16 = 64 blocks.
 * <p>
 * The amplifier tier is read from the client-side block entity at the sound's position; the tier reaches the client
 * through the block entity update tag. Since the volume is only read when playback starts, an amplifier installed
 * during playback applies from the next song.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Redirect(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private float netmusicplus$scaleMusicVolume(SoundInstance instance) {
        float volume = instance.getVolume();
        if (!(instance instanceof NetMusicSound)) {
            return volume;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return volume;
        }
        BlockPos pos = BlockPos.containing(instance.getX(), instance.getY(), instance.getZ());
        float multiplier = AmplifierData.multiplierAt(level, pos);
        return multiplier > 1.0F ? volume * multiplier : volume;
    }
}
