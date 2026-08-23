package com.raishxn.gtna.api.registry;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

import com.raishxn.gtna.GTNACORE;

public class GTNARegistry {

    // A merged build has no logical "gtna" ModContainer. In that case GTCEu's helper safely
    // binds the registrate to the currently active (gtceu) mod bus without emitting false FATALs.
    public static final GTRegistrate REGISTRATE = GTRegistrate.createIgnoringListenerErrors(GTNACORE.MOD_ID);

    static {
        GTNARegistry.REGISTRATE.defaultCreativeTab((ResourceKey<CreativeModeTab>) null);
    }

    private GTNARegistry() {/**/}
}
