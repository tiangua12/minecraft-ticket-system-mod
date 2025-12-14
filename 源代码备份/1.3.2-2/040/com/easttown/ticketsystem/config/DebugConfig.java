package com.easttown.ticketsystem.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DebugConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue SHOW_DEBUG_LOGS;

    static {
        BUILDER.push("Debug Settings");

        SHOW_DEBUG_LOGS = BUILDER
            .comment("Whether to show debug logs in the console (default: true)")
            .define("showDebugLogs", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}