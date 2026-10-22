package com.wuliaoppx.netmusicplus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.List;
import java.util.Locale;

/**
 * Explains the amplifier enchantment in the tooltip: vanilla only shows the enchantment name, which says nothing about
 * what the level actually does.
 */
public final class AmplifierTooltip {
    private AmplifierTooltip() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(AmplifierTooltip::onTooltip);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        append(event.getItemStack(), event.getToolTip());
    }

    private static void append(ItemStack stack, List<Component> lines) {
        if (stack.is(NetMusicPlus.EXPANDER.get())) {
            lines.add(Component.translatable("tooltip.netmusicplus.expander").withStyle(ChatFormatting.DARK_GRAY));
        }
        int level = AmplifierData.levelOf(stack);
        if (level <= 0) {
            return;
        }
        lines.add(Component.translatable("tooltip.netmusicplus.amplifier",
                        String.format(Locale.ROOT, "%.1f", AmplifierData.multiplier(level)),
                        AmplifierData.range(level))
                .withStyle(ChatFormatting.GRAY));
        if (stack.is(Items.ENCHANTED_BOOK)) {
            lines.add(Component.translatable("tooltip.netmusicplus.amplifier.hint").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
