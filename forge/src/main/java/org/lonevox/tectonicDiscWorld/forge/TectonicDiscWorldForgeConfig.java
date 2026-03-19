package org.lonevox.tectonicDiscWorld.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import org.lonevox.tectonicDiscWorld.TectonicDiscWorldConfig;

public final class TectonicDiscWorldForgeConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.IntValue UNAFFECTED_RADIUS;
    private static final ForgeConfigSpec.IntValue FALLOFF_RADIUS;
    private static final ForgeConfigSpec.IntValue OFFSET_X;
    private static final ForgeConfigSpec.IntValue OFFSET_Z;
    private static final ForgeConfigSpec.BooleanValue FORCE_ENABLE_PACK;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("worldgen");

        UNAFFECTED_RADIUS = builder
                .comment("Radius in blocks where Tectonic terrain is left untouched.")
                .defineInRange("unaffectedRadius", TectonicDiscWorldConfig.unaffectedRadius, 0, 1_000_000);
        FALLOFF_RADIUS = builder
                .comment("Width in blocks of the transition band from land to outer ocean.")
                .defineInRange("falloffRadius", TectonicDiscWorldConfig.falloffRadius, 1, 1_000_000);
        OFFSET_X = builder
                .comment("Shifts the center of the circular region on the X axis.")
                .defineInRange("offsetX", TectonicDiscWorldConfig.offsetX, -30_000_000, 30_000_000);
        OFFSET_Z = builder
                .comment("Shifts the center of the circular region on the Z axis.")
                .defineInRange("offsetZ", TectonicDiscWorldConfig.offsetZ, -30_000_000, 30_000_000);
        FORCE_ENABLE_PACK = builder
                .comment("If true, registers the generated data pack as always enabled and top priority.")
                .define("forceEnablePack", TectonicDiscWorldConfig.forceEnablePack);

        builder.pop();
        SPEC = builder.build();
    }

    /** Copies Forge config values into the common config holder. */
    static void apply() {
        TectonicDiscWorldConfig.unaffectedRadius = UNAFFECTED_RADIUS.get();
        TectonicDiscWorldConfig.falloffRadius = FALLOFF_RADIUS.get();
        TectonicDiscWorldConfig.offsetX = OFFSET_X.get();
        TectonicDiscWorldConfig.offsetZ = OFFSET_Z.get();
        TectonicDiscWorldConfig.forceEnablePack = FORCE_ENABLE_PACK.get();
    }

    private TectonicDiscWorldForgeConfig() {}
}
