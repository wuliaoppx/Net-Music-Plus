package com.wuliaoppx.netmusicplus;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes {@link Mp3State} to the MP3 player stack's NBT tag.
 * <p>
 * 1.20.1 has no data components, so the state lives in a single {@code netmusicplus} compound on the item. Item NBT
 * is part of the stack itself: it is saved with inventories and shulker boxes and sent to the owning client by
 * vanilla, so no extra packet or block entity is needed.
 * <p>
 * All key names are centralised here; {@link Mp3State} stays a plain record.
 */
public final class Mp3Nbt {
    /** Root compound that holds everything below. */
    public static final String ROOT = "netmusicplus";
    public static final String DISCS = "discs";
    public static final String INDEX = "index";
    public static final String MODE = "mode";
    public static final String EXTERNAL = "external";
    public static final String PLAYING = "playing";
    public static final String UPGRADES = "upgrades";

    private Mp3Nbt() {
    }

    public static Mp3State read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) {
            return Mp3State.EMPTY;
        }
        CompoundTag root = tag.getCompound(ROOT);
        List<ItemStack> discs = new ArrayList<>();
        ListTag list = root.getList(DISCS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            discs.add(ItemStack.of(list.getCompound(i)));
        }
        return new Mp3State(discs,
                root.getInt(INDEX),
                PlayMode.byName(root.getString(MODE)),
                root.getBoolean(EXTERNAL),
                root.getBoolean(PLAYING),
                root.getInt(UPGRADES));
    }

    public static void write(ItemStack stack, Mp3State state) {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        // Empty slots are written as empty compounds so the slot order is preserved.
        for (ItemStack disc : state.discs()) {
            CompoundTag entry = new CompoundTag();
            if (!disc.isEmpty()) {
                disc.save(entry);
            }
            list.add(entry);
        }
        root.put(DISCS, list);
        root.putInt(INDEX, state.index());
        root.putString(MODE, state.mode().getSerializedName());
        root.putBoolean(EXTERNAL, state.external());
        root.putBoolean(PLAYING, state.playing());
        root.putInt(UPGRADES, state.upgrades());
        stack.getOrCreateTag().put(ROOT, root);
    }
}
