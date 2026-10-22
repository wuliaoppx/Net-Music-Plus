package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.wuliaoppx.netmusicplus.AmplifierData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies the amplifier enchantment from the placed item onto the music player's block entity.
 * <p>
 * The enchantment lives on the item stack (that is what the anvil upgrades), while the sound radius is computed from
 * the block entity, so the two have to be joined when the block is placed. {@code Block#setPlacedBy} is the vanilla
 * hook that still has both the stack and the fresh block entity.
 */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void netmusicplus$copyAmplifier(Level level, BlockPos pos, BlockState state, LivingEntity placer,
                                            ItemStack stack, CallbackInfo ci) {
        if (!state.is(InitBlocks.MUSIC_PLAYER)) {
            return;
        }
        AmplifierData.copyToBlockEntity(level, pos, stack);
    }
}
