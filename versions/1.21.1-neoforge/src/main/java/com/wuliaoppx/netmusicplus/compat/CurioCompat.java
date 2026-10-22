package com.wuliaoppx.netmusicplus.compat;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * Optional Curios integration: the MP3 player can sit in the {@code mp3} slot and keeps playing there.
 * <p>
 * Curios is a soft dependency, so every reference to its classes must stay inside this class: it is only loaded when
 * {@link #LOADED} is true (the guard runs before any method that touches the API).
 */
public final class CurioCompat {
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CurioCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** The MP3 player worn in a curio slot, or {@link ItemStack#EMPTY}. */
    public static ItemStack findEquipped(Player player) {
        if (!LOADED) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(NetMusicPlus.MP3.get())))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    /**
     * No-op: Curios syncs its stacks on its own once the item's components change, and forcing a re-read here would
     * just allocate an item handler snapshot every time the MP3 state changes.
     */
    public static void markChanged(Player player) {
    }
}
