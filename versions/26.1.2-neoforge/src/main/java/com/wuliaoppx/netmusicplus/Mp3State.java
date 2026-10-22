package com.wuliaoppx.netmusicplus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Everything the MP3 player remembers: the loaded discs, which one is selected, the play mode, whether it plays out
 * loud, whether it is currently playing, and how many storage expansions are installed.
 * <p>
 * Stored as a data component on the item, which means it is saved with the stack (inventories, curios slots, shulker
 * boxes) and synced to the owning client by vanilla, without any custom block or block entity.
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

    /**
     * The disc slots are a fixed length list that is mostly empty, so the item codecs must be the <em>optional</em>
     * ones: {@code ItemStack.CODEC} / {@code ItemStack.STREAM_CODEC} reject air stacks outright
     * ({@code "Item must not be minecraft:air"}, {@code "Empty ItemStack not allowed"}), which made the component
     * impossible to save or sync as soon as a single slot was empty.
     */
    public static final Codec<Mp3State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("discs", List.of()).forGetter(Mp3State::discs),
            Codec.INT.optionalFieldOf("index", 0).forGetter(Mp3State::index),
            PlayMode.CODEC.optionalFieldOf("mode", PlayMode.LOOP).forGetter(Mp3State::mode),
            Codec.BOOL.optionalFieldOf("external", false).forGetter(Mp3State::external),
            Codec.BOOL.optionalFieldOf("playing", false).forGetter(Mp3State::playing),
            Codec.INT.optionalFieldOf("upgrades", 0).forGetter(Mp3State::upgrades)
    ).apply(instance, Mp3State::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Mp3State> STREAM_CODEC = StreamCodec.of(
            (buf, state) -> {
                ItemStack.OPTIONAL_LIST_STREAM_CODEC.encode(buf, state.discs);
                buf.writeVarInt(state.index);
                buf.writeVarInt(state.mode.ordinal());
                buf.writeBoolean(state.external);
                buf.writeBoolean(state.playing);
                buf.writeVarInt(state.upgrades);
            },
            buf -> new Mp3State(
                    ItemStack.OPTIONAL_LIST_STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    PlayMode.values()[Math.floorMod(buf.readVarInt(), PlayMode.values().length)],
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readVarInt()));

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
