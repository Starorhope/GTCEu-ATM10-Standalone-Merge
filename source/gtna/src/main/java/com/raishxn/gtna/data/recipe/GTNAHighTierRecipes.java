package com.raishxn.gtna.data.recipe;

import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.CustomTags;

import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.data.GTNAItems;

import java.util.Locale;

import static com.gregtechceu.gtceu.api.GTValues.MAX;
import static com.gregtechceu.gtceu.api.GTValues.OpV;
import static com.gregtechceu.gtceu.api.GTValues.UEV;
import static com.gregtechceu.gtceu.api.GTValues.UHV;
import static com.gregtechceu.gtceu.api.GTValues.VA;
import static com.gregtechceu.gtceu.api.GTValues.VN;

/** A complete, non-circular progression for the high tiers enabled by GTNA. */
public final class GTNAHighTierRecipes {

    private GTNAHighTierRecipes() {}

    public static void register(RecipeOutput provider) {
        ItemStack[] circuits = {
                GTNAItems.UEV_CIRCUIT.asStack(),
                GTNAItems.UIV_CIRCUIT.asStack(),
                GTNAItems.UXV_CIRCUIT.asStack(),
                GTNAItems.OPV_CIRCUIT.asStack(),
                GTNAItems.MAX_CIRCUIT.asStack()
        };

        ItemStack[] motors = {
                GTItems.ELECTRIC_MOTOR_UHV.asStack(), GTItems.ELECTRIC_MOTOR_UEV.asStack(),
                GTItems.ELECTRIC_MOTOR_UIV.asStack(), GTItems.ELECTRIC_MOTOR_UXV.asStack(),
                GTItems.ELECTRIC_MOTOR_OpV.asStack()
        };
        ItemStack[] pumps = {
                GTItems.ELECTRIC_PUMP_UHV.asStack(), GTItems.ELECTRIC_PUMP_UEV.asStack(),
                GTItems.ELECTRIC_PUMP_UIV.asStack(), GTItems.ELECTRIC_PUMP_UXV.asStack(),
                GTItems.ELECTRIC_PUMP_OpV.asStack()
        };
        ItemStack[] pistons = {
                GTItems.ELECTRIC_PISTON_UHV.asStack(), GTItems.ELECTRIC_PISTON_UEV.asStack(),
                GTItems.ELECTRIC_PISTON_UIV.asStack(), GTItems.ELECTRIC_PISTON_UXV.asStack(),
                GTItems.ELECTRIC_PISTON_OpV.asStack()
        };
        ItemStack[] conveyors = {
                GTItems.CONVEYOR_MODULE_UHV.asStack(), GTItems.CONVEYOR_MODULE_UEV.asStack(),
                GTItems.CONVEYOR_MODULE_UIV.asStack(), GTItems.CONVEYOR_MODULE_UXV.asStack(),
                GTItems.CONVEYOR_MODULE_OpV.asStack()
        };
        ItemStack[] robotArms = {
                GTItems.ROBOT_ARM_UHV.asStack(), GTItems.ROBOT_ARM_UEV.asStack(),
                GTItems.ROBOT_ARM_UIV.asStack(), GTItems.ROBOT_ARM_UXV.asStack(),
                GTItems.ROBOT_ARM_OpV.asStack()
        };
        ItemStack[] sensors = {
                GTItems.SENSOR_UHV.asStack(), GTItems.SENSOR_UEV.asStack(), GTItems.SENSOR_UIV.asStack(),
                GTItems.SENSOR_UXV.asStack(), GTItems.SENSOR_OpV.asStack()
        };
        ItemStack[] emitters = {
                GTItems.EMITTER_UHV.asStack(), GTItems.EMITTER_UEV.asStack(), GTItems.EMITTER_UIV.asStack(),
                GTItems.EMITTER_UXV.asStack(), GTItems.EMITTER_OpV.asStack()
        };
        ItemStack[] fieldGenerators = {
                GTItems.FIELD_GENERATOR_UHV.asStack(), GTItems.FIELD_GENERATOR_UEV.asStack(),
                GTItems.FIELD_GENERATOR_UIV.asStack(), GTItems.FIELD_GENERATOR_UXV.asStack(),
                GTItems.FIELD_GENERATOR_OpV.asStack()
        };

        registerCircuitProgression(provider, circuits, motors, sensors, emitters, fieldGenerators);
        registerComponentProgression(provider, motors, pumps, pistons, conveyors, robotArms, sensors, emitters,
                fieldGenerators);
        registerCasingProgression(provider, circuits);
    }

    private static void registerCircuitProgression(RecipeOutput provider, ItemStack[] circuits, ItemStack[] motors,
                                                    ItemStack[] sensors, ItemStack[] emitters,
                                                    ItemStack[] fieldGenerators) {
        ItemStack previousResearch = GTItems.WETWARE_MAINFRAME_UHV.asStack();
        for (int tier = UEV; tier <= MAX; tier++) {
            int index = tier - UEV;
            int componentIndex = tier - UEV;
            String tierName = tierName(tier);
            ItemStack researchStack = previousResearch;
            int researchEUt = VA[tier - 1];
            int researchCWUt = 128 << index;

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_" + tierName + "_circuit")
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier - 1], 4)
                    .inputItems(motors[componentIndex].getItem(), 2)
                    .inputItems(sensors[componentIndex].getItem(), 2)
                    .inputItems(emitters[componentIndex].getItem(), 2)
                    .inputItems(fieldGenerators[componentIndex])
                    .inputItems(GTItems.ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT, 32)
                    .inputItems(TagPrefix.foil, GTMaterials.Neutronium, 32)
                    .inputItems(TagPrefix.wireFine, GTMaterials.RutheniumTriniumAmericiumNeutronate, 64)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(2304))
                    .inputFluids(GTMaterials.Neutronium.getFluid(576))
                    .outputItems(circuits[index])
                    .duration(1200)
                    .EUt(researchEUt)
                    .stationResearch(b -> b
                            .researchStack(researchStack)
                            .CWUt(researchCWUt)
                            .EUt(researchEUt))
                    .save(provider);

            previousResearch = circuits[index];
        }
    }

    private static void registerComponentProgression(RecipeOutput provider, ItemStack[] motors, ItemStack[] pumps,
                                                      ItemStack[] pistons, ItemStack[] conveyors,
                                                      ItemStack[] robotArms, ItemStack[] sensors,
                                                      ItemStack[] emitters, ItemStack[] fieldGenerators) {
        ItemStack[] previousMotors = prepend(GTItems.ELECTRIC_MOTOR_UV.asStack(), motors);
        ItemStack[] previousPumps = prepend(GTItems.ELECTRIC_PUMP_UV.asStack(), pumps);
        ItemStack[] previousPistons = prepend(GTItems.ELECTRIC_PISTON_UV.asStack(), pistons);
        ItemStack[] previousConveyors = prepend(GTItems.CONVEYOR_MODULE_UV.asStack(), conveyors);
        ItemStack[] previousRobotArms = prepend(GTItems.ROBOT_ARM_UV.asStack(), robotArms);
        ItemStack[] previousSensors = prepend(GTItems.SENSOR_UV.asStack(), sensors);
        ItemStack[] previousEmitters = prepend(GTItems.EMITTER_UV.asStack(), emitters);
        ItemStack[] previousFieldGenerators = prepend(GTItems.FIELD_GENERATOR_UV.asStack(), fieldGenerators);

        for (int tier = UHV; tier <= OpV; tier++) {
            int index = tier - UHV;
            String suffix = tierName(tier);
            int eut = VA[tier - 1];
            int cwut = 64 << index;

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_electric_motor_" + suffix)
                    .inputItems(previousMotors[index])
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                    .inputItems(TagPrefix.rodLong, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.wireFine, GTMaterials.RutheniumTriniumAmericiumNeutronate, 64)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(1152))
                    .inputFluids(GTMaterials.Lubricant.getFluid(1000))
                    .outputItems(motors[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousMotors[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_electric_pump_" + suffix)
                    .inputItems(previousPumps[index])
                    .inputItems(motors[index])
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                    .inputItems(TagPrefix.pipeNormalFluid, GTMaterials.Neutronium, 2)
                    .inputItems(TagPrefix.rotor, GTMaterials.Neutronium, 2)
                    .inputItems(TagPrefix.screw, GTMaterials.Neutronium, 8)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(1152))
                    .inputFluids(GTMaterials.Lubricant.getFluid(2000))
                    .outputItems(pumps[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousPumps[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_electric_piston_" + suffix)
                    .inputItems(previousPistons[index])
                    .inputItems(motors[index].getItem(), 2)
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.rodLong, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.gear, GTMaterials.Neutronium, 2)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(1152))
                    .inputFluids(GTMaterials.Lubricant.getFluid(1000))
                    .outputItems(pistons[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousPistons[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_conveyor_module_" + suffix)
                    .inputItems(previousConveyors[index])
                    .inputItems(motors[index].getItem(), 2)
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.StyreneButadieneRubber.getFluid(3456))
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(1152))
                    .inputFluids(GTMaterials.Lubricant.getFluid(1000))
                    .outputItems(conveyors[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousConveyors[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_robot_arm_" + suffix)
                    .inputItems(previousRobotArms[index])
                    .inputItems(motors[index].getItem(), 2)
                    .inputItems(pistons[index])
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 4)
                    .inputItems(TagPrefix.rodLong, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.gear, GTMaterials.Neutronium, 2)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(2304))
                    .inputFluids(GTMaterials.Lubricant.getFluid(2000))
                    .outputItems(robotArms[index])
                    .duration(800).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousRobotArms[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_sensor_" + suffix)
                    .inputItems(previousSensors[index])
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 4)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.rod, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.wireFine, GTMaterials.RutheniumTriniumAmericiumNeutronate, 64)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(2304))
                    .outputItems(sensors[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousSensors[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_emitter_" + suffix)
                    .inputItems(previousEmitters[index])
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 4)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.rodLong, GTMaterials.Neutronium, 4)
                    .inputItems(TagPrefix.wireFine, GTMaterials.RutheniumTriniumAmericiumNeutronate, 64)
                    .inputItems(TagPrefix.cableGtSingle, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(2304))
                    .outputItems(emitters[index])
                    .duration(600).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousEmitters[index]).CWUt(cwut).EUt(eut))
                    .save(provider);

            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_field_generator_" + suffix)
                    .inputItems(previousFieldGenerators[index])
                    .inputItems(emitters[index].getItem(), 4)
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 8)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 8)
                    .inputItems(TagPrefix.wireGtOctal, GTMaterials.RutheniumTriniumAmericiumNeutronate, 16)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(4608))
                    .inputFluids(GTMaterials.Neutronium.getFluid(1152))
                    .outputItems(fieldGenerators[index])
                    .duration(1000).EUt(eut)
                    .stationResearch(b -> b.researchStack(previousFieldGenerators[index]).CWUt(cwut).EUt(eut))
                    .save(provider);
        }
    }

    private static void registerCasingProgression(RecipeOutput provider, ItemStack[] circuits) {
        ItemStack[] casings = {
                GTBlocks.MACHINE_CASING_UEV.asStack(), GTBlocks.MACHINE_CASING_UIV.asStack(),
                GTBlocks.MACHINE_CASING_UXV.asStack(), GTBlocks.MACHINE_CASING_OpV.asStack(),
                GTBlocks.MACHINE_CASING_MAX.asStack()
        };
        ItemStack previous = GTBlocks.MACHINE_CASING_UHV.asStack();
        for (int tier = UEV; tier <= MAX; tier++) {
            int index = tier - UEV;
            ItemStack researchStack = previous;
            int researchEUt = VA[tier - 1];
            int researchCWUt = 128 << index;
            GTRecipeTypes.ASSEMBLY_LINE_RECIPES.recipeBuilder("gtna_machine_casing_" + tierName(tier))
                    .inputItems(previous)
                    .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 8)
                    .inputItems(CustomTags.CIRCUITS_ARRAY[tier], 2)
                    .inputItems(TagPrefix.cableGtQuadruple, GTMaterials.Europium, 4)
                    .inputFluids(GTMaterials.SolderingAlloy.getFluid(1152))
                    .inputFluids(GTMaterials.Neutronium.getFluid(576))
                    .outputItems(casings[index])
                    .duration(600).EUt(researchEUt)
                    .stationResearch(b -> b.researchStack(researchStack).CWUt(researchCWUt).EUt(researchEUt))
                    .save(provider);
            previous = casings[index];
        }
    }

    private static ItemStack[] prepend(ItemStack first, ItemStack[] following) {
        ItemStack[] result = new ItemStack[following.length];
        result[0] = first;
        System.arraycopy(following, 0, result, 1, following.length - 1);
        return result;
    }

    private static String tierName(int tier) {
        return VN[tier].toLowerCase(Locale.ROOT);
    }
}
