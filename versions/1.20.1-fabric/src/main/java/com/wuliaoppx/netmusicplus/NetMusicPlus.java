package com.wuliaoppx.netmusicplus;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Net Music Plus: an Amplifier enchantment for NetMusic music players, plus a portable MP3 player.
 * <p>
 * NetMusic creates {@code NetMusicSound} with a hardcoded {@code volume = 4.0F}; the sound engine turns that volume
 * into the audible radius ({@code max(volume, 1) * attenuationDistance} = 4 * 16 = 64 blocks), and NetMusic broadcasts
 * the play packet with a hardcoded radius of 96 blocks ({@code NetworkHandler#sendToNearBy}). Both numbers are scaled
 * by the amplifier enchantment's multiplier, which is configurable (defaults: 200% / 300% / 400%).
 * <p>
 * On 1.20.1 the enchantment is a real {@link Enchantment} registered into {@code BuiltInRegistries.ENCHANTMENT} (the
 * data driven enchantment registry only exists from 1.21 on), and the MP3 player keeps its state in the item stack's
 * NBT tag instead of a data component.
 * <p>
 * Fabric flavour: registrations happen eagerly in {@link NetMusicPlusFabric} and the values are parked in the
 * {@link Reg} holders below, so the shared code can keep calling {@code NetMusicPlus.MP3.get()} like it did on NeoForge.
 */
public final class NetMusicPlus {
    public static final String MOD_ID = "netmusicplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation AMPLIFIER_ID = new ResourceLocation(MOD_ID, "amplifier");

    /** Filled by {@link NetMusicPlusFabric}; the enchantment is registered, not data driven, on 1.20.1. */
    public static final Reg<Enchantment> AMPLIFIER = new Reg<>();

    /** NetMusic registers its tab as {@code netmusic:netmusic_group} (see {@code InitItems} bytecode: id + "netmusic_group"). */
    public static final ResourceKey<CreativeModeTab> NET_MUSIC_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    new ResourceLocation("netmusic", "netmusic_group"));

    public static final Reg<Mp3PlayerItem> MP3 = new Reg<>();
    /** Storage expansion: dropped into the MP3 player's expansion slot it adds ten disc slots, up to three times. */
    public static final Reg<Item> EXPANDER = new Reg<>();
    public static final Reg<ExtendedScreenHandlerType<Mp3Menu>> MP3_MENU = new Reg<>();

    private NetMusicPlus() {
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /** The screen handler type, kept here so both sides agree on the opening data. */
    public static MenuType<Mp3Menu> menuType() {
        return MP3_MENU.get();
    }

    /**
     * A lazily filled registration slot: {@link #get()} is only called after {@link NetMusicPlusFabric} has registered
     * the object, which happens during mod initialisation before anything can ask for it.
     */
    public static final class Reg<T> implements Supplier<T> {
        private T value;

        @Override
        public T get() {
            if (this.value == null) {
                throw new IllegalStateException("netmusicplus: registration used before it was created");
            }
            return this.value;
        }

        void set(T value) {
            this.value = value;
        }
    }

    /** Registers an object under the mod namespace. */
    static <T> T register(Registry<T> registry, String path, T value) {
        return Registry.register(registry, id(path), value);
    }
}
