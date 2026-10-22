package com.wuliaoppx.netmusicplus;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * Puts the amplifier enchantment back onto the music player item when the block is mined.
 * <p>
 * The level is stored on the block entity (see {@link AmplifierData}), not on the block itself, so the loot table
 * produces a clean item; this handler re-applies the enchantment to the dropped stack so the upgrade follows the
 * block. NetMusic adds the inserted disc to the same drop list, hence the item filter.
 */
@EventBusSubscriber(modid = NetMusicPlus.MOD_ID)
public final class AmplifierEvents {
    private AmplifierEvents() {
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof TileEntityMusicPlayer musicPlayer)) {
            return;
        }
        int tier = AmplifierData.getTier(musicPlayer);
        if (tier <= 0 || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Holder<Enchantment> enchantment = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(NetMusicPlus.AMPLIFIER);

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (!stack.is(InitBlocks.MUSIC_PLAYER.get().asItem())) {
                continue;
            }
            ItemStack enchanted = stack.copy();
            AmplifierData.setLevel(enchanted, enchantment, tier);
            drop.setItem(enchanted);
        }
    }
}
