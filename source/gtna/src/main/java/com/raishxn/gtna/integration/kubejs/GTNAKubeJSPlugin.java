package com.raishxn.gtna.integration.kubejs;

import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.integration.kjs.builders.machine.MultiblockMachineBuilderWrapper;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.machine.multiblock.electric.WorkableElectricMultipleRecipesMachine;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class GTNAKubeJSPlugin implements KubeJSPlugin {

    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(GTRegistries.Keys.MACHINE, machineRegistry -> machineRegistry.add(
                GTNACORE.id("multiple_recipes"),
                MultiblockMachineBuilderWrapper.class,
                id -> MultiblockMachineBuilderWrapper.createKJSMulti(
                        id, WorkableElectricMultipleRecipesMachine::new)));
    }

    @Override
    public void registerBindings(BindingRegistry event) {
        event.add("GTNAPartAbility", GTNAPartAbilityWrapper.class);
    }
}
