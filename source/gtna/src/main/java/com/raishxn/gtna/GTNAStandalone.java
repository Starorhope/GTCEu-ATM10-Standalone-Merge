package com.raishxn.gtna;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;

/** Standalone NeoForge entry point; omitted when GTNA is embedded into GTCEu. */
@Mod(GTNACORE.MOD_ID)
public final class GTNAStandalone {

    public GTNAStandalone(IEventBus modBus, FMLModContainer container) {
        new GTNACORE(modBus, container);
    }
}
