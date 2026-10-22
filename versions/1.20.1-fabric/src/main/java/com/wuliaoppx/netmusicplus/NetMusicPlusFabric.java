package com.wuliaoppx.netmusicplus;

import com.wuliaoppx.netmusicplus.net.Mp3ControlPayload;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.wuliaoppx.netmusicplus.net.Mp3StopPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * Fabric entry point: registers the enchantment, the items and the screen handler, then wires the server side
 * listeners. Everything client only lives in {@code client.NetMusicPlusFabricClient}.
 */
public final class NetMusicPlusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetMusicPlusConfig.load();

        // 1.20.1 enchantments are registered objects, not data files. Registering before the items means the books
        // added to the creative tab below can be built straight away.
        Enchantment amplifier = Registry.register(BuiltInRegistries.ENCHANTMENT, NetMusicPlus.id("amplifier"),
                new AmplifierEnchantment());
        NetMusicPlus.AMPLIFIER.set(amplifier);

        Mp3PlayerItem mp3 = Registry.register(BuiltInRegistries.ITEM, NetMusicPlus.id("mp3_player"),
                new Mp3PlayerItem(new net.fabricmc.fabric.api.item.v1.FabricItemSettings().stacksTo(1)));
        NetMusicPlus.MP3.set(mp3);

        Item expander = Registry.register(BuiltInRegistries.ITEM, NetMusicPlus.id("mp3_expander"),
                new Item(new net.fabricmc.fabric.api.item.v1.FabricItemSettings().stacksTo(16)));
        NetMusicPlus.EXPANDER.set(expander);

        // 1.20.1 recipes cannot put NBT on a JSON result, so the amplifier books use a serializer that builds the
        // enchanted book itself. Clients parse the synced form, which is a plain vanilla shapeless recipe.
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, NetMusicPlus.id("amplifier_book"),
                AmplifierBookRecipe.INSTANCE);

        // 1.20.1's ExtendedScreenHandlerType carries the opening data as a raw FriendlyByteBuf (no StreamCodec yet).
        ExtendedScreenHandlerType<Mp3Menu> menu = new ExtendedScreenHandlerType<>(
                (containerId, inventory, buf) -> new Mp3Menu(containerId, inventory, buf));
        Registry.register(BuiltInRegistries.MENU, NetMusicPlus.id("mp3_menu"), menu);
        NetMusicPlus.MP3_MENU.set(menu);

        ServerPlayNetworking.registerGlobalReceiver(Mp3ControlPayload.ID, (server, player, handler, buf, sender) -> {
            // Read the payload on the network thread; the buffer is only valid for the duration of this call.
            int action = buf.readVarInt();
            server.execute(() -> Mp3ServerPlayback.handleControl(player, action));
        });

        ItemGroupEvents.modifyEntriesEvent(NetMusicPlus.NET_MUSIC_TAB).register(entries -> {
            // Appending to the tab's own list: the anchors of addAfter/addBefore are NetMusic's items, which we cannot
            // name without a hard dependency, and the vanilla search tab already lists every item by itself.
            List<ItemStack> display = entries.getDisplayStacks();
            addOnce(display, new ItemStack(mp3));
            addOnce(display, new ItemStack(expander));
            for (int level = 1; level <= AmplifierData.MAX_TIER; level++) {
                addOnce(display, AmplifierData.book(amplifier, level));
            }
        });

        Mp3ServerPlayback.register();
    }

    /** Keeps repeated tab rebuilds from stacking duplicates (the tab list is rebuilt whenever entries are re-evaluated). */
    private static void addOnce(List<ItemStack> stacks, ItemStack stack) {
        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                return;
            }
        }
        stacks.add(stack);
    }
}
