package com.wuliaoppx.netmusicplus.compat;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Optional Curios integration: the MP3 player can sit in the {@code mp3} curio slot over the chestplate and keeps
 * playing there.
 * <p>
 * Curios is a soft dependency, so every reference to its classes stays inside this class: it is only touched when
 * {@link #LOADED} is true (the guard runs before any method that reaches the API). On Forge, classes from an absent
 * mod would otherwise be resolved when this class is loaded, which happens during the server tick.
 * <p>
 * The slot is data driven and lives in Curios' own namespace: {@code data/curios/curios/slots/mp3.json} defines it,
 * {@code data/curios/curios/entities/player.json} grants it to players and
 * {@code data/curios/tags/items/mp3.json} lists the items that fit.
 */
public final class CurioCompat {
    private static final boolean LOADED = ModList.get().isLoaded("curios");

    private CurioCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** The MP3 player worn in the curio slot, or {@link ItemStack#EMPTY}. */
    public static ItemStack findEquipped(Player player) {
        if (!LOADED) {
            return ItemStack.EMPTY;
        }
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.is(NetMusicPlus.MP3.get())))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    /**
     * No-op: Curios syncs its stacks on its own once the item's components change, and forcing a re-read here would
     * just allocate an item handler snapshot every time the MP3 state changes.
     */
    public static void markChanged(Player player) {
    }
}
