package com.raishxn.gtna.common.data;

import brachy.modularui.api.drawable.Text;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipeSerializer;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.gui.RecipeUIModifier;
import com.gregtechceu.gtceu.api.recipe.gui.RecipeViewerCapabilityLayoutBuilder;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;

import com.raishxn.gtna.GTNACORE;

public class GTNARecipeType {

    public static final String HYDRAULIC = "hydraulic";
    public static final GTRecipeType HYDRAULIC_MANUFACTURING = register("hydraulic_manufacturing", HYDRAULIC)
            .setMaxIOSize(9, 2, 3, 1)
            .setEUIO(IO.IN)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ASSEMBLER))
            .setSound(GTSoundEntries.BATH);

    public static final String SUPERHEATER_NAME = "superheater";
    public static final GTRecipeType SUPERHEATER_RECIPES = register("super_heater", SUPERHEATER_NAME)
            .setMaxIOSize(1, 0, 0, 1)
            .setEUIO(IO.IN)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
            .setSound(GTSoundEntries.ARC);

    public static final String WOODCUTTER = "woodcutter";
    public static final GTRecipeType WOODCUTTER_RECIPES = register("woodcutter", WOODCUTTER)
            .setMaxIOSize(1, 6, 0, 0)
            .setEUIO(IO.IN)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
            .setSound(GTSoundEntries.SAW_TOOL);
    // No final do arquivo, antes de init()
    public static final String INFERNAL_COKE = "infernal_coke";
    public static final GTRecipeType INFERNAL_COKE_RECIPES = register("infernal_coke", INFERNAL_COKE)
            .setMaxIOSize(1, 1, 0, 1)
            .setEUIO(IO.IN)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
            .setSound(GTSoundEntries.FURNACE);

    public static final String HIGH_PRESSURE_REACTOR = "high_pressure_reactor";
    public static final GTRecipeType HIGH_PRESSURE_REACTOR_RECIPES = register("high_pressure_reactor",
            HIGH_PRESSURE_REACTOR)
            .setMaxIOSize(0, 0, 2, 1)
            .setEUIO(IO.IN)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
            .setSound(GTSoundEntries.ARC);

    public static final String SLAUGTHERHOUSE = "slaugterhouse";
    public static final GTRecipeType SLAUGHTERHOUSE_RECIPES = register("slaughterhouse", SLAUGTHERHOUSE)
            .setEUIO(IO.IN)
            .setMaxIOSize(1, 64, 0, 0)
            .UI(builder -> builder
                    .setProgressBar(GTGuiTextures.PROGRESS_ARROW)
                    // Runtime loot is generated after the base recipe finishes. Keep the
                    // 64-output machine capacity, but do not render 64 fake empty JEI slots.
                    .setRecipeViewerLayoutCapabilityLayoutBuilder(ItemRecipeCapability.CAP,
                            (layout, widget, io) -> {
                                if (io == IO.IN) {
                                    RecipeViewerCapabilityLayoutBuilder.ITEM
                                            .createCapabilityUILayout(layout, widget, io);
                                }
                            })
                    .addRecipeUIModifier(RecipeUIModifier.textLine(
                            Text.lang("gtna.recipe.slaughterhouse.dynamic_outputs"))))
            .setSound(GTSoundEntries.MACERATOR);

    public static final String ARTIFICIAL_STAR = "annihilate_generator";
    public static final GTRecipeType ARTIFICIAL_STAR_RECIPES = register("annihilate_generator", ARTIFICIAL_STAR)
            .setMaxIOSize(1, 1, 0, 0)
            .setEUIO(IO.OUT)
            .UI(builder -> builder.setProgressBar(GTGuiTextures.PROGRESS_ARROW))
            .setSound(GTSoundEntries.ARC);

    public static final String COSMOS_SIMULATION = "cosmos_simulation";
    public static final GTRecipeType COSMOS_SIMULATION_RECIPES = register("cosmos_simulation", COSMOS_SIMULATION)
            .setMaxIOSize(1, 120, 1, 18)
            .UI(builder -> builder
                    .setProgressBar(GTGuiTextures.PROGRESS_ARROW)
                    // The largest current recipes use 13 item and 11 fluid outputs.
                    // These viewer-only grids leave the machine's large output capacity intact.
                    .setRecipeViewerLayoutGridBuilder(ItemRecipeCapability.CAP, IO.OUT,
                            layout -> GTMuiWidgets.createGrid(13, 5, true, 's'))
                    .setRecipeViewerLayoutGridBuilder(FluidRecipeCapability.CAP, IO.OUT,
                            layout -> GTMuiWidgets.createGrid(11, 5, true, 's')))
            .setSound(GTSoundEntries.SCIENCE);

    public static GTRecipeType register(String name, String group, RecipeType<?>... proxyRecipes) {
        GTRecipeType recipeType = new GTRecipeType(GTNACORE.id(name), group, proxyRecipes);
        GTRegistries.register(BuiltInRegistries.RECIPE_TYPE, recipeType.registryName, recipeType);
        recipeType.setSerializer(GTRegistries.register(BuiltInRegistries.RECIPE_SERIALIZER,
                recipeType.registryName, new GTRecipeSerializer()));
        return recipeType;
    }

    public static void init() {}
}
