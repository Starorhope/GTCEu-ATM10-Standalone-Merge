package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GCYMMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;

import appeng.core.definitions.AEBlocks;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.data.GTNAMachines2;

import java.util.function.Consumer;

import static com.gregtechceu.gtceu.api.GTValues.*;

public class GTNAHatchesRecipes {

    public static void register(RecipeOutput provider) {
        // --- Thread Hatch ZPM ---
        // Segue o padrão explícito do GTNAMachineRecipes
        if (GTNAMachines2.THREAD_HATCHES[ZPM] != null) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.THREAD_HATCHES[ZPM].asStack().getItem())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("EFE")
                    .define('A', CustomTags.ZPM_CIRCUITS)
                    .define('B', GTItems.ROBOT_ARM_ZPM.asStack().getItem())
                    .define('C', GTItems.CONVEYOR_MODULE_ZPM.asStack().getItem())
                    .define('D', GTItems.FIELD_GENERATOR_ZPM.asStack().getItem())
                    .define('E',
                            ChemicalHelper.get(TagPrefix.wireGtQuadruple, GTMaterials.UraniumRhodiumDinaquadide)
                                    .getItem())
                    .define('F', GTMachines.HULL[ZPM].asStack().getItem())
                    .unlockedBy("has_hull_zpm",
                            InventoryChangeTrigger.TriggerInstance.hasItems(GTMachines.HULL[ZPM].asStack().getItem()))
                    .save(provider);
        }

        // --- Thread Hatch UV ---
        if (GTNAMachines2.THREAD_HATCHES[UV] != null) {
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.THREAD_HATCHES[UV].asStack().getItem())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("EFE")
                    .define('A', CustomTags.UV_CIRCUITS)
                    .define('B', GTItems.ROBOT_ARM_UV.asStack().getItem())
                    .define('C', GTItems.CONVEYOR_MODULE_UV.asStack().getItem())
                    .define('D', GTItems.FIELD_GENERATOR_UV.asStack().getItem())
                    .define('E',
                            ChemicalHelper.get(TagPrefix.wireGtQuadruple,
                                    GTMaterials.EnrichedNaquadahTriniumEuropiumDuranide).getItem())
                    .define('F', GTMachines.HULL[UV].asStack().getItem())
                    .unlockedBy("has_hull_uv",
                            InventoryChangeTrigger.TriggerInstance.hasItems(GTMachines.HULL[UV].asStack().getItem()))
                    .save(provider);
        }
        createAccelerateRecipe(provider, LV, GTItems.SENSOR_LV, GTItems.FIELD_GENERATOR_LV);
        createAccelerateRecipe(provider, MV, GTItems.SENSOR_MV, GTItems.FIELD_GENERATOR_MV);
        createAccelerateRecipe(provider, HV, GTItems.SENSOR_HV, GTItems.FIELD_GENERATOR_HV);
        createAccelerateRecipe(provider, EV, GTItems.SENSOR_EV, GTItems.FIELD_GENERATOR_EV);
        createAccelerateRecipe(provider, IV, GTItems.SENSOR_IV, GTItems.FIELD_GENERATOR_IV);
        createAccelerateRecipe(provider, LuV, GTItems.SENSOR_LuV, GTItems.FIELD_GENERATOR_LuV);
        createAccelerateRecipe(provider, ZPM, GTItems.SENSOR_ZPM, GTItems.FIELD_GENERATOR_ZPM);
        createAccelerateRecipe(provider, UV, GTItems.SENSOR_UV, GTItems.FIELD_GENERATOR_UV);
        createOutputBoostRecipe(provider, LV, GTItems.EMITTER_LV, GTItems.SENSOR_LV);
        createOutputBoostRecipe(provider, MV, GTItems.EMITTER_MV, GTItems.SENSOR_MV);
        createOutputBoostRecipe(provider, HV, GTItems.EMITTER_HV, GTItems.SENSOR_HV);
        createOutputBoostRecipe(provider, EV, GTItems.EMITTER_EV, GTItems.SENSOR_EV);
        createOutputBoostRecipe(provider, IV, GTItems.EMITTER_IV, GTItems.SENSOR_IV);
        createOutputBoostRecipe(provider, LuV, GTItems.EMITTER_LuV, GTItems.SENSOR_LuV);
        createOutputBoostRecipe(provider, ZPM, GTItems.EMITTER_ZPM, GTItems.SENSOR_ZPM);
        createOutputBoostRecipe(provider, UV, GTItems.EMITTER_UV, GTItems.SENSOR_UV);
        for (int tier = LV; tier <= OpV; tier++) {
            createInfiniteInputBusRecipe(provider, tier, getEmitter(tier), getSensor(tier));
            createInfiniteInputHatchRecipe(provider, tier, getEmitter(tier), getFieldGenerator(tier));
            createOutputBoostItemBusRecipe(provider, tier, getEmitter(tier), getSensor(tier));
            createOutputBoostFluidHatchRecipe(provider, tier, getEmitter(tier), getFieldGenerator(tier));
        }

        createOverclockRecipe(provider, UV, GTItems.FIELD_GENERATOR_UV, GTItems.VOLTAGE_COIL_UV);
        registerHighTierUtilityHatches(provider);
        createCraftingCPUInterfaceRecipe(provider);
        createMEStorageAccessRecipes(provider);
    }

    /**
     * Continues the utility-hatch progression past UV without relying on a
     * non-existent MAX-tier assembler. Every recipe runs at the preceding
     * voltage and researches the immediately preceding hatch.
     */
    private static void registerHighTierUtilityHatches(RecipeOutput provider) {
        for (int tier = UHV; tier <= MAX; tier++) {
            createHighTierAccelerateRecipe(provider, tier);
            createHighTierOverclockRecipe(provider, tier);
        }
        for (int tier = UHV; tier <= OpV; tier++) {
            createAdvancedParallelRecipe(provider, tier);
        }
    }

    private static void createHighTierAccelerateRecipe(RecipeOutput provider, int tier) {
        MachineDefinition output = GTNAMachines2.ACCELERATE_HATCHES[tier];
        MachineDefinition previous = GTNAMachines2.ACCELERATE_HATCHES[tier - 1];
        if (output == null || previous == null) return;

        int index = tier - UHV;
        int eut = VA[tier - 1];
        int cwut = 64 << index;
        int solder = 1152 << index;
        String tierName = VN[tier].toLowerCase(java.util.Locale.ROOT);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_accelerate_hatch_" + tierName)
                .inputItems(previous.asStack())
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(getSensor(tier), 4)
                .inputItems(getFieldGenerator(tier), 4)
                .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                .inputItems(TagPrefix.wireGtQuadruple, GTMaterials.RutheniumTriniumAmericiumNeutronate, 4)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(solder))
                .outputItems(output.asStack())
                .duration(600 + 100 * index)
                .EUt(eut)
                .stationResearch(b -> b.researchStack(previous.asStack()).CWUt(cwut).EUt(eut))
                .save(provider);
    }

    private static void createHighTierOverclockRecipe(RecipeOutput provider, int tier) {
        MachineDefinition output = GTNAMachines2.OVERCLOCK_HATCHES[tier];
        MachineDefinition previous = GTNAMachines2.OVERCLOCK_HATCHES[tier - 1];
        if (output == null || previous == null) return;

        int index = tier - UHV;
        int eut = VA[tier - 1];
        int cwut = 64 << index;
        int solder = 1152 << index;
        String tierName = VN[tier].toLowerCase(java.util.Locale.ROOT);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_overclock_hatch_" + tierName)
                .inputItems(previous.asStack())
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(getFieldGenerator(tier), 4)
                // GTCEu's voltage-coil component progression ends at UV.
                .inputItems(GTItems.VOLTAGE_COIL_UV.get(), 4)
                .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                .inputItems(TagPrefix.wireGtOctal, GTMaterials.RutheniumTriniumAmericiumNeutronate, 8)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(solder))
                .outputItems(output.asStack())
                .duration(600 + 100 * index)
                .EUt(eut)
                .stationResearch(b -> b.researchStack(previous.asStack()).CWUt(cwut).EUt(eut))
                .save(provider);
    }

    private static void createAdvancedParallelRecipe(RecipeOutput provider, int tier) {
        MachineDefinition output = GTNAMachines2.ADVANCED_PARALLEL_HATCH[tier];
        MachineDefinition previous = tier == UHV ? GCYMMachines.PARALLEL_HATCH[UV] :
                GTNAMachines2.ADVANCED_PARALLEL_HATCH[tier - 1];
        if (output == null || previous == null) return;

        int index = tier - UHV;
        int eut = VA[tier - 1];
        int cwut = 128 << index;
        int solder = 2304 << index;
        int circuitTier = Math.min(tier + 1, MAX);
        String tierName = VN[tier].toLowerCase(java.util.Locale.ROOT);

        GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_parallel_hatch_" + tierName)
                // The UV GCYM hatch provides 256 parallels; every GTNA step is 4x.
                .inputItems(previous.asStack())
                .inputItems(GTMachines.HULL[tier].asStack())
                .inputItems(getSensor(tier), 2)
                .inputItems(getEmitter(tier), 2)
                .inputItems(getFieldGenerator(tier))
                .inputItems(CustomTags.CIRCUITS_ARRAY[circuitTier], 4)
                .inputItems(TagPrefix.cableGtDouble, GTMaterials.Europium, 8)
                .inputFluids(GTMaterials.SolderingAlloy.getFluid(solder))
                .outputItems(output.asStack())
                .duration(800 + 100 * index)
                .EUt(eut)
                .stationResearch(b -> b.researchStack(previous.asStack()).CWUt(cwut).EUt(eut))
                .save(provider);
    }

    private static void createCraftingCPUInterfaceRecipe(RecipeOutput provider) {
        if (GTNAMachines2.CRAFTING_CPU_INTERFACE == null) {
            return;
        }
        ItemLike hull = GTMachines.HULL[HV].asStack().getItem();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.CRAFTING_CPU_INTERFACE.asStack().getItem())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("AEA")
                .define('A', CustomTags.HV_CIRCUITS)
                .define('B', AEBlocks.CRAFTING_UNIT.stack().getItem())
                .define('C', GTItems.EMITTER_HV.asStack().getItem())
                .define('D', hull)
                .define('E', GTItems.FIELD_GENERATOR_HV.asStack().getItem())
                .unlockedBy("has_hull_hv", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                .save(provider);
    }

    private static void createMEStorageAccessRecipes(RecipeOutput provider) {
        if (GTNAMachines2.ME_STORAGE_ACCESS_HATCH != null) {
            ItemLike hull = GTMachines.HULL[EV].asStack().getItem();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.ME_STORAGE_ACCESS_HATCH.asStack().getItem())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("AEA")
                    .define('A', CustomTags.EV_CIRCUITS)
                    .define('B', AEBlocks.CRAFTING_STORAGE_64K.stack().getItem())
                    .define('C', GTItems.EMITTER_EV.asStack().getItem())
                    .define('D', hull)
                    .define('E', GTItems.FIELD_GENERATOR_EV.asStack().getItem())
                    .unlockedBy("has_hull_ev", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                    .save(provider);
        }
        if (GTNAMachines2.ME_BIG_STORAGE_ACCESS_HATCH != null) {
            ItemLike hull = GTMachines.HULL[IV].asStack().getItem();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                    GTNAMachines2.ME_BIG_STORAGE_ACCESS_HATCH.asStack().getItem())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("AEA")
                    .define('A', CustomTags.IV_CIRCUITS)
                    .define('B', AEBlocks.CRAFTING_STORAGE_256K.stack().getItem())
                    .define('C', GTItems.EMITTER_IV.asStack().getItem())
                    .define('D', hull)
                    .define('E', GTItems.FIELD_GENERATOR_IV.asStack().getItem())
                    .unlockedBy("has_hull_iv", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                    .save(provider);
        }
        if (GTNAMachines2.ME_IO_PORT_HATCH != null) {
            ItemLike hull = GTMachines.HULL[EV].asStack().getItem();
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.ME_IO_PORT_HATCH.asStack().getItem())
                    .pattern("ABA")
                    .pattern("CDC")
                    .pattern("AEA")
                    .define('A', CustomTags.EV_CIRCUITS)
                    .define('B', AEBlocks.IO_PORT.stack().getItem())
                    .define('C', GTItems.CONVEYOR_MODULE_EV.asStack().getItem())
                    .define('D', hull)
                    .define('E', GTItems.FIELD_GENERATOR_EV.asStack().getItem())
                    .unlockedBy("has_hull_ev", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                    .save(provider);
        }
    }

    private static void createAccelerateRecipe(RecipeOutput provider, int tier, ItemLike sensor,
                                               ItemLike middleItem) {
        if (GTNAMachines2.ACCELERATE_HATCHES[tier] == null) return;
        ItemLike hull = GTMachines.HULL[tier].asStack().getItem();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.ACCELERATE_HATCHES[tier].asStack().getItem())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', sensor)
                .define('B', middleItem)
                .define('C', hull)
                .unlockedBy("has_hull", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                .save(provider);
    }

    private static void createOverclockRecipe(RecipeOutput provider, int tier, ItemLike fieldGen,
                                              ItemLike coil) {
        if (GTNAMachines2.OVERCLOCK_HATCHES[tier] == null) return;
        ItemLike hull = GTMachines.HULL[tier].asStack().getItem();
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GTNAMachines2.OVERCLOCK_HATCHES[tier].asStack().getItem())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', fieldGen)
                .define('B', coil)
                .define('C', hull)
                .unlockedBy("has_hull", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                .save(provider);
    }

    private static void createOutputBoostRecipe(RecipeOutput provider, int tier, ItemLike emitter,
                                                ItemLike sensor) {
        if (GTNAMachines2.OUTPUT_BOOST_HATCHES[tier] == null) return;
        ItemLike hull = GTMachines.HULL[tier].asStack().getItem();
        GTNARecipeVisibility.saveRestricted(provider, id("output_boost_hatch_", tier),
                restrictedProvider -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        GTNAMachines2.OUTPUT_BOOST_HATCHES[tier].asStack().getItem())
                        .pattern("ABA")
                        .pattern("BCB")
                        .pattern("ABA")
                        .define('A', emitter)
                        .define('B', sensor)
                        .define('C', hull)
                        .unlockedBy("has_hull", InventoryChangeTrigger.TriggerInstance.hasItems(hull))
                        .save(restrictedProvider));
    }

    private static void createInfiniteInputBusRecipe(RecipeOutput provider, int tier, ItemLike emitter,
                                                     ItemLike sensor) {
        if (GTNAMachines2.INFINITE_INPUT_BUSES[tier] == null) return;
        ItemLike baseBus = GTMachines.ITEM_IMPORT_BUS[tier].asStack().getItem();
        GTNARecipeVisibility.saveRestricted(provider, id("infinite_input_bus_", tier),
                restrictedProvider -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        GTNAMachines2.INFINITE_INPUT_BUSES[tier].asStack().getItem())
                        .pattern("ABA")
                        .pattern("BCB")
                        .pattern("ABA")
                        .define('A', emitter)
                        .define('B', sensor)
                        .define('C', baseBus)
                        .unlockedBy("has_base_bus", InventoryChangeTrigger.TriggerInstance.hasItems(baseBus))
                        .save(restrictedProvider));
    }

    private static void createInfiniteInputHatchRecipe(RecipeOutput provider, int tier, ItemLike emitter,
                                                       ItemLike fieldGenerator) {
        if (GTNAMachines2.INFINITE_INPUT_HATCHES[tier] == null) return;
        ItemLike baseHatch = GTMachines.FLUID_IMPORT_HATCH[tier].asStack().getItem();
        GTNARecipeVisibility.saveRestricted(provider, id("infinite_input_hatch_", tier),
                restrictedProvider -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        GTNAMachines2.INFINITE_INPUT_HATCHES[tier].asStack().getItem())
                        .pattern("ABA")
                        .pattern("BCB")
                        .pattern("ABA")
                        .define('A', emitter)
                        .define('B', fieldGenerator)
                        .define('C', baseHatch)
                        .unlockedBy("has_base_hatch", InventoryChangeTrigger.TriggerInstance.hasItems(baseHatch))
                        .save(restrictedProvider));
    }

    private static void createOutputBoostItemBusRecipe(RecipeOutput provider, int tier, ItemLike emitter,
                                                       ItemLike sensor) {
        if (GTNAMachines2.OUTPUT_BOOST_ITEM_BUSES[tier] == null) return;
        ItemLike baseBus = GTMachines.ITEM_EXPORT_BUS[tier].asStack().getItem();
        GTNARecipeVisibility.saveRestricted(provider, id("output_boost_item_bus_", tier),
                restrictedProvider -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        GTNAMachines2.OUTPUT_BOOST_ITEM_BUSES[tier].asStack().getItem())
                        .pattern("ABA")
                        .pattern("BCB")
                        .pattern("ABA")
                        .define('A', emitter)
                        .define('B', sensor)
                        .define('C', baseBus)
                        .unlockedBy("has_base_bus", InventoryChangeTrigger.TriggerInstance.hasItems(baseBus))
                        .save(restrictedProvider));
    }

    private static void createOutputBoostFluidHatchRecipe(RecipeOutput provider, int tier, ItemLike emitter,
                                                          ItemLike fieldGenerator) {
        if (GTNAMachines2.OUTPUT_BOOST_FLUID_HATCHES[tier] == null) return;
        ItemLike baseHatch = GTMachines.FLUID_EXPORT_HATCH[tier].asStack().getItem();
        GTNARecipeVisibility.saveRestricted(provider, id("output_boost_fluid_hatch_", tier),
                restrictedProvider -> ShapedRecipeBuilder.shaped(RecipeCategory.MISC,
                        GTNAMachines2.OUTPUT_BOOST_FLUID_HATCHES[tier].asStack().getItem())
                        .pattern("ABA")
                        .pattern("BCB")
                        .pattern("ABA")
                        .define('A', emitter)
                        .define('B', fieldGenerator)
                        .define('C', baseHatch)
                        .unlockedBy("has_base_hatch", InventoryChangeTrigger.TriggerInstance.hasItems(baseHatch))
                        .save(restrictedProvider));
    }

    private static ResourceLocation id(String prefix, int tier) {
        return GTNACORE.id(prefix + VN[tier].toLowerCase());
    }

    private static ItemLike getEmitter(int tier) {
        return (switch (tier) {
            case LV -> GTItems.EMITTER_LV;
            case MV -> GTItems.EMITTER_MV;
            case HV -> GTItems.EMITTER_HV;
            case EV -> GTItems.EMITTER_EV;
            case IV -> GTItems.EMITTER_IV;
            case LuV -> GTItems.EMITTER_LuV;
            case ZPM -> GTItems.EMITTER_ZPM;
            case UV -> GTItems.EMITTER_UV;
            case UHV -> GTItems.EMITTER_UHV;
            case UEV -> GTItems.EMITTER_UEV;
            case UIV -> GTItems.EMITTER_UIV;
            case UXV -> GTItems.EMITTER_UXV;
            case OpV -> GTItems.EMITTER_OpV;
            case MAX -> GTItems.EMITTER_OpV;
            default -> GTItems.EMITTER_LV;
        }).asStack().getItem();
    }

    private static ItemLike getSensor(int tier) {
        return (switch (tier) {
            case LV -> GTItems.SENSOR_LV;
            case MV -> GTItems.SENSOR_MV;
            case HV -> GTItems.SENSOR_HV;
            case EV -> GTItems.SENSOR_EV;
            case IV -> GTItems.SENSOR_IV;
            case LuV -> GTItems.SENSOR_LuV;
            case ZPM -> GTItems.SENSOR_ZPM;
            case UV -> GTItems.SENSOR_UV;
            case UHV -> GTItems.SENSOR_UHV;
            case UEV -> GTItems.SENSOR_UEV;
            case UIV -> GTItems.SENSOR_UIV;
            case UXV -> GTItems.SENSOR_UXV;
            case OpV -> GTItems.SENSOR_OpV;
            case MAX -> GTItems.SENSOR_OpV;
            default -> GTItems.SENSOR_LV;
        }).asStack().getItem();
    }

    private static ItemLike getFieldGenerator(int tier) {
        return (switch (tier) {
            case LV -> GTItems.FIELD_GENERATOR_LV;
            case MV -> GTItems.FIELD_GENERATOR_MV;
            case HV -> GTItems.FIELD_GENERATOR_HV;
            case EV -> GTItems.FIELD_GENERATOR_EV;
            case IV -> GTItems.FIELD_GENERATOR_IV;
            case LuV -> GTItems.FIELD_GENERATOR_LuV;
            case ZPM -> GTItems.FIELD_GENERATOR_ZPM;
            case UV -> GTItems.FIELD_GENERATOR_UV;
            case UHV -> GTItems.FIELD_GENERATOR_UHV;
            case UEV -> GTItems.FIELD_GENERATOR_UEV;
            case UIV -> GTItems.FIELD_GENERATOR_UIV;
            case UXV -> GTItems.FIELD_GENERATOR_UXV;
            case OpV -> GTItems.FIELD_GENERATOR_OpV;
            case MAX -> GTItems.FIELD_GENERATOR_OpV;
            default -> GTItems.FIELD_GENERATOR_LV;
        }).asStack().getItem();
    }
}
