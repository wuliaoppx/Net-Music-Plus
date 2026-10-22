package com.wuliaoppx.netmusicplus;

import com.github.tartaricacid.netmusic.init.InitItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * The amplifier enchantment (levels I-III, 200% / 300% / 400%).
 * <p>
 * 1.20.1 has no data driven enchantments, so this is a registered {@link Enchantment} subclass. The category is a
 * plain vanilla one because {@code EnchantmentCategory.create} does not exist in 1.20.1; the actual item restriction
 * is enforced by {@link #canEnchant(ItemStack)}, which is what the anvil checks when combining a book with an item.
 */
public class AmplifierEnchantment extends Enchantment {
    public AmplifierEnchantment() {
        super(Rarity.COMMON, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return AmplifierData.MAX_TIER;
    }

    /** Mirrors the reference 1.21 data: min_cost base 1, +1 per level above the first. */
    @Override
    public int getMinCost(int level) {
        return 1 + Math.max(0, level - 1);
    }

    /** Mirrors the reference 1.21 data: max_cost base 1, +2 per level above the first. */
    @Override
    public int getMaxCost(int level) {
        return 1 + Math.max(0, level - 1) * 2;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return stack.is(InitItems.MUSIC_PLAYER.get()) || stack.is(NetMusicPlus.MP3.get());
    }
}
