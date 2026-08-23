//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
package com.raishxn.gtna.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.resources.model.ModelResourceLocation;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.client.renderer.BlockHighlightHandler;
import com.raishxn.gtna.client.renderer.TesseractTargetHighlightHandler;
import com.raishxn.gtna.client.renderer.machine.AnnihilateGeneratorRenderer;
import com.raishxn.gtna.client.renderer.machine.EyeOfHarmonyRenderer;
import com.raishxn.gtna.client.renderer.machine.EyeOfWoodRenderer;
import com.raishxn.gtna.common.CommonProxy;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;
import com.gregtechceu.gtceu.data.pack.GTDynamicResourcePack;
import dev.toma.configuration.client.ConfigurationClient;

@OnlyIn(Dist.CLIENT)
public class ClientProxy extends CommonProxy {

    /** Merged mode: initialize client listeners without attaching a screen to GTCEu's container. */
    public ClientProxy(IEventBus modBus) {
        this(modBus, true);
    }

    private ClientProxy(IEventBus modBus, boolean merged) {
        super(modBus);
        if (merged) {
            // The merged physical file has only the gtceu ModInfo, so add GTNA's client domain explicitly.
            GTDynamicResourcePack.addNamespace(GTNACORE.MOD_ID);
        }
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::registerAdditionalModels);
    }

    /** Standalone mode: preserve GTNA's own configuration-screen extension point. */
    public ClientProxy(IEventBus modBus, FMLModContainer container) {
        this(modBus, false);
        IConfigScreenFactory configScreenFactory =
                (ignored, parent) -> ConfigurationClient.getConfigScreen(GTNACORE.MOD_ID, parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, configScreenFactory);
    }

    public static void registerMergedGameSubscribers(IEventBus gameBus) {
        gameBus.register(ClientEventHandler.class);
        gameBus.register(BlockHighlightHandler.class);
        gameBus.register(TesseractTargetHighlightHandler.class);
    }

    private void clientSetup(FMLClientSetupEvent event) {
        registerDynamicRenderers();
    }

    private void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/star")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/space")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/overworld")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/the_nether")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/the_end")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/eye_of_wood_sweat")));
        event.register(ModelResourceLocation.standalone(GTNACORE.id("obj/eye_of_wood_thinking")));
    }

    private static void registerDynamicRenderers() {
        DynamicRenderManager.register(
                GTNACORE.id("annihilate_generator/star"), AnnihilateGeneratorRenderer.TYPE);
        DynamicRenderManager.register(
                GTNACORE.id("eye_of_harmony/render"), EyeOfHarmonyRenderer.TYPE);
        DynamicRenderManager.register(
                GTNACORE.id("eye_of_wood/render"), EyeOfWoodRenderer.TYPE);
    }
}
