package com.alexlego19.ainpcs.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AiNpcsClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Boolean> showTitleScreenMenu;

    static {
        BUILDER.push("UI Settings");
        showTitleScreenMenu = BUILDER
                .comment("Whether to show the NPC Manager button on the Title Screen")
                .define("showTitleScreenMenu", true);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}
