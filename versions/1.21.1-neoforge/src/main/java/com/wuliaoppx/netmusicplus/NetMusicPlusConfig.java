package com.wuliaoppx.netmusicplus;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config: the amplifier multipliers and the MP3 player's out-loud range.
 * <p>
 * Every multiplier is relative to NetMusic's built-in range of 64 blocks, which comes from its hardcoded
 * {@code volume = 4.0F} times the vanilla attenuation distance of 16 (see {@link AmplifierData}).
 */
public final class NetMusicPlusConfig {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.DoubleValue LEVEL_1;
    private static final ModConfigSpec.DoubleValue LEVEL_2;
    private static final ModConfigSpec.DoubleValue LEVEL_3;
    private static final ModConfigSpec.DoubleValue MP3_OUT_LOUD_RADIUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Amplifier enchantment levels. 1.0 keeps NetMusic's default 64 block range.")
                .push("amplifier");
        LEVEL_1 = builder.comment("Range multiplier for Amplifier I (default 200%, i.e. 128 blocks).")
                .defineInRange("level_1_multiplier", 2.0D, 1.0D, 64.0D);
        LEVEL_2 = builder.comment("Range multiplier for Amplifier II (default 300%, i.e. 192 blocks).")
                .defineInRange("level_2_multiplier", 3.0D, 1.0D, 64.0D);
        LEVEL_3 = builder.comment("Range multiplier for Amplifier III (default 400%, i.e. 256 blocks).")
                .defineInRange("level_3_multiplier", 4.0D, 1.0D, 64.0D);
        builder.pop();

        builder.comment("MP3 player.").push("mp3");
        MP3_OUT_LOUD_RADIUS = builder
                .comment("Range in blocks when the MP3 player plays out loud (before the amplifier multiplier).",
                        "NetMusic's music player and the vanilla jukebox use 64.")
                .defineInRange("out_loud_radius", 64.0D, 1.0D, 1024.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private NetMusicPlusConfig() {
    }

    public static float multiplier(int tier) {
        if (!SPEC.isLoaded()) {
            // The sound engine can ask for a radius before the configs are read; fall back to the defaults.
            return switch (tier) {
                case 1 -> 2.0F;
                case 2 -> 3.0F;
                case 3 -> 4.0F;
                default -> 1.0F;
            };
        }
        double value = switch (tier) {
            case 1 -> LEVEL_1.get();
            case 2 -> LEVEL_2.get();
            case 3 -> LEVEL_3.get();
            default -> 1.0D;
        };
        return (float) value;
    }

    public static float outLoudRadius() {
        return SPEC.isLoaded() ? MP3_OUT_LOUD_RADIUS.get().floatValue() : 64.0F;
    }
}
