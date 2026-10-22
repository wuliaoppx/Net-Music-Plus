package com.wuliaoppx.netmusicplus;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The amplifier level of a placed music player, and the multiplier it stands for.
 * <p>
 * The level lives in {@link BlockEntity#getPersistentData()}: NeoForge writes that tag into {@code NeoForgeData} in
 * {@code saveWithoutMetadata}, which is exactly what NetMusic's music player returns from {@code getUpdateTag}. So the
 * level is saved with the world and synced to clients without needing our own block entity (or a mixin into theirs).
 */
public final class AmplifierData {
    public static final String TIER_KEY = "netmusicplus_tier";
    /** Vanilla attenuation distance for a sound instance with {@code volume = 4.0F}: {@code 4 * 16}. */
    public static final float BASE_RADIUS = 64.0F;
    public static final int MAX_TIER = 3;

    private AmplifierData() {
    }

    public static float multiplier(int tier) {
        return NetMusicPlusConfig.multiplier(tier);
    }

    public static int range(int tier) {
        return Math.round(BASE_RADIUS * multiplier(tier));
    }

    public static String roman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> "-";
        };
    }

    public static int getTier(BlockEntity blockEntity) {
        return blockEntity == null ? 0 : blockEntity.getPersistentData().getInt(TIER_KEY);
    }

    public static int getTier(LevelReader level, BlockPos pos) {
        return level == null ? 0 : getTier(level.getBlockEntity(pos));
    }

    public static float multiplierAt(LevelReader level, BlockPos pos) {
        return multiplier(getTier(level, pos));
    }

    /**
     * Sets (or clears, when {@code tier <= 0}) the installed amplifier and pushes the change to tracking clients.
     */
    public static void setTier(Level level, BlockPos pos, BlockEntity blockEntity, int tier) {
        CompoundTag data = blockEntity.getPersistentData();
        if (tier <= 0) {
            data.remove(TIER_KEY);
        } else {
            data.putInt(TIER_KEY, Math.min(tier, MAX_TIER));
        }
        blockEntity.setChanged();
        if (level != null) {
            level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The amplifier level carried by {@code stack} as an enchantment, or {@code 0} when it has none. */
    public static int levelFromStack(ItemStack stack) {
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            if (entry.getKey().is(NetMusicPlus.AMPLIFIER)) {
                return Math.min(entry.getIntValue(), MAX_TIER);
            }
        }
        return 0;
    }

    /** Like {@link #levelFromStack} but also looks at the enchantments stored in an enchanted book. */
    public static int levelOf(ItemStack stack) {
        int level = levelFromStack(stack);
        if (level > 0) {
            return level;
        }
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) {
            return 0;
        }
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stored.entrySet()) {
            if (entry.getKey().is(NetMusicPlus.AMPLIFIER)) {
                return Math.min(entry.getIntValue(), MAX_TIER);
            }
        }
        return 0;
    }

    /**
     * Writes the amplifier level onto {@code stack} as a real enchantment component.
     * <p>
     * Note: {@code EnchantmentHelper#updateEnchantments} is a no-op when the stack has no enchantment component yet
     * (it returns {@code ItemEnchantments.EMPTY} and never calls the updater), which silently produced plain items.
     */
    public static void setLevel(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        DataComponentType<ItemEnchantments> type = stack.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS
                : DataComponents.ENCHANTMENTS;
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(type, ItemEnchantments.EMPTY));
        mutable.set(enchantment, Math.min(level, MAX_TIER));
        stack.set(type, mutable.toImmutable());
    }

    public static ItemStack book(Holder<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        setLevel(stack, enchantment, level);
        return stack;
    }
}
