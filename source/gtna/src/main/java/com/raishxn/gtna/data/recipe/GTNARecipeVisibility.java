package com.raishxn.gtna.data.recipe;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.common.data.condition.RestrictedItemsEnabledForgeCondition;

import java.util.function.Consumer;

public final class GTNARecipeVisibility {

    private GTNARecipeVisibility() {}

    public static void saveRestricted(RecipeOutput provider, ResourceLocation id,
                                      Consumer<RecipeOutput> recipeFactory) {
        // Since 1.21 recipe conditions are attached directly to RecipeOutput.
        // The callers' vanilla builders already derive the same result-based ID.
        recipeFactory.accept(provider.withConditions(RestrictedItemsEnabledForgeCondition.INSTANCE));
    }
}
