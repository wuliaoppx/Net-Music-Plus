package com.wuliaoppx.netmusicplus;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The MP3 player: holds up to {@link Mp3State#MAX_DISCS} NetMusic discs and plays them from the inventory or a curio slot. */
public class Mp3PlayerItem extends Item {
    public Mp3PlayerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            Mp3Menu.open(serverPlayer);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Mp3State state = Mp3Support.state(stack);
        tooltip.add(Component.translatable("mp3.netmusicplus.output."
                        + (state.external() ? "out_loud" : "personal")).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mp3.netmusicplus.mode_label", state.mode().displayName())
                .withStyle(ChatFormatting.GRAY));
        if (!state.hasDiscs()) {
            tooltip.add(Component.translatable("mp3.netmusicplus.no_discs").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            ItemStack disc = state.currentDisc().isEmpty() ? ItemStack.EMPTY : state.currentDisc();
            if (!disc.isEmpty()) {
                tooltip.add(Component.translatable("mp3.netmusicplus.track", Mp3Support.trackName(disc))
                        .withStyle(ChatFormatting.GRAY));
            }
            tooltip.add(Component.translatable("mp3.netmusicplus.disc_count", state.discs().size(),
                    Mp3State.MAX_DISCS).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.translatable("mp3.netmusicplus.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
