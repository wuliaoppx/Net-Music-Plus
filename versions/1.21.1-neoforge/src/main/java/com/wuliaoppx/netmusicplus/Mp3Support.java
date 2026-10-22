package com.wuliaoppx.netmusicplus;

import com.github.tartaricacid.netmusic.init.InitItems;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.wuliaoppx.netmusicplus.compat.CurioCompat;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Helpers shared by the item, the menu, the server playback and the client playback. */
public final class Mp3Support {
    private Mp3Support() {
    }

    public static boolean isDisc(ItemStack stack) {
        return !stack.isEmpty() && stack.is(InitItems.MUSIC_CD.get());
    }

    public static Mp3State state(ItemStack stack) {
        return stack.getOrDefault(NetMusicPlus.MP3_STATE.get(), Mp3State.EMPTY);
    }

    public static void setState(ItemStack stack, Mp3State state) {
        stack.set(NetMusicPlus.MP3_STATE.get(), state);
    }

    /**
     * The MP3 player that should be playing: the one worn in a curio slot first, then the first one in the inventory.
     * Only one MP3 plays at a time, no matter how many the player carries.
     */
    public static ItemStack findActive(Player player) {
        ItemStack equipped = CurioCompat.findEquipped(player);
        if (!equipped.isEmpty()) {
            return equipped;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(NetMusicPlus.MP3.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemMusicCD.SongInfo songInfo(ItemStack disc) {
        return disc.isEmpty() ? null : ItemMusicCD.getSongInfo(disc);
    }

    public static String trackName(ItemStack disc) {
        ItemMusicCD.SongInfo info = songInfo(disc);
        if (info == null) {
            return "";
        }
        return info.transName != null && !info.transName.isBlank() ? info.transName : info.songName;
    }

    public static String trackUrl(ItemStack disc) {
        ItemMusicCD.SongInfo info = songInfo(disc);
        return info == null || info.songUrl == null ? "" : info.songUrl;
    }

    /** Length of the disc in ticks, or {@code 0} when it carries no song information. */
    public static int durationTicks(ItemStack disc) {
        ItemMusicCD.SongInfo info = songInfo(disc);
        return info == null ? 0 : Math.max(1, info.songTime) * 20;
    }

    /** Tells the server to re-send the changed stack to its owner and to nearby clients. */
    public static void markChanged(Player player) {
        player.getInventory().setChanged();
        CurioCompat.markChanged(player);
    }
}
