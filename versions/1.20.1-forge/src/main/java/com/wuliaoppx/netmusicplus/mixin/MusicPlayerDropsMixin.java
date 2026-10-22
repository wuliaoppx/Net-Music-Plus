package com.wuliaoppx.netmusicplus.mixin;

import com.github.tartaricacid.netmusic.block.BlockMusicPlayer;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.wuliaoppx.netmusicplus.AmplifierData;
import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Puts the amplifier enchantment back onto the music player item when the block is mined.
 * <p>
 * The level is stored on the block entity (see {@code AmplifierData}), not on the block, so the loot table produces a
 * clean item; this hook re-applies the enchantment to the dropped stack so the upgrade follows the block. NetMusic adds
 * the inserted disc to the same list, hence the item filter.
 */
@Mixin(BlockMusicPlayer.class)
public abstract class MusicPlayerDropsMixin {
    // SRG name + remap = false: the target is NetMusic's override, which ships in production names already.
    @Inject(method = "m_49635_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/storage/loot/LootParams$Builder;)Ljava/util/List;",
            at = @At("RETURN"), remap = false)
    private void netmusicplus$keepAmplifier(BlockState state, LootParams.Builder params,
                                            CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockEntity blockEntity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        int tier = AmplifierData.getTier(blockEntity);
        List<ItemStack> drops = cir.getReturnValue();
        if (tier <= 0 || drops == null || !(params.getLevel() instanceof ServerLevel)) {
            return;
        }
        Enchantment enchantment = NetMusicPlus.AMPLIFIER.get();
        for (ItemStack stack : drops) {
            if (stack.is(InitBlocks.MUSIC_PLAYER.get().asItem())) {
                AmplifierData.setLevel(stack, enchantment, tier);
            }
        }
    }
}
