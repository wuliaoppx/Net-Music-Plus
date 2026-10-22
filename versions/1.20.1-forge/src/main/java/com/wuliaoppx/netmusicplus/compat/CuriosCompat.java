package com.wuliaoppx.netmusicplus.compat;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Reads the MP3 player out of a Curios slot, if Curios is installed.
 * <p>
 * Everything goes through reflection on purpose. Curios is an optional runtime dependency, and the 1.20.1 build of
 * Curios references NeoForge-only classes in some of its interface signatures, so linking against the jar directly
 * would tie this mod to a mod that may simply not be there. A failed lookup is remembered and costs nothing after the
 * first call.
 */
public final class CuriosCompat {
    private static final String API_CLASS = "top.theillusivec4.curios.api.CuriosApi";
    private static final String HANDLER_CLASS = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";

    private static boolean resolved;
    private static boolean available;
    private static Method getCuriosInventory;
    private static Method findFirstCurio;
    private static Method slotStack;

    private CuriosCompat() {
    }

    /** The first MP3 player worn in any curio slot, or an empty stack when there is none (or no Curios at all). */
    public static ItemStack findEquipped(LivingEntity entity) {
        if (!resolve()) {
            return ItemStack.EMPTY;
        }
        try {
            Object lazy = getCuriosInventory.invoke(null, entity);
            if (lazy instanceof net.minecraftforge.common.util.LazyOptional<?> optional) {
                Object handler = optional.orElse(null);
                if (handler == null) {
                    return ItemStack.EMPTY;
                }
                @SuppressWarnings("unchecked")
                Predicate<ItemStack> filter = stack -> !stack.isEmpty() && stack.is(NetMusicPlus.MP3.get());
                Optional<?> result = (Optional<?>) findFirstCurio.invoke(handler, filter);
                if (result != null && result.isPresent()) {
                    Object slotResult = result.get();
                    return (ItemStack) slotStack.invoke(slotResult);
                }
            }
        } catch (Throwable throwable) {
            available = false;
            NetMusicPlus.LOGGER.warn("Cannot read the Curios inventory; the MP3 player will only be found in the inventory.",
                    throwable);
        }
        return ItemStack.EMPTY;
    }

    /** Curios syncs the equipped stack itself, so a change to it needs no extra packet. */
    public static void markChanged(LivingEntity entity) {
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            Class<?> api = Class.forName(API_CLASS);
            Class<?> handler = Class.forName(HANDLER_CLASS);
            Class<?> slotResult = Class.forName("top.theillusivec4.curios.api.SlotResult");
            getCuriosInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
            findFirstCurio = handler.getMethod("findFirstCurio", Predicate.class);
            slotStack = slotResult.getMethod("stack");
            available = true;
        } catch (Throwable absent) {
            NetMusicPlus.LOGGER.info("Curios is not installed; the MP3 player only works from the inventory.");
            available = false;
        }
        return available;
    }
}
