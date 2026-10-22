package com.wuliaoppx.netmusicplus.client;

import com.wuliaoppx.netmusicplus.NetMusicPlus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Resource-pack lookup for the MP3 GUI art. Every texture here is optional: when no pack provides it the screen falls
 * back to the colours of the selected theme, so the mod looks complete without any pack and a pack can replace as much
 * or as little as it likes.
 * <p>
 * Files live in {@code assets/netmusicplus/textures/gui/<name>.png}; see the example pack shipped next to the jar.
 */
public final class Mp3Textures {
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private Mp3Textures() {
    }

    /** Returns {@code textures/gui/<name>.png} when it exists, otherwise {@code null}. */
    public static ResourceLocation gui(String name) {
        return CACHE.computeIfAbsent(name, key -> {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NetMusicPlus.MOD_ID,
                    "textures/gui/" + key + ".png");
            Minecraft minecraft = Minecraft.getInstance();
            return minecraft != null && minecraft.getResourceManager().getResource(id).isPresent() ? id : null;
        });
    }

    /** Forgets the lookups so a resource reload (F3+T) or a pack change is picked up the next time a screen opens. */
    public static void clear() {
        CACHE.clear();
    }
}
