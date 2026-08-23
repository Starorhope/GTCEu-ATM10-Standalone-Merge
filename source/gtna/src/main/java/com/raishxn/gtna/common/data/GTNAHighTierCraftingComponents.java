package com.raishxn.gtna.common.data;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.data.recipe.CraftingComponent;
import com.gregtechceu.gtceu.data.recipe.GTCraftingComponents;
import com.gregtechceu.gtceu.data.recipe.event.CraftingComponentModificationEvent;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

import static com.gregtechceu.gtceu.api.GTValues.MAX;
import static com.gregtechceu.gtceu.api.GTValues.OpV;
import static com.gregtechceu.gtceu.api.GTValues.UEV;

/**
 * Replaces GTCEu's low-tier fallbacks for the tiers exposed by GTNA.
 *
 * <p>GTCEu intentionally ships UEV-MAX machine registrations without a complete
 * material/component progression. GTNA opts into those registrations, so every
 * crafting component used by the generated machine recipes must be supplied here.</p>
 */
public final class GTNAHighTierCraftingComponents {

    private GTNAHighTierCraftingComponents() {}

    @SubscribeEvent
    public static void modify(CraftingComponentModificationEvent event) {
        for (int tier = UEV; tier <= MAX; tier++) {
            addMaterial(GTCraftingComponents.WIRE_ELECTRIC, tier, TagPrefix.wireGtSingle,
                    GTMaterials.RutheniumTriniumAmericiumNeutronate);
            addMaterial(GTCraftingComponents.WIRE_QUAD, tier, TagPrefix.wireGtQuadruple,
                    GTMaterials.RutheniumTriniumAmericiumNeutronate);
            addMaterial(GTCraftingComponents.WIRE_OCT, tier, TagPrefix.wireGtOctal,
                    GTMaterials.RutheniumTriniumAmericiumNeutronate);
            addMaterial(GTCraftingComponents.WIRE_HEX, tier, TagPrefix.wireGtHex,
                    GTMaterials.RutheniumTriniumAmericiumNeutronate);

            addMaterial(GTCraftingComponents.CABLE, tier, TagPrefix.cableGtSingle,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_DOUBLE, tier, TagPrefix.cableGtDouble,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_QUAD, tier, TagPrefix.cableGtQuadruple,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_OCT, tier, TagPrefix.cableGtOctal,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_HEX, tier, TagPrefix.cableGtHex,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_TIER_UP, tier, TagPrefix.cableGtSingle,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_TIER_UP_DOUBLE, tier, TagPrefix.cableGtDouble,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_TIER_UP_QUAD, tier, TagPrefix.cableGtQuadruple,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_TIER_UP_OCT, tier, TagPrefix.cableGtOctal,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.CABLE_TIER_UP_HEX, tier, TagPrefix.cableGtHex,
                    GTMaterials.Europium);

            addMaterial(GTCraftingComponents.PIPE_NORMAL, tier, TagPrefix.pipeNormalFluid, GTMaterials.Neutronium);
            addMaterial(GTCraftingComponents.PIPE_LARGE, tier, TagPrefix.pipeLargeFluid, GTMaterials.Neutronium);
            addMaterial(GTCraftingComponents.PIPE_NONUPLE, tier, TagPrefix.pipeNonupleFluid, GTMaterials.Neutronium);
            GTCraftingComponents.GLASS.add(tier, GTBlocks.FUSION_GLASS.asStack());
            addMaterial(GTCraftingComponents.PLATE, tier, TagPrefix.plate, GTMaterials.Neutronium);
            addMaterial(GTCraftingComponents.HULL_PLATE, tier, TagPrefix.plate, GTMaterials.Polybenzimidazole);
            addMaterial(GTCraftingComponents.ROTOR, tier, TagPrefix.rotor, GTMaterials.Neutronium);
            GTCraftingComponents.GRINDER.add(tier, GTItems.COMPONENT_GRINDER_TUNGSTEN.asStack());
            addMaterial(GTCraftingComponents.SAWBLADE, tier, TagPrefix.toolHeadBuzzSaw, GTMaterials.Duranium);

            addMaterial(GTCraftingComponents.COIL_HEATING, tier, TagPrefix.wireGtDouble, GTMaterials.Trinium);
            addMaterial(GTCraftingComponents.COIL_HEATING_DOUBLE, tier, TagPrefix.wireGtQuadruple,
                    GTMaterials.Trinium);
            addMaterial(GTCraftingComponents.COIL_ELECTRIC, tier, TagPrefix.wireGtOctal,
                    GTMaterials.RutheniumTriniumAmericiumNeutronate);
            addMaterial(GTCraftingComponents.ROD_MAGNETIC, tier, TagPrefix.block, GTMaterials.SamariumMagnetic);
            addMaterial(GTCraftingComponents.ROD_DISTILLATION, tier, TagPrefix.spring, GTMaterials.Trinium);
            addMaterial(GTCraftingComponents.ROD_ELECTROMAGNETIC, tier, TagPrefix.rod, GTMaterials.VanadiumGallium);
            addMaterial(GTCraftingComponents.ROD_RADIOACTIVE, tier, TagPrefix.rod, GTMaterials.Tritanium);
            addMaterial(GTCraftingComponents.PIPE_REACTOR, tier, TagPrefix.pipeNormalFluid,
                    GTMaterials.Polybenzimidazole);
            GTCraftingComponents.POWER_COMPONENT.add(tier,
                    GTItems.ULTRA_HIGH_POWER_INTEGRATED_CIRCUIT.asStack());
            GTCraftingComponents.VOLTAGE_COIL.add(tier, GTItems.VOLTAGE_COIL_UV.asStack());
            addMaterial(GTCraftingComponents.SPRING, tier, TagPrefix.spring, GTMaterials.Europium);
            GTCraftingComponents.CRATE.add(tier, GTMachines.SUPER_CHEST[2].asStack());
            GTCraftingComponents.DRUM.add(tier, GTMachines.SUPER_TANK[2].asStack());
            addMaterial(GTCraftingComponents.FRAME, tier, TagPrefix.frameGt, GTMaterials.Neutronium);
            addMaterial(GTCraftingComponents.SMALL_SPRING_TRANSFORMER, tier, TagPrefix.springSmall,
                    GTMaterials.Europium);
            addMaterial(GTCraftingComponents.SPRING_TRANSFORMER, tier, TagPrefix.spring, GTMaterials.Europium);
        }

        // GTCEu has no MAX-tier component items. Use the last real component tier
        // instead of silently falling back to LV when another high-tier recipe asks.
        addMaxComponentFallbacks();
    }

    private static void addMaxComponentFallbacks() {
        GTCraftingComponents.MOTOR.add(MAX, GTItems.ELECTRIC_MOTOR_OpV.asStack());
        GTCraftingComponents.PUMP.add(MAX, GTItems.ELECTRIC_PUMP_OpV.asStack());
        GTCraftingComponents.PISTON.add(MAX, GTItems.ELECTRIC_PISTON_OpV.asStack());
        GTCraftingComponents.CONVEYOR.add(MAX, GTItems.CONVEYOR_MODULE_OpV.asStack());
        GTCraftingComponents.ROBOT_ARM.add(MAX, GTItems.ROBOT_ARM_OpV.asStack());
        GTCraftingComponents.SENSOR.add(MAX, GTItems.SENSOR_OpV.asStack());
        GTCraftingComponents.EMITTER.add(MAX, GTItems.EMITTER_OpV.asStack());
        GTCraftingComponents.FIELD_GENERATOR.add(MAX, GTItems.FIELD_GENERATOR_OpV.asStack());
    }

    private static void addMaterial(CraftingComponent component, int tier, TagPrefix prefix, Material material) {
        component.add(tier, prefix, material);
    }
}
