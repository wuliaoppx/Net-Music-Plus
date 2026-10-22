package com.wuliaoppx.netmusicplus;

import com.mojang.logging.LogUtils;
import com.wuliaoppx.netmusicplus.client.Mp3Client;
import com.wuliaoppx.netmusicplus.client.Mp3Hud;
import com.wuliaoppx.netmusicplus.net.Mp3ControlPayload;
import com.wuliaoppx.netmusicplus.net.Mp3PlayPayload;
import com.wuliaoppx.netmusicplus.net.Mp3StopPayload;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * Net Music Plus: an Amplifier enchantment for NetMusic music players, plus a portable MP3 player.
 * <p>
 * NetMusic creates {@code NetMusicSound} with a hardcoded {@code volume = 4.0F}; the sound engine turns that volume
 * into the audible radius ({@code max(volume, 1) * attenuationDistance} = 4 * 16 = 64 blocks), and NetMusic broadcasts
 * the play packet with a hardcoded radius of 96 blocks ({@code NetworkHandler#sendToNearby}). Both numbers are scaled
 * by the amplifier enchantment's multiplier, which is configurable (defaults: 200% / 300% / 400%).
 * <p>
 * The enchantment itself is plain data ({@code data/netmusicplus/enchantment/amplifier.json}). The three books are
 * added to NetMusic's own creative tab instead of {@code CreativeModeTabs.INGREDIENTS}: vanilla already generates an
 * enchanted book for every registered enchantment there, so adding them again trips
 * {@code IllegalArgumentException("... already exists in this tab!")}.
 */
@Mod(NetMusicPlus.MOD_ID)
public class NetMusicPlus {
    public static final String MOD_ID = "netmusicplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceKey<Enchantment> AMPLIFIER =
            ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(MOD_ID, "amplifier"));

    /** NetMusic registers its tab as {@code netmusic:netmusic} ({@code InitItems.TABS}). */
    private static final ResourceKey<CreativeModeTab> NET_MUSIC_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("netmusic", "netmusic"));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final DeferredItem<Mp3PlayerItem> MP3 =
            ITEMS.registerItem("mp3_player", Mp3PlayerItem::new, new Item.Properties().stacksTo(1));

    /** Storage expansion: dropped into the MP3 player's expansion slot it adds ten disc slots, up to three times. */
    public static final DeferredItem<Item> EXPANDER =
            ITEMS.registerSimpleItem("mp3_expander", new Item.Properties().stacksTo(16));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Mp3State>> MP3_STATE =
            COMPONENTS.registerComponentType("mp3_state",
                    builder -> builder.persistent(Mp3State.CODEC).networkSynchronized(Mp3State.STREAM_CODEC));

    public static final DeferredHolder<MenuType<?>, MenuType<Mp3Menu>> MP3_MENU =
            MENUS.register("mp3", () -> IMenuTypeExtension.create(
                    (containerId, inventory, buffer) -> new Mp3Menu(containerId, inventory,
                            buffer.readVarInt(), buffer.readVarInt())));

    public NetMusicPlus(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        COMPONENTS.register(modBus);
        MENUS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, NetMusicPlusConfig.SPEC);

        modBus.addListener(NetMusicPlus::registerPayloads);
        modBus.addListener(NetMusicPlus::addCreativeItems);

        NeoForge.EVENT_BUS.addListener(Mp3ServerPlayback::onServerTick);
        NeoForge.EVENT_BUS.addListener(Mp3ServerPlayback::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(Mp3ServerPlayback::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(AmplifierTooltip::onTooltip);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(Mp3Client::registerKeys);
            modBus.addListener(Mp3Client::registerScreens);
            modBus.addListener(Mp3Hud::register);
            NeoForge.EVENT_BUS.addListener(Mp3Client::onClientTick);
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(Mp3PlayPayload.TYPE, Mp3PlayPayload.STREAM_CODEC, Mp3PlayPayload::handle);
        registrar.playToClient(Mp3StopPayload.TYPE, Mp3StopPayload.STREAM_CODEC, Mp3StopPayload::handle);
        registrar.playToServer(Mp3ControlPayload.TYPE, Mp3ControlPayload.STREAM_CODEC, Mp3ControlPayload::handle);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(NET_MUSIC_TAB)) {
            return;
        }
        ItemStack mp3 = new ItemStack(MP3.get());
        if (!event.getParentEntries().contains(mp3)) {
            event.accept(mp3, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        ItemStack expander = new ItemStack(EXPANDER.get());
        if (!event.getParentEntries().contains(expander)) {
            event.accept(expander, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
        event.getParameters().holders()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(AMPLIFIER))
                .ifPresent(enchantment -> {
                    for (int level = 1; level <= AmplifierData.MAX_TIER; level++) {
                        ItemStack book = AmplifierData.book(enchantment, level);
                        // PARENT_TAB_ONLY is mandatory: the search tab already lists every enchantment book
                        // (CreativeModeTabs.generateEnchantmentBookTypesAllLevels), and the entry set rejects
                        // duplicates with an IllegalArgumentException that crashes the inventory screen.
                        if (!event.getParentEntries().contains(book)) {
                            event.accept(book, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        }
                    }
                });
    }
}
