package com.wuliaoppx.netmusicplus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.Locale;

/**
 * Explains the amplifier enchantment in the tooltip: vanilla only shows the enchantment name, which says nothing about
 * what the level actually does.
 */
public final class AmplifierTooltip {
    private AmplifierTooltip() {
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.is(NetMusicPlus.EXPANDER.get())) {
            event.getToolTip().add(Component.translatable("tooltip.netmusicplus.expander")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        int level = AmplifierData.levelOf(stack);
        if (level <= 0) {
            return;
        }
        event.getToolTip().add(Component.translatable("tooltip.netmusicplus.amplifier",
                        String.format(Locale.ROOT, "%.1f", AmplifierData.multiplier(level)),
                        AmplifierData.range(level))
                .withStyle(ChatFormatting.GRAY));
        if (stack.is(Items.ENCHANTED_BOOK)) {
            event.getToolTip().add(Component.translatable("tooltip.netmusicplus.amplifier.hint")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
