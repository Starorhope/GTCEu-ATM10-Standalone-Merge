package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.recipe.condition.RecipeConditionType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.condition.CompactCondition;
import com.raishxn.gtna.common.data.condition.RestrictedItemsEnabledCondition;

public class GTNARecipeConditions {

    public static final RecipeConditionType<CompactCondition> COMPACT = GTRegistries.register(
            GTRegistries.RECIPE_CONDITIONS, GTNACORE.id("compact"),
            new RecipeConditionType<>(() -> CompactCondition.INSTANCE, CompactCondition.CODEC));

    public static final RecipeConditionType<RestrictedItemsEnabledCondition> RESTRICTED_ITEMS_ENABLED = GTRegistries
            .register(GTRegistries.RECIPE_CONDITIONS, GTNACORE.id("restricted_items_enabled"),
                    new RecipeConditionType<>(() -> RestrictedItemsEnabledCondition.INSTANCE,
                            RestrictedItemsEnabledCondition.CODEC));

    public static void init() {}
}
