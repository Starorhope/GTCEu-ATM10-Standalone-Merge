package yuuki1293.pccard;

import appeng.api.ids.AECreativeTabIds;
import appeng.api.upgrades.Upgrades;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEParts;
import appeng.core.localization.GuiText;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import yuuki1293.pccard.api.PatternProviderRegistration;
import yuuki1293.pccard.datagen.PCCDataGen;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class PCCard {
    public static final String MODID = "pccard";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredItem<Item> PROGRAMMED_CIRCUIT_CARD_ITEM = ITEMS.register("card_programmed_circuit", () -> Upgrades.createUpgradeCardItem(new Item.Properties()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RECIPE_CIRCUIT = DATA_COMPONENTS
        .registerComponentType("recipe_circuit", builder -> builder
            .persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.VAR_INT));

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();
    private final String packOwnerModId;

    public PCCard(IEventBus modEventBus, ModContainer modContainer) {
        this(MODID);
        bootstrap(modEventBus, modContainer, this, false);
    }

    private PCCard(String packOwnerModId) {
        this.packOwnerModId = Objects.requireNonNull(packOwnerModId, "packOwnerModId");
    }

    /**
     * Initializes PCC when its classes and resources are merged into another logical mod container.
     * Registry and instance-event setup is shared with the standalone entry point, while configuration
     * filenames remain in PCC's namespace and its data-generator subscriber is registered explicitly.
     */
    public static void bootstrapMerged(IEventBus modEventBus, ModContainer modContainer) {
        bootstrap(modEventBus, modContainer, new PCCard(modContainer.getModId()), true);
    }

    private static void bootstrap(IEventBus modEventBus, ModContainer modContainer, PCCard eventHandler,
                                  boolean merged) {
        Objects.requireNonNull(modEventBus, "modEventBus");
        Objects.requireNonNull(modContainer, "modContainer");
        Objects.requireNonNull(eventHandler, "eventHandler");
        if (!BOOTSTRAPPED.compareAndSet(false, true)) {
            LOGGER.debug("Programmed Circuit Card bootstrap already completed; ignoring duplicate request");
            return;
        }

        ITEMS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);
        modEventBus.register(eventHandler);
        if (merged) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC, "pccard-client.toml");
            modContainer.registerConfig(ModConfig.Type.COMMON, ConfigCommon.SPEC, "pccard-common.toml");
            modEventBus.register(PCCDataGen.class);
        } else {
            modContainer.registerConfig(ModConfig.Type.CLIENT, ConfigClient.SPEC);
            modContainer.registerConfig(ModConfig.Type.COMMON, ConfigCommon.SPEC);
        }
    }

    @SubscribeEvent
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(this::postRegistrationInitialization).whenComplete((res, err) -> {
            if (err != null) {
                LOGGER.warn(err.getMessage());
            }
        });
    }

    public void postRegistrationInitialization() {
        var patternProviderGroup = GuiText.CraftingInterface.getTranslationKey();
        var item = PROGRAMMED_CIRCUIT_CARD_ITEM.get();

        // AE2 Pattern Provider
        PatternProviderRegistration.register(AEParts.PATTERN_PROVIDER, patternProviderGroup);
        PatternProviderRegistration.register(AEBlocks.PATTERN_PROVIDER, patternProviderGroup);

        // Extended AE Pattern Provider
        {
            var exPatternProviderGroup = "gui.extendedae.ex_pattern_provider";
            var resourceExBE = ResourceLocation.tryBuild("extendedae", "ex_pattern_provider");
            var resourceExPart = ResourceLocation.tryBuild("extendedae", "ex_pattern_provider_part");
            var patternProviderExBE = BuiltInRegistries.BLOCK.getOptional(resourceExBE);
            var patternProviderExPart = BuiltInRegistries.ITEM.getOptional(resourceExPart);
            if (patternProviderExBE.isPresent() && patternProviderExPart.isPresent()) {
                Upgrades.add(item, patternProviderExBE.get(), 1, exPatternProviderGroup);
                Upgrades.add(item, patternProviderExPart.get(), 1, exPatternProviderGroup);
            }
        }

        // Advanced AE Pattern Provider
        {
            var adPatternProviderGroup = "gui.advanced_ae.AdvPatternProvider";
            var namespaceAd = "advanced_ae";
            var resourceAdBE = ResourceLocation.tryBuild(namespaceAd, "small_adv_pattern_provider");
            var resourceAdPart = ResourceLocation.tryBuild(namespaceAd, "small_adv_pattern_provider_part");
            var resourceAdExBE = ResourceLocation.tryBuild(namespaceAd, "adv_pattern_provider");
            var resourceAdExPart = ResourceLocation.tryBuild(namespaceAd, "adv_pattern_provider_part");
            var patternProviderAdBE = BuiltInRegistries.BLOCK.getOptional(resourceAdBE);
            var patternProviderAdPart = BuiltInRegistries.ITEM.getOptional(resourceAdPart);
            var patternProviderAdExBE = BuiltInRegistries.BLOCK.getOptional(resourceAdExBE);
            var patternProviderAdExPart = BuiltInRegistries.ITEM.getOptional(resourceAdExPart);
            if (patternProviderAdBE.isPresent() && patternProviderAdPart.isPresent() && patternProviderAdExBE.isPresent() && patternProviderAdExPart.isPresent()) {
                Upgrades.add(item, patternProviderAdBE.get(), 1, adPatternProviderGroup);
                Upgrades.add(item, patternProviderAdPart.get(), 1, adPatternProviderGroup);
                Upgrades.add(item, patternProviderAdExBE.get(), 1, adPatternProviderGroup);
                Upgrades.add(item, patternProviderAdExPart.get(), 1, adPatternProviderGroup);
            }
        }

        // Expanded AE Pattern Provider
        {
            var expPatternProviderGroup = "gui.expandedae.exp_pattern_provider";
            var namespaceExp = "expandedae";
            var resourceExpBE = ResourceLocation.tryBuild(namespaceExp, "exp_pattern_provider");
            var resourceExpPart = ResourceLocation.tryBuild(namespaceExp, "exp_pattern_provider_part");
            var patternProviderExpBE = BuiltInRegistries.BLOCK.getOptional(resourceExpBE);
            var patternProviderExpPart = BuiltInRegistries.ITEM.getOptional(resourceExpPart);
            if (patternProviderExpBE.isPresent() && patternProviderExpPart.isPresent()) {
                Upgrades.add(item, patternProviderExpBE.get(), 1, expPatternProviderGroup);
                Upgrades.add(item, patternProviderExpPart.get(), 1, expPatternProviderGroup);
            }
        }

        // Mega Cells Pattern Provider
        {
            var megaPatternProviderGroup = "block.megacells.mega_pattern_provider";
            var namespaceMega = "megacells";
            var resourceMegaBE = ResourceLocation.tryBuild(namespaceMega, "mega_pattern_provider");
            var resourceMegaPart = ResourceLocation.tryBuild(namespaceMega, "cable_mega_pattern_provider");
            var patternProviderMegaBE = BuiltInRegistries.BLOCK.getOptional(resourceMegaBE);
            var patternProviderMegaPart = BuiltInRegistries.ITEM.getOptional(resourceMegaPart);
            if (patternProviderMegaBE.isPresent() && patternProviderMegaPart.isPresent()) {
                Upgrades.add(item, patternProviderMegaBE.get(), 1, megaPatternProviderGroup);
                Upgrades.add(item, patternProviderMegaPart.get(), 1, megaPatternProviderGroup);
            }
        }
    }

    @SubscribeEvent
    public void onBuildCreativeModeTabContentsEvent(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(AECreativeTabIds.MAIN)) {
            event.accept(PROGRAMMED_CIRCUIT_CARD_ITEM);
            LOGGER.debug("Add Programmed Circuit Card in AE2 creative tab");
        }
    }

    @SubscribeEvent
    public void onAddPackFindersEvent(AddPackFindersEvent event) {
        event.addPackFinders(
            ResourceLocation.fromNamespaceAndPath(packOwnerModId, "resourcepacks/pccard_modern"),
            PackType.CLIENT_RESOURCES,
            Component.translatable("resourcepack.pccard.modern"),
            PackSource.BUILT_IN,
            false,
            Pack.Position.TOP);
    }

    public static ResourceLocation id(String id) {
        return ResourceLocation.fromNamespaceAndPath(MODID, id);
    }
}
