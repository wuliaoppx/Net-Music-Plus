package com.wuliaoppx.netmusicplus;

import com.mojang.logging.LogUtils;
import com.wuliaoppx.netmusicplus.client.Mp3Client;
import com.wuliaoppx.netmusicplus.net.Net;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Net Music Plus: an Amplifier enchantment for NetMusic music players, plus a portable MP3 player.
 * <p>
 * NetMusic creates {@code NetMusicSound} with a hardcoded {@code volume = 4.0F}; the sound engine turns that volume
 * into the audible radius ({@code max(volume, 1) * attenuationDistance} = 4 * 16 = 64 blocks), and NetMusic broadcasts
 * the play packet with a hardcoded radius of 96 blocks ({@code NetworkHandler#sendToNearby}). Both numbers are scaled
 * by the amplifier enchantment's multiplier, which is configurable (defaults: 200% / 300% / 400%).
 * <p>
 * On 1.20.1 the enchantment is a real {@link Enchantment} in the enchantment registry (the data driven enchantment
 * registry only exists from 1.21 on), and the MP3 player keeps its state in the item stack's NBT tag instead of a data
 * component.
 */
@Mod(NetMusicPlus.MOD_ID)
public final class NetMusicPlus {
    public static final String MOD_ID = "netmusicplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation AMPLIFIER_ID = new ResourceLocation(MOD_ID, "amplifier");

    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    /** Registered on load, not data driven: 1.20.1 has no enchantment codec registry yet. */
    public static final RegistryObject<Enchantment> AMPLIFIER =
            ENCHANTMENTS.register("amplifier", AmplifierEnchantment::new);

    public static final RegistryObject<Mp3PlayerItem> MP3 = ITEMS.register("mp3_player",
            () -> new Mp3PlayerItem(new Item.Properties().stacksTo(1)));
    /** Storage expansion: dropped into the MP3 player's expansion slot it adds ten disc slots, up to three times. */
    public static final RegistryObject<Item> EXPANDER = ITEMS.register("mp3_expander",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<MenuType<Mp3Menu>> MP3_MENU = MENUS.register("mp3_menu",
            () -> IForgeMenuType.create((containerId, inventory, data) -> new Mp3Menu(containerId, inventory, data)));

    /** 1.20.1 recipes cannot put NBT on a JSON result, so the amplifier books use their own serializer. */
    public static final RegistryObject<RecipeSerializer<ShapelessRecipe>> AMPLIFIER_BOOK =
            RECIPE_SERIALIZERS.register("amplifier_book", () -> AmplifierBookRecipe.INSTANCE);

    /**
     * NetMusic registers its tab as {@code netmusic:netmusic}: {@code InitItems} calls
     * {@code TABS.register("netmusic", ...)} on a {@code netmusic} namespace, and the title key is
     * {@code itemGroup.netmusic}.
     */
    public static final ResourceKey<CreativeModeTab> NET_MUSIC_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("netmusic", "netmusic"));

    public NetMusicPlus() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENCHANTMENTS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);

        NetMusicPlusConfig.register();
        Net.register();
        modBus.addListener(NetMusicPlus::addCreativeItems);

        MinecraftForge.EVENT_BUS.register(Mp3ServerPlayback.Events.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            modBus.addListener(Mp3Client::registerKeys);
            modBus.addListener(NetMusicPlus::onClientSetup);
            Mp3Client.registerEvents();
            AmplifierTooltip.register();
        });
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(Mp3Client::registerScreens);
    }

    /**
     * Adds the mod's items and amplifier books to NetMusic's own tab.
     * <p>
     * Only the parent tab is touched ({@code PARENT_TAB_ONLY}): vanilla pre-fills the search tab with an enchanted
     * book for every registered enchantment, and adding the same stack twice trips the tab's duplicate check.
     */
    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(NET_MUSIC_TAB)) {
            return;
        }
        addIfAbsent(event, new ItemStack(MP3.get()));
        addIfAbsent(event, new ItemStack(EXPANDER.get()));
        for (int level = 1; level <= AmplifierData.MAX_TIER; level++) {
            addIfAbsent(event, AmplifierData.book(AMPLIFIER.get(), level));
        }
    }

    private static void addIfAbsent(BuildCreativeModeTabContentsEvent event, ItemStack stack) {
        for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> existing : event.getEntries()) {
            if (ItemStack.isSameItemSameTags(existing.getKey(), stack)) {
                return;
            }
        }
        event.accept(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /** The screen handler type, kept here so both sides agree on the opening data. */
    public static MenuType<Mp3Menu> menuType() {
        return MP3_MENU.get();
    }
}
