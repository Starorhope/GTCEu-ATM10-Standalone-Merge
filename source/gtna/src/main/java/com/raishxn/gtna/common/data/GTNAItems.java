package com.raishxn.gtna.common.data;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.common.item.behavior.CoverPlaceBehavior;
import com.gregtechceu.gtceu.common.item.behavior.TooltipBehavior;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.item.CoordinateCardBehavior;
import com.raishxn.gtna.common.item.PatternBufferUpgraderBehavior;
import com.raishxn.gtna.common.item.RealityRipperSwordItem;
import com.raishxn.gtna.common.item.StructureDetectBehavior;
import com.raishxn.gtna.common.item.TesseractTargetMarkerBehavior;
import com.raishxn.gtna.common.item.armor.QuantumCosmicNexusArmorItem;
import com.tterrag.registrate.util.entry.ItemEntry;

import static com.gregtechceu.gtceu.common.data.GTItems.attach;
import static com.raishxn.gtna.api.registry.GTNARegistry.REGISTRATE;

public class GTNAItems {

    private static final String[] INDUSTRIAL_COMPONENT_GROUPS = { "standard", "extended", "special" };
    private static final String[] INDUSTRIAL_COMPONENT_GROUP_NAMES = { "Standard", "Extended", "Special" };
    private static final String[] INDUSTRIAL_COMPONENT_SIZES = { "small", "medium", "large" };
    private static final String[] INDUSTRIAL_COMPONENT_SIZE_NAMES = { "Small", "Medium", "Large" };

    static {
        REGISTRATE.creativeModeTab(() -> GTNACreativeModeTabs.ITEMS);
    }

    // Ferramentas e Itens de Debug Existentes
    public static ItemEntry<ComponentItem> DEBUG_STRUCTURE_WRITER;
    public static ItemEntry<ComponentItem> STRUCTURE_DETECT;
    public static ItemEntry<ComponentItem> COORDINATE_CARD;
    public static ItemEntry<ComponentItem> TESSERACT_TARGET_MARKER;

    // --- NOVOS COMPONENTES HIDRÁULICOS ---
    public static ItemEntry<ComponentItem> HYDRAULIC_MOTOR;
    public static ItemEntry<ComponentItem> HYDRAULIC_PISTON;
    public static ItemEntry<ComponentItem> HYDRAULIC_PUMP;
    public static ItemEntry<ComponentItem> HYDRAULIC_ARM;
    public static ItemEntry<ComponentItem> HYDRAULIC_CONVEYOR;
    public static ItemEntry<ComponentItem> HYDRAULIC_REGULATOR;
    public static ItemEntry<ComponentItem> HYDRAULIC_VAPOR_GENERATOR;
    public static ItemEntry<ComponentItem> HYDRAULIC_STEAM_JET_SPEWER;
    public static ItemEntry<ComponentItem> HYDRAULIC_STEAM_RECEIVER;
    public static ItemEntry<ComponentItem> PRECISION_STEAM_COMPONENT;
    public static ItemEntry<ComponentItem> PRIMITIVE_MANS_SPACETIME_DISTORTION_DEVICE;
    public static ItemEntry<ComponentItem> UEV_CIRCUIT;
    public static ItemEntry<ComponentItem> UIV_CIRCUIT;
    public static ItemEntry<ComponentItem> UXV_CIRCUIT;
    public static ItemEntry<ComponentItem> OPV_CIRCUIT;
    public static ItemEntry<ComponentItem> MAX_CIRCUIT;
    @SuppressWarnings("unchecked")
    public static ItemEntry<ComponentItem>[][] INDUSTRIAL_COMPONENTS = new ItemEntry[INDUSTRIAL_COMPONENT_GROUPS.length][INDUSTRIAL_COMPONENT_SIZES.length];

    public static ItemEntry<com.raishxn.gtna.common.item.NexusLinkerItem> NEXUS_LINKER;
    public static ItemEntry<QuantumCosmicNexusArmorItem> QUANTUM_COSMIC_NEXUS_HELMET;
    public static ItemEntry<QuantumCosmicNexusArmorItem> QUANTUM_COSMIC_NEXUS_CHESTPLATE;
    public static ItemEntry<QuantumCosmicNexusArmorItem> QUANTUM_COSMIC_NEXUS_LEGGINGS;
    public static ItemEntry<QuantumCosmicNexusArmorItem> QUANTUM_COSMIC_NEXUS_BOOTS;
    public static ItemEntry<ComponentItem> QUANTUM_NETWORK_TERMINAL;
    public static ItemEntry<ComponentItem> NEXUS_STRUCTURE_TERMINAL;
    public static ItemEntry<ComponentItem> PATTERN_BUFFER_UPGRADE_21;
    public static ItemEntry<ComponentItem> PATTERN_BUFFER_UPGRADE_32;
    public static ItemEntry<ComponentItem> PATTERN_BUFFER_UPGRADE_72;
    public static ItemEntry<ComponentItem> INFINITE_CELL_COMPONENT;
    public static ItemEntry<ComponentItem> ANNIHILATION_CONSTRAINER;
    public static ItemEntry<ComponentItem> NEUTRONIUM_ANTIMATTER_FUEL_ROD;
    public static ItemEntry<ComponentItem> DRACONIUM_ANTIMATTER_FUEL_ROD;
    public static ItemEntry<ComponentItem> COSMIC_NEUTRONIUM_ANTIMATTER_FUEL_ROD;
    public static ItemEntry<ComponentItem> INFINITY_ANTIMATTER_FUEL_ROD;
    public static ItemEntry<RealityRipperSwordItem> REALITY_RIPPER_SWORD;
    public static ItemEntry<ComponentItem> INFINITE_STEAM_SINGLEBLOCK_COVER;
    public static ItemEntry<ComponentItem> INFINITE_ELECTRIC_SINGLEBLOCK_COVER;

    public static void init() {
        STRUCTURE_DETECT = REGISTRATE
                .item("structure_detect", ComponentItem::new)
                .lang("Structure Detector")
                .properties(stack -> stack.stacksTo(1))
                .onRegister(attach(StructureDetectBehavior.INSTANCE))
                .model((ctx, provider) -> {
                    provider.generated(ctx, ResourceLocation.fromNamespaceAndPath("gtceu", "item/portable_scanner"));
                })
                .register();

        COORDINATE_CARD = REGISTRATE
                .item("coordinate_card", ComponentItem::new)
                .lang("Coordinate Card")
                .properties(stack -> stack.stacksTo(1))
                .onRegister(attach(CoordinateCardBehavior.INSTANCE))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/coordinate_card")))
                .register();

        TESSERACT_TARGET_MARKER = REGISTRATE
                .item("tesseract_target_marker", ComponentItem::new)
                .lang("Tesseract Target Marker")
                .properties(stack -> stack.stacksTo(1))
                .onRegister(attach(TesseractTargetMarkerBehavior.INSTANCE))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/tesseract_target_marker")))
                .register();

        HYDRAULIC_MOTOR = REGISTRATE.item("hydraulic_motor", ComponentItem::new)
                .properties(stack -> stack.stacksTo(64))
                .lang("Hydraulic Motor")
                .register();
        HYDRAULIC_PISTON = REGISTRATE.item("hydraulic_piston", ComponentItem::new)
                .lang("Hydraulic Piston")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_PUMP = REGISTRATE.item("hydraulic_pump", ComponentItem::new)
                .lang("Hydraulic Pump")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_ARM = REGISTRATE.item("hydraulic_arm", ComponentItem::new)
                .lang("Hydraulic Arm")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_CONVEYOR = REGISTRATE.item("hydraulic_conveyor", ComponentItem::new)
                .lang("Hydraulic Conveyor")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_REGULATOR = REGISTRATE.item("hydraulic_regulator", ComponentItem::new)
                .lang("Hydraulic Regulator")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_VAPOR_GENERATOR = REGISTRATE.item("hydraulic_vapor_generator", ComponentItem::new)
                .lang("Hydraulic Vapor Generator")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_STEAM_JET_SPEWER = REGISTRATE.item("hydraulic_steam_jet_spewer", ComponentItem::new)
                .lang("Hydraulic Steam Jet Spewer")
                .properties(stack -> stack.stacksTo(64))
                .register();
        HYDRAULIC_STEAM_RECEIVER = REGISTRATE.item("hydraulic_steam_receiver", ComponentItem::new)
                .lang("Hydraulic Steam Receiver")
                .properties(stack -> stack.stacksTo(64))
                .register();
        PRECISION_STEAM_COMPONENT = REGISTRATE.item("precision_steam_component", ComponentItem::new)
                .lang("Precision Steam Component")
                .properties(stack -> stack.stacksTo(64))
                .register();
        PRIMITIVE_MANS_SPACETIME_DISTORTION_DEVICE = REGISTRATE
                .item("primitive_mans_spacetime_distortion_device", ComponentItem::new)
                .lang("Primitive Man's SpaceTime Distortion Device")
                .properties(stack -> stack.stacksTo(64))
                .onRegister(attach(new TooltipBehavior(lines -> lines.add(
                        Component.translatable("item.gtna.primitive_mans_spacetime_distortion_device.tooltip")
                                .withStyle(ChatFormatting.GRAY)))))
                .model((ctx, provider) -> provider.generated(ctx,
                        GTNACORE.id("item/primitive_mans_spacetime_distortion_device")))
                .register();

        UEV_CIRCUIT = registerHighTierCircuit("uev_circuit", "UEV Circuit", "wetware_processor_mainframe");
        UIV_CIRCUIT = registerHighTierCircuit("uiv_circuit", "UIV Circuit", "crystal_processor_mainframe");
        UXV_CIRCUIT = registerHighTierCircuit("uxv_circuit", "UXV Circuit", "quantum_processor_mainframe");
        OPV_CIRCUIT = registerHighTierCircuit("opv_circuit", "OpV Circuit", "nano_processor_mainframe");
        MAX_CIRCUIT = registerHighTierCircuit("max_circuit", "MAX Circuit", "micro_processor_mainframe");
        registerIndustrialComponents();

        NEXUS_LINKER = REGISTRATE.item("nexus_linker", com.raishxn.gtna.common.item.NexusLinkerItem::new)
                .lang("Nexus Linker")
                .properties(stack -> stack.stacksTo(1))
                .register();

        QUANTUM_COSMIC_NEXUS_HELMET = REGISTRATE.item("quantum_cosmic_nexus_helmet",
                props -> new QuantumCosmicNexusArmorItem(net.minecraft.world.item.ArmorItem.Type.HELMET, props))
                .lang("Quantum Cosmic Nexus Helmet")
                .properties(stack -> stack.stacksTo(1))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/" + ctx.getName())))
                .register();

        QUANTUM_COSMIC_NEXUS_CHESTPLATE = REGISTRATE.item("quantum_cosmic_nexus_chestplate",
                props -> new QuantumCosmicNexusArmorItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, props))
                .lang("Quantum Cosmic Nexus Chestplate")
                .properties(stack -> stack.stacksTo(1))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/" + ctx.getName())))
                .register();

        QUANTUM_COSMIC_NEXUS_LEGGINGS = REGISTRATE.item("quantum_cosmic_nexus_leggings",
                props -> new QuantumCosmicNexusArmorItem(net.minecraft.world.item.ArmorItem.Type.LEGGINGS, props))
                .lang("Quantum Cosmic Nexus Leggings")
                .properties(stack -> stack.stacksTo(1))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/" + ctx.getName())))
                .register();

        QUANTUM_COSMIC_NEXUS_BOOTS = REGISTRATE.item("quantum_cosmic_nexus_boots",
                props -> new QuantumCosmicNexusArmorItem(net.minecraft.world.item.ArmorItem.Type.BOOTS, props))
                .lang("Quantum Cosmic Nexus Boots")
                .properties(stack -> stack.stacksTo(1))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/" + ctx.getName())))
                .register();

        QUANTUM_NETWORK_TERMINAL = REGISTRATE.item("quantum_network_terminal", ComponentItem::new)
                .lang("Quantum Network Terminal")
                .properties(stack -> stack.stacksTo(1))
                .onRegister(attach(com.raishxn.gtna.common.item.QuantumNetworkTerminalBehavior.INSTANCE))
                .register();

        NEXUS_STRUCTURE_TERMINAL = REGISTRATE.item("nexus_structure_terminal", ComponentItem::new)
                .lang("Nexus Structure Terminal")
                .properties(stack -> stack.stacksTo(1))
                .onRegister(attach(com.raishxn.gtna.common.item.terminal.NexusTerminalBehavior.INSTANCE))
                .register();

        PATTERN_BUFFER_UPGRADE_21 = REGISTRATE.item("pattern_buffer_upgrade_21", ComponentItem::new)
                .lang("Pattern Buffer Expansion Card")
                .properties(stack -> stack.stacksTo(16))
                .onRegister(attach(new PatternBufferUpgraderBehavior(() -> GTNAMachines2.ME_PATTERN_BUFFER)))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/pattern_buffer_upgrader")))
                .register();

        PATTERN_BUFFER_UPGRADE_32 = REGISTRATE.item("pattern_buffer_upgrade_32", ComponentItem::new)
                .lang("Pattern Buffer Precision Card")
                .properties(stack -> stack.stacksTo(16))
                .onRegister(attach(new PatternBufferUpgraderBehavior(() -> GTNAMachines2.ME_ADVANCED_PATTERN_BUFFER)))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/ex_pattern_buffer_upgrader")))
                .register();

        PATTERN_BUFFER_UPGRADE_72 = REGISTRATE.item("pattern_buffer_upgrade_72", ComponentItem::new)
                .lang("Pattern Buffer Ascension Card")
                .properties(stack -> stack.stacksTo(16))
                .onRegister(attach(new PatternBufferUpgraderBehavior(() -> GTNAMachines2.ME_ULTIMATE_PATTERN_BUFFER)))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/ex_pattern_buffer_ultra_upgrader")))
                .register();

        INFINITE_CELL_COMPONENT = REGISTRATE.item("infinite_cell_component", ComponentItem::new)
                .lang("Infinite Cell Component")
                .properties(stack -> stack.stacksTo(64))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/infinite_cell_component")))
                .register();

        ANNIHILATION_CONSTRAINER = REGISTRATE.item("annihilation_constrainer", ComponentItem::new)
                .lang("Annihilation Constrainer")
                .properties(stack -> stack.stacksTo(64))
                .register();

        NEUTRONIUM_ANTIMATTER_FUEL_ROD = REGISTRATE.item("neutronium_antimatter_fuel_rod", ComponentItem::new)
                .lang("Neutronium Antimatter Fuel Rod")
                .properties(stack -> stack.stacksTo(64))
                .register();

        DRACONIUM_ANTIMATTER_FUEL_ROD = REGISTRATE.item("draconium_antimatter_fuel_rod", ComponentItem::new)
                .lang("Draconium Antimatter Fuel Rod")
                .properties(stack -> stack.stacksTo(64))
                .register();

        COSMIC_NEUTRONIUM_ANTIMATTER_FUEL_ROD = REGISTRATE
                .item("cosmic_neutronium_antimatter_fuel_rod", ComponentItem::new)
                .lang("Cosmic Neutronium Antimatter Fuel Rod")
                .properties(stack -> stack.stacksTo(64))
                .register();

        INFINITY_ANTIMATTER_FUEL_ROD = REGISTRATE.item("infinity_antimatter_fuel_rod", ComponentItem::new)
                .lang("Infinity Antimatter Fuel Rod")
                .properties(stack -> stack.stacksTo(64))
                .register();

        REALITY_RIPPER_SWORD = REGISTRATE.item("reality_ripper_sword", RealityRipperSwordItem::new)
                .lang("Reality Ripper")
                .properties(stack -> stack.stacksTo(1))
                .model((ctx, provider) -> provider.generated(ctx, GTNACORE.id("item/" + ctx.getName())))
                .register();

        INFINITE_STEAM_SINGLEBLOCK_COVER = REGISTRATE.item("infinite_steam_singleblock_cover", ComponentItem::new)
                .lang("Infinite Steam Singleblock Cover")
                .properties(stack -> stack.stacksTo(64))
                .onRegister(attach(new CoverPlaceBehavior(GTNACovers.INFINITE_STEAM_SINGLEBLOCK_COVER)))
                .model((ctx, provider) -> provider.generated(ctx,
                        GTNACORE.id("item/734")))
                .register();

        INFINITE_ELECTRIC_SINGLEBLOCK_COVER = REGISTRATE.item("infinite_electric_singleblock_cover",
                ComponentItem::new)
                .lang("Infinite Electric Singleblock Cover")
                .properties(stack -> stack.stacksTo(64))
                .onRegister(attach(new CoverPlaceBehavior(GTNACovers.INFINITE_ELECTRIC_SINGLEBLOCK_COVER)))
                .model((ctx, provider) -> provider.generated(ctx,
                        GTNACORE.id("item/733")))
                .register();
    }

    private static ItemEntry<ComponentItem> registerHighTierCircuit(String id, String name, String texture) {
        return REGISTRATE.item(id, ComponentItem::new)
                .lang(name)
                .properties(stack -> stack.stacksTo(64))
                .model((ctx, provider) -> provider.generated(ctx,
                        ResourceLocation.fromNamespaceAndPath("gtceu", "item/" + texture)))
                .register();
    }

    private static void registerIndustrialComponents() {
        for (int group = 0; group < INDUSTRIAL_COMPONENT_GROUPS.length; group++) {
            for (int size = 0; size < INDUSTRIAL_COMPONENT_SIZES.length; size++) {
                String id = INDUSTRIAL_COMPONENT_GROUPS[group] + "_industrial_components_" + INDUSTRIAL_COMPONENT_SIZES[size];
                String lang = INDUSTRIAL_COMPONENT_GROUP_NAMES[group] + " Industrial Components (" +
                        INDUSTRIAL_COMPONENT_SIZE_NAMES[size] + ")";
                String sizeKey = INDUSTRIAL_COMPONENT_SIZES[size];
                INDUSTRIAL_COMPONENTS[group][size] = REGISTRATE.item(id, ComponentItem::new)
                        .lang(lang)
                        .properties(stack -> stack.stacksTo(64))
                        .model((ctx, provider) -> provider.generated(ctx,
                                GTNACORE.id("item/industrial_components_" + sizeKey + "_0"),
                                GTNACORE.id("item/industrial_components_" + sizeKey + "_1")))
                        .register();
            }
        }
    }
}
