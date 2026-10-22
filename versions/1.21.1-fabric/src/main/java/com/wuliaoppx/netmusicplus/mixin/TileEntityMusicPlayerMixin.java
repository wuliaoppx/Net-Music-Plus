package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.wuliaoppx.netmusicplus.AmplifierData;
import com.wuliaoppx.netmusicplus.AmplifierHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the amplifier level to NetMusic's music player block entity.
 * <p>
 * Fabric has no equivalent of NeoForge's {@code BlockEntity#getPersistentData()}, so the level needs its own field. The
 * field is written in {@code saveAdditional}, which NetMusic's {@code getUpdateTag} already goes through
 * ({@code saveWithoutMetadata}), so the value is saved with the world and synced to tracking clients for free.
 */
@Mixin(TileEntityMusicPlayer.class)
public abstract class TileEntityMusicPlayerMixin implements AmplifierHolder {
    @Unique
    private int netmusicplus$tier;

    @Override
    public int netmusicplus$tier() {
        return this.netmusicplus$tier;
    }

    @Override
    public void netmusicplus$setTier(int tier) {
        this.netmusicplus$tier = tier;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void netmusicplus$saveAmplifier(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.netmusicplus$tier > 0) {
            tag.putInt(AmplifierData.TIER_KEY, this.netmusicplus$tier);
        }
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void netmusicplus$loadAmplifier(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.netmusicplus$tier = tag.getInt(AmplifierData.TIER_KEY);
    }
}
