package com.wuliaoppx.netmusicplus;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Everything the MP3 player remembers: the loaded discs, which one is selected, the play mode, whether it plays out
 * loud, whether it is currently playing, and how many storage expansions are installed.
 * <p>
 * Stored in the item stack's NBT by {@link Mp3Nbt}, which means it is saved with the stack (inventories, trinket
 * slots, shulker boxes) and synced to the owning client by vanilla, without any custom block or block entity.
 */
public record Mp3State(List<ItemStack> discs, int index, PlayMode mode, boolean external, boolean playing, int upgrades) {
    /** Slots a fresh MP3 player has. */
    public static final int BASE_DISCS = 20;
    /** Extra slots per installed storage expansion. */
    public static final int DISCS_PER_UPGRADE = 10;
    /** How many expansions fit into one MP3 player. */
    public static final int MAX_UPGRADES = 3;
    /** Slots of a fully upgraded MP3 player. */
    public static final int MAX_DISCS = BASE_DISCS + DISCS_PER_UPGRADE * MAX_UPGRADES;

    public static final Mp3State EMPTY = new Mp3State(List.of(), 0, PlayMode.LOOP, false, false, 0);

    @Override
    public List<ItemStack> discs() {
        return this.discs.size() > MAX_DISCS ? this.discs.subList(0, MAX_DISCS) : this.discs;
    }

    /** Number of disc slots the player currently has: {@code 20} plus {@code 10} per installed expansion. */
    public int capacity() {
        return BASE_DISCS + DISCS_PER_UPGRADE * Math.max(0, Math.min(this.upgrades, MAX_UPGRADES));
    }

    public boolean hasDiscs() {
        for (ItemStack disc : this.discs) {
            if (!disc.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack currentDisc() {
        return this.index >= 0 && this.index < this.discs.size() ? this.discs.get(this.index) : ItemStack.EMPTY;
    }

    /** The selected slot, moved forward when it is empty and something playable exists. */
    public int playableIndex() {
        if (!this.currentDisc().isEmpty()) {
            return this.index;
        }
        for (int i = 0; i < this.discs.size(); i++) {
            if (!this.discs.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * The slot to play next.
     *
     * @param direction  1 for forward, -1 for backward
     * @param automatic  whether this happens because a song ended (honours {@link PlayMode#SINGLE}) or because the
     *                   player pressed a button (always moves)
     */
    public int nextIndex(int direction, boolean automatic, RandomSource random) {
        int size = this.discs.size();
        if (size == 0) {
            return 0;
        }
        if (automatic && this.mode == PlayMode.SINGLE) {
            return this.index;
        }
        if (automatic && this.mode == PlayMode.SHUFFLE && size > 1) {
            for (int attempt = 0; attempt < 32; attempt++) {
                int candidate = random.nextInt(size);
                if (candidate != this.index && !this.discs.get(candidate).isEmpty()) {
                    return candidate;
                }
            }
        }
        for (int i = 1; i <= size; i++) {
            int candidate = Math.floorMod(this.index + direction * i, size);
            if (!this.discs.get(candidate).isEmpty()) {
                return candidate;
            }
        }
        return this.index;
    }

    public Mp3State withDiscs(List<ItemStack> newDiscs) {
        return new Mp3State(List.copyOf(newDiscs), this.index, this.mode, this.external, this.playing, this.upgrades);
    }

    public Mp3State withIndex(int newIndex) {
        return new Mp3State(this.discs, newIndex, this.mode, this.external, this.playing, this.upgrades);
    }

    public Mp3State withMode(PlayMode newMode) {
        return new Mp3State(this.discs, this.index, newMode, this.external, this.playing, this.upgrades);
    }

    public Mp3State withExternal(boolean newExternal) {
        return new Mp3State(this.discs, this.index, this.mode, newExternal, this.playing, this.upgrades);
    }

    public Mp3State withPlaying(boolean newPlaying) {
        return new Mp3State(this.discs, this.index, this.mode, this.external, newPlaying, this.upgrades);
    }

    public Mp3State withUpgrades(int newUpgrades) {
        return new Mp3State(this.discs, this.index, this.mode, this.external, this.playing,
                Math.max(0, Math.min(newUpgrades, MAX_UPGRADES)));
    }
}
