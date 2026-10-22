package com.wuliaoppx.netmusicplus.compat;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Optional Trinkets integration: the MP3 player can sit in the {@code mp3} trinket slot and keeps playing there.
 * <p>
 * Trinkets is a soft dependency, so every reference to its classes must stay inside this class: it is only loaded when
 * {@link #LOADED} is true (the guard runs before any method that touches the API).
 * <p>
 * The slot itself is data driven and lives in Trinkets' own namespace: {@code data/trinkets/slots/mp3/mp3.json}
 * defines the {@code mp3/mp3} slot, {@code data/trinkets/slots/mp3/group.json} pins its group over the chestplate
 * screen handler slot ({@code slot_id 6}), {@code data/trinkets/tags/item/mp3/mp3.json} lists the items that fit in it
 * and {@code data/trinkets/entities/mp3.json} grants that slot to players.
 */
public final class TrinketsCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("trinkets");

    private TrinketsCompat() {
    }

    public static boolean isLoaded() {
        return LOADED;
    }

    /** The MP3 player worn in a trinket slot, or {@link ItemStack#EMPTY}. */
    public static ItemStack findEquipped(Player player) {
        if (!LOADED) {
            return ItemStack.EMPTY;
        }
        return TrinketsApi.getTrinketComponent(player)
                .map(component -> findMp3(component))
                .orElse(ItemStack.EMPTY);
    }

    private static ItemStack findMp3(TrinketComponent component) {
        return component.getEquipped(stack -> stack.is(NetMusicPlus.MP3.get())).stream()
                .findFirst()
                .map(pair -> pair.getB())
                .orElse(ItemStack.EMPTY);
    }

    /**
     * No-op: Trinkets syncs its stacks on its own once the item's components change, and forcing a re-read here would
     * just allocate an item handler snapshot every time the MP3 state changes.
     */
    public static void markChanged(Player player) {
    }
}
