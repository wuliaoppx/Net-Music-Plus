package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.wuliaoppx.netmusicplus.AmplifierData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies the amplifier enchantment from the placed item into the block entity.
 * <p>
 * A block entity has no way to see the item that placed it, and NetMusic's {@code BlockMusicPlayer} does not override
 * {@code setPlacedBy}. Injecting at the head of {@link Block#setPlacedBy} runs right after the block (and therefore the
 * block entity) exists, and before the item stack is discarded.
 */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "setPlacedBy", at = @At("HEAD"))
    private void netmusicplus$storeAmplifierLevel(Level level, BlockPos pos, BlockState state,
                                                      LivingEntity placer, ItemStack stack, CallbackInfo callback) {
        if (level.isClientSide() || !state.is(InitBlocks.MUSIC_PLAYER.get())) {
            return;
        }
        int tier = AmplifierData.levelFromStack(stack);
        if (tier <= 0) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            AmplifierData.setTier(level, pos, blockEntity, tier);
        }
    }
}
