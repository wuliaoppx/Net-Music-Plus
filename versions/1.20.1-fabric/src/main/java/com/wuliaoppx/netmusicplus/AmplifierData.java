package com.wuliaoppx.netmusicplus;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The amplifier level of a placed music player, and the multiplier it stands for.
 * <p>
 * The level lives on NetMusic's block entity through {@link AmplifierHolder} (see
 * {@code mixin.TileEntityMusicPlayerMixin}): it is written into the block entity's own NBT, which NetMusic already
 * saves and syncs to clients, so no separate block entity is needed.
 * <p>
 * On 1.20.1 an enchantment is stored in the plain {@code Enchantments} / {@code StoredEnchantments} NBT lists
 * ({@code {id: "netmusicplus:amplifier", lvl: 2s}}), so the level is read and written directly there. Vanilla's anvil
 * writes exactly the same structure, which is what makes the upgrade work.
 */
public final class AmplifierData {
    public static final String TIER_KEY = "netmusicplus_tier";
    public static final String ENCHANTMENTS_KEY = "Enchantments";
    public static final String STORED_ENCHANTMENTS_KEY = "StoredEnchantments";
    private static final String ID_KEY = "id";
    private static final String LEVEL_KEY = "lvl";
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
        return blockEntity instanceof AmplifierHolder holder ? holder.netmusicplus$tier() : 0;
    }

    public static int getTier(LevelReader level, BlockPos pos) {
        return level == null ? 0 : getTier(level.getBlockEntity(pos));
    }

    public static float multiplierAt(LevelReader level, BlockPos pos) {
        return multiplier(getTier(level, pos));
    }

    /** Sets (or clears, when {@code tier <= 0}) the installed amplifier and pushes the change to tracking clients. */
    public static void setTier(Level level, BlockPos pos, BlockEntity blockEntity, int tier) {
        if (!(blockEntity instanceof AmplifierHolder holder)) {
            return;
        }
        holder.netmusicplus$setTier(Math.max(0, Math.min(tier, MAX_TIER)));
        blockEntity.setChanged();
        if (level != null) {
            level.sendBlockUpdated(pos, blockEntity.getBlockState(), blockEntity.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Copies the enchantment of a freshly placed item onto its block entity. */
    public static void copyToBlockEntity(Level level, BlockPos pos, ItemStack stack) {
        int tier = levelOf(stack);
        if (tier <= 0) {
            return;
        }
        setTier(level, pos, level.getBlockEntity(pos), tier);
    }

    /** The amplifier level carried by {@code stack} as an enchantment, or {@code 0} when it has none. */
    public static int levelFromStack(ItemStack stack) {
        return readLevel(stack.getTag(), ENCHANTMENTS_KEY);
    }

    /** Like {@link #levelFromStack} but also looks at the enchantments stored in an enchanted book. */
    public static int levelOf(ItemStack stack) {
        int level = levelFromStack(stack);
        if (level > 0) {
            return level;
        }
        return readLevel(stack.getTag(), STORED_ENCHANTMENTS_KEY);
    }

    private static int readLevel(CompoundTag tag, String key) {
        if (tag == null || !tag.contains(key, Tag.TAG_LIST)) {
            return 0;
        }
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (NetMusicPlus.AMPLIFIER_ID.toString().equals(entry.getString(ID_KEY))) {
                return Math.min(Math.max(entry.getInt(LEVEL_KEY), 0), MAX_TIER);
            }
        }
        return 0;
    }

    /**
     * Writes the amplifier level onto {@code stack} in the vanilla enchantment NBT format.
     * <p>
     * Enchanted books keep their enchantments in {@code StoredEnchantments}, every other item in
     * {@code Enchantments}; an existing amplifier entry is replaced so the level never stacks.
     */
    public static void setLevel(ItemStack stack, Enchantment enchantment, int level) {
        if (level <= 0) {
            return;
        }
        String key = stack.is(Items.ENCHANTED_BOOK) ? STORED_ENCHANTMENTS_KEY : ENCHANTMENTS_KEY;
        CompoundTag tag = stack.getOrCreateTag();
        ListTag existing = tag.getList(key, Tag.TAG_COMPOUND);
        ListTag result = new ListTag();
        for (int i = 0; i < existing.size(); i++) {
            CompoundTag entry = existing.getCompound(i);
            if (!NetMusicPlus.AMPLIFIER_ID.toString().equals(entry.getString(ID_KEY))) {
                result.add(entry);
            }
        }
        CompoundTag entry = new CompoundTag();
        entry.putString(ID_KEY, NetMusicPlus.AMPLIFIER_ID.toString());
        entry.putShort(LEVEL_KEY, (short) Math.min(level, MAX_TIER));
        result.add(entry);
        tag.put(key, result);
    }

    public static ItemStack book(Enchantment enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        setLevel(stack, enchantment, level);
        return stack;
    }
}
