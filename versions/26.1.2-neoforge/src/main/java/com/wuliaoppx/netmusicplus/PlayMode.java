package com.wuliaoppx.netmusicplus;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/** Play modes shown by the MP3 player GUI. */
public enum PlayMode implements StringRepresentable {
    LOOP("loop"),
    SINGLE("single"),
    SHUFFLE("shuffle");

    public static final Codec<PlayMode> CODEC = StringRepresentable.fromEnum(PlayMode::values);

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

    public Component displayName() {
        return Component.translatable("mp3.netmusicplus.mode." + this.name);
    }
}
