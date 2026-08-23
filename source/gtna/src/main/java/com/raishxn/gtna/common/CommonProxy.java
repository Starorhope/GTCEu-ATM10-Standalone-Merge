package com.raishxn.gtna.common;

import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.data.info.GTNAMaterialFlags;
import com.raishxn.gtna.api.data.tag.GTNATagPrefix;
import com.raishxn.gtna.common.data.*;
import com.raishxn.gtna.data.GTNALangProvider;
import com.raishxn.gtna.data.recipe.GTNARecipeConditions;
import com.raishxn.gtna.network.GTNANetworkHandler;

import java.util.concurrent.CompletableFuture;

import static com.raishxn.gtna.api.registry.GTNARegistry.REGISTRATE;

public class CommonProxy {

    private boolean didRegisterContent;

    public CommonProxy(IEventBus eventBus) {
        REGISTRATE.registerEventListeners(eventBus);
        eventBus.addListener(this::registerContent);
        eventBus.addListener(this::modifyMaterials);
        eventBus.addListener(this::gatherData);
        eventBus.addListener(GTNANetworkHandler::registerPayloads);
    }

    public void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        boolean server = event.includeServer();
        boolean client = event.includeClient();
        generator.addProvider(client, new GTNALangProvider(packOutput));
    }

    /** Load all former IGTAddon callback content during GT 8's pre-freeze registration pass. */
    private void registerContent(RegisterEvent event) {
        if (didRegisterContent) {
            return;
        }
        didRegisterContent = true;

        GTNAElements.init();
        GTNAMaterialFlags.register();
        GTNATagPrefix.register();
        GTNAMaterials.init();
        // This listener runs at NORMAL after GTCEu's own NORMAL listener. GTRegistrate
        // commits entries later at LOW/LOWEST, so declarations made here are still timely
        // without loading GTItems before GTCEu has initialized its materials.
        GTNACreativeModeTabs.init();
        GTNARecipeConditions.init();
        GTNARecipeType.init();
        GTNACovers.init();
        GTNAItems.init();
        GTNAMachines.init();
        GTNAMachines2.init();
        GTNAEnergyHatches.init();
    }

    private void modifyMaterials(PostMaterialEvent event) {}
}
