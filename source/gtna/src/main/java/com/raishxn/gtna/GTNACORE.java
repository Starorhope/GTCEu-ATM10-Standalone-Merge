package com.raishxn.gtna;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

import com.raishxn.gtna.client.ClientProxy;
import com.raishxn.gtna.common.CommonProxy;
import com.raishxn.gtna.common.command.GTNACommands;
import com.raishxn.gtna.common.data.condition.RestrictedItemsEnabledForgeCondition;
import com.raishxn.gtna.common.data.GTNAHighTierCraftingComponents;
import com.raishxn.gtna.common.item.TesseractTargetMarkerBehavior;
import com.raishxn.gtna.common.item.armor.QuantumCosmicNexusArmorHandler;
import com.raishxn.gtna.config.GTNAConfigBootstrap;
import com.gregtechceu.gtceu.data.pack.GTDynamicDataPack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("removal")
public class GTNACORE {

    public static final String MOD_ID = "gtna";
    public static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    public GTNACORE(IEventBus modBus, FMLModContainer container) {
        bootstrap(modBus, container, false);
    }

    /**
     * Starts GTNA when its classes are merged into a JAR exposing only GTCEu's logical mod container.
     * The supplied bus/container therefore belong to GTCEu; GTNA must not install its config screen on it.
     */
    public static void bootstrapMerged(IEventBus modBus, FMLModContainer container) {
        bootstrap(modBus, container, true);
    }

    private static void bootstrap(IEventBus modBus, FMLModContainer container, boolean merged) {
        if (!BOOTSTRAPPED.compareAndSet(false, true)) {
            return;
        }

        if (merged) {
            // AddonFinder exposes only the owning logical mod id (gtceu) in a merged file.
            // Keep GTNA's generated recipes visible through GTCEu's dynamic server pack.
            GTDynamicDataPack.addNamespace(MOD_ID);
        }
        GTNAConfigBootstrap.init();
        NeoForge.EVENT_BUS.register(GTNAHighTierCraftingComponents.class);
        RestrictedItemsEnabledForgeCondition.register(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            if (merged) {
                new ClientProxy(modBus);
            } else {
                new ClientProxy(modBus, container);
            }
        } else {
            new CommonProxy(modBus);
        }

        if (merged) {
            registerMergedGameSubscribers();
        }
    }

    private static void registerMergedGameSubscribers() {
        NeoForge.EVENT_BUS.register(GTNACommands.class);
        NeoForge.EVENT_BUS.register(TesseractTargetMarkerBehavior.class);
        NeoForge.EVENT_BUS.register(QuantumCosmicNexusArmorHandler.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientProxy.registerMergedGameSubscribers(NeoForge.EVENT_BUS);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
