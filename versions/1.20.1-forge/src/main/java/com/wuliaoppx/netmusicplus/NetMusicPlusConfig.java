package com.wuliaoppx.netmusicplus;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Common config: the amplifier multipliers and the MP3 player's out-loud range.
 * <p>
 * Every multiplier is relative to NetMusic's built-in range of 64 blocks, which comes from its hardcoded
 * {@code volume = 4.0F} times the vanilla attenuation distance of 16 (see {@link AmplifierData}).
 * <p>
 * Both sides read these values (the client for the audible radius, the server for the broadcast radius), hence a
 * COMMON config rather than a server one.
 */
public final class NetMusicPlusConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.DoubleValue LEVEL_1;
    private static final ForgeConfigSpec.DoubleValue LEVEL_2;
    private static final ForgeConfigSpec.DoubleValue LEVEL_3;
    private static final ForgeConfigSpec.DoubleValue OUT_LOUD_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("Amplifier multipliers, relative to NetMusic's 64 block range.").push("amplifier");
        LEVEL_1 = builder.comment("Level I: 200% of the vanilla radius (128 blocks).")
                .defineInRange("level_1_multiplier", 2.0D, 1.0D, 64.0D);
        LEVEL_2 = builder.comment("Level II: 300% (192 blocks).")
                .defineInRange("level_2_multiplier", 3.0D, 1.0D, 64.0D);
        LEVEL_3 = builder.comment("Level III: 400% (256 blocks).")
                .defineInRange("level_3_multiplier", 4.0D, 1.0D, 64.0D);
        builder.pop();
        builder.comment("Base radius of the MP3 player's out-loud mode, before the amplifier is applied.").push("mp3");
        OUT_LOUD_RADIUS = builder.comment("Radius in blocks; 64 matches the music player's own range.")
                .defineInRange("out_loud_radius", 64.0D, 1.0D, 1024.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private NetMusicPlusConfig() {
    }

    /** Registered from the mod constructor (COMMON) so both sides can read the values. */
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static float multiplier(int tier) {
        double value = switch (tier) {
            case 1 -> get(LEVEL_1, 2.0D);
            case 2 -> get(LEVEL_2, 3.0D);
            case 3 -> get(LEVEL_3, 4.0D);
            default -> 1.0D;
        };
        return (float) value;
    }

    public static float outLoudRadius() {
        return (float) get(OUT_LOUD_RADIUS, 64.0D);
    }

    /** Falls back to the default until the config is loaded, which can be after the first sound request. */
    private static double get(ForgeConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException notLoadedYet) {
            return fallback;
        }
    }
}
