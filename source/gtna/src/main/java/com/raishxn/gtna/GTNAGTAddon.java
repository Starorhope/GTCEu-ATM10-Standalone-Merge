package com.raishxn.gtna;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.api.registry.GTNARegistry;
import com.raishxn.gtna.data.recipe.*;

@GTAddon(GTNACORE.MOD_ID)
public class GTNAGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GTNARegistry.REGISTRATE;
    }

    @Override
    public boolean requiresHighTier() {
        return true;
    }

    @Override
    public void gtInitComplete() {}

    @Override
    public void addRecipes(RecipeOutput provider) {
        GTNAHighTierRecipes.register(provider);
        GTNAMaterialRecipes.register(provider);
        GTNAItemRecipes.register(provider);
        GTNAMachineRecipes.register(provider);
        GTNAHatchesRecipes.register(provider);
        GTNABlockRecipes.register(provider);
        GTNAGeneratesRecipes.register(provider);
        GTNAWoodCutterRecipes.register(provider);
        GTNAInfernalCokeRecipes.register(provider);
        GTNAHighPressureRecipes.register(provider);
        VoidminerRecipes.register(provider);
    }

    @Override
    public void removeRecipes(java.util.function.Consumer<ResourceLocation> consumer) {
        consumer.accept(GTNAGeneratesRecipes.CORE_ECHOITE_BLOCK_COMPRESSION_RECIPE_ID);
    }
}
