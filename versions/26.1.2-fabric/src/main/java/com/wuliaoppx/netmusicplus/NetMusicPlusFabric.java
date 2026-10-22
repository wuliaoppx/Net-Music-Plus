package com.wuliaoppx.netmusicplus;

import com.wuliaoppx.netmusicplus.net.Mp3ControlPayload;
import com.wuliaoppx.netmusicplus.net.Mp3OpenData;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.wuliaoppx.netmusicplus.net.Mp3StopPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Fabric entry point: registers the items, the state component and the screen handler, then wires the server side
 * listeners. Everything client only lives in {@code client.NetMusicPlusFabricClient}.
 */
public final class NetMusicPlusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        NetMusicPlusConfig.load();

        DataComponentType<Mp3State> state = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
                NetMusicPlus.id("mp3_state"), DataComponentType.<Mp3State>builder()
                        .persistent(Mp3State.CODEC)
                        .networkSynchronized(Mp3State.STREAM_CODEC)
                        .build());
        NetMusicPlus.MP3_STATE.set(state);

        ResourceKey<Item> mp3Key = ResourceKey.create(Registries.ITEM, NetMusicPlus.id("mp3_player"));
        Mp3PlayerItem mp3 = Registry.register(BuiltInRegistries.ITEM, mp3Key,
                new Mp3PlayerItem(new Item.Properties().stacksTo(1)
                        .component(state, Mp3State.EMPTY)
                        .setId(mp3Key)));
        NetMusicPlus.MP3.set(mp3);

        ResourceKey<Item> expanderKey = ResourceKey.create(Registries.ITEM, NetMusicPlus.id("mp3_expander"));
        Item expander = Registry.register(BuiltInRegistries.ITEM, expanderKey,
                new Item(new Item.Properties().stacksTo(16).setId(expanderKey)));
        NetMusicPlus.EXPANDER.set(expander);

        ExtendedMenuType<Mp3Menu, Mp3OpenData> menu = new ExtendedMenuType<>(
                (containerId, inventory, data) -> new Mp3Menu(containerId, inventory, data.playerSlot(), data.capacity()),
                Mp3OpenData.STREAM_CODEC);
        Registry.register(BuiltInRegistries.MENU, NetMusicPlus.id("mp3_menu"), menu);
        NetMusicPlus.MP3_MENU.set(menu);

        Mp3PlayPayload.register();
        Mp3StopPayload.register();
        Mp3ControlPayload.register();

        ServerPlayNetworking.registerGlobalReceiver(Mp3ControlPayload.TYPE, (payload, context) ->
                context.server().execute(() -> Mp3ServerPlayback.handleControl(context.player(), payload.action())));

        CreativeModeTabEvents.modifyOutputEvent(NetMusicPlus.NET_MUSIC_TAB).register(entries -> {
            // Appending to the tab's own list: the anchors of addAfter/addBefore are NetMusic's items, which we cannot
            // name without a hard dependency, and the vanilla search tab already lists every item by itself.
            List<ItemStack> display = entries.getDisplayStacks();
            display.add(new ItemStack(mp3));
            display.add(new ItemStack(expander));
            entries.getContext().holders()
                    .lookup(Registries.ENCHANTMENT)
                    .flatMap(registry -> registry.get(NetMusicPlus.AMPLIFIER))
                    .ifPresent(enchantment -> {
                        for (int level = 1; level <= AmplifierData.MAX_TIER; level++) {
                            display.add(AmplifierData.book(enchantment, level));
                        }
                    });
        });

        Mp3ServerPlayback.register();
    }
}
