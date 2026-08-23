package com.raishxn.gtna.common.data.condition;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.mojang.serialization.MapCodec;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.config.ConfigHolder;

public class RestrictedItemsEnabledForgeCondition implements ICondition {

    private static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, GTNACORE.MOD_ID);
    public static final RestrictedItemsEnabledForgeCondition INSTANCE = new RestrictedItemsEnabledForgeCondition();
    public static final MapCodec<RestrictedItemsEnabledForgeCondition> CODEC = MapCodec.unit(INSTANCE).stable();

    static {
        CONDITION_CODECS.register("restricted_items_enabled", () -> CODEC);
    }

    private RestrictedItemsEnabledForgeCondition() {}

    @Override
    public boolean test(IContext context) {
        return ConfigHolder.areRestrictedRecipesEnabled();
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }

    public static void register(IEventBus modBus) {
        CONDITION_CODECS.register(modBus);
    }
}
