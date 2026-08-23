package yuuki1293.pccard;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ConfigCommon {
    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.IntValue SEARCH_DEPTH;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Programmed Circuit Card common configuration").push("network");
        SEARCH_DEPTH = builder
            .comment("Maximum depth used while following nested AE2 subnet storage buses.",
                "Higher values support deeper subnet trees but perform more grid lookups per craft.")
            .defineInRange("search_depth", 5, 0, 100);
        builder.pop();
        SPEC = builder.build();
    }

    private ConfigCommon() {}

    public static int getSearchDepth() {
        return SEARCH_DEPTH.get();
    }
}
