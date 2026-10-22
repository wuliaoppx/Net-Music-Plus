package com.wuliaoppx.netmusicplus;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Play modes shown by the MP3 player GUI. */
public enum PlayMode implements StringRepresentable {
    LOOP("loop"),
    SINGLE("single"),
    SHUFFLE("shuffle");

    private final String name;

    PlayMode(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public PlayMode cycle() {
        return values()[(this.ordinal() + 1) % values().length];
    }

    /** The mode stored under this name, falling back to {@link #LOOP} for unknown or missing values. */
    public static PlayMode byName(String name) {
        for (PlayMode mode : values()) {
            if (mode.name.equals(name)) {
                return mode;
            }
        }
        return LOOP;
    }

    public Component displayName() {
        return Component.translatable("mp3.netmusicplus.mode." + this.name);
    }
}
