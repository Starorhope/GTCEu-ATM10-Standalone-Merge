package com.raishxn.gtna.common.machine.multiblock.energy;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.raishxn.gtna.common.data.GTNABlocks;
import com.raishxn.gtna.common.machine.multiblock.part.ae.GTNACraftingCPUInterfacePartMachine;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import com.raishxn.gtna.utils.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.List;
import java.util.Locale;

public class NexusMEHyperCoreMachine extends WorkableMultiblockMachine implements IMuiMachine {

    public static final int TOTAL_MODULE_SLOTS = 481;

    private static final long MODULE_I_STORAGE = 4L * 1024L * 1024L;
    private static final long MODULE_II_STORAGE = 64L * 1024L * 1024L;
    private static final long MODULE_III_STORAGE = 1024L * 1024L * 1024L;
    private static final long MODULE_IV_STORAGE = 16L * 1024L * 1024L * 1024L;

    private static final long MODULE_I_COPROCESSORS = 64L;
    private static final long MODULE_II_COPROCESSORS = 4096L;
    private static final long MODULE_III_COPROCESSORS = 262_144L;
    private static final long MODULE_IV_COPROCESSORS = 16_777_216L;

    private static final long MODULE_I_THREADS = 4L;
    private static final long MODULE_II_THREADS = 16L;
    private static final long MODULE_III_THREADS = 128L;
    private static final long MODULE_IV_THREADS = 1024L;

    private long installedModules;
    private long matrixI;
    private long matrixII;
    private long matrixIII;
    private long matrixIV;
    private long totalStorageBytes;
    private long totalCoProcessors;
    private long totalThreads;
    private int highestModuleTier;
    private boolean transcendentMode;
    private TickableSubscription interfaceSyncSubscription;
    private int interfaceSyncTicks;

    public NexusMEHyperCoreMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        scheduleInterfaceSync();
    }

    @Override
    public void onUnload() {
        if (interfaceSyncSubscription != null) {
            interfaceSyncSubscription.unsubscribe();
            interfaceSyncSubscription = null;
        }
        super.onUnload();
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        recalculateInternalModules();
        configureCraftingCpuInterfaces();
        scheduleInterfaceSync();
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        resetStats();
        configureCraftingCpuInterfaces();
    }

    private void resetStats() {
        installedModules = 0L;
        matrixI = 0L;
        matrixII = 0L;
        matrixIII = 0L;
        matrixIV = 0L;
        totalStorageBytes = 0L;
        totalCoProcessors = 0L;
        totalThreads = 0L;
        highestModuleTier = 0;
        transcendentMode = false;
    }

    private void recalculateInternalModules() {
        resetStats();
        if (getLevel() == null || !hasStructureCache()) {
            return;
        }

        for (long packedPos : getDefaultPatternState().getCache().keySet()) {
            BlockPos pos = BlockPos.of(packedPos);
            int moduleTier = getModuleTier(getLevel().getBlockState(pos).getBlock());
            if (moduleTier <= 0) {
                continue;
            }

            installedModules++;
            highestModuleTier = Math.max(highestModuleTier, moduleTier);
            switch (moduleTier) {
                case 1 -> addModule(1, MODULE_I_STORAGE, MODULE_I_COPROCESSORS, MODULE_I_THREADS);
                case 2 -> addModule(2, MODULE_II_STORAGE, MODULE_II_COPROCESSORS, MODULE_II_THREADS);
                case 3 -> addModule(3, MODULE_III_STORAGE, MODULE_III_COPROCESSORS, MODULE_III_THREADS);
                case 4 -> addModule(4, MODULE_IV_STORAGE, MODULE_IV_COPROCESSORS, MODULE_IV_THREADS);
                default -> {}
            }
        }

        transcendentMode = matrixIV == TOTAL_MODULE_SLOTS;
    }

    private void addModule(int tier, long storage, long coProcessors, long threads) {
        switch (tier) {
            case 1 -> matrixI++;
            case 2 -> matrixII++;
            case 3 -> matrixIII++;
            case 4 -> matrixIV++;
            default -> {}
        }
        totalStorageBytes += storage;
        totalCoProcessors += coProcessors;
        totalThreads += threads;
    }

    private int getModuleTier(Block block) {
        if (block == GTNABlocks.T1_CRAFTING_STORAGE_CORE.get()) return 1;
        if (block == GTNABlocks.T2_CRAFTING_STORAGE_CORE.get()) return 2;
        if (block == GTNABlocks.T3_CRAFTING_STORAGE_CORE.get()) return 3;
        if (block == GTNABlocks.T4_CRAFTING_STORAGE_CORE.get()) return 4;
        if (block == GTNABlocks.T5_CRAFTING_STORAGE_CORE.get()) return 4;
        if (block == Registries.getBlock("ae2:crafting_unit")) return 1;
        return 0;
    }

    public long getAeStorageBytes() {
        if (transcendentMode) {
            return Long.MAX_VALUE;
        }
        return totalStorageBytes;
    }

    public long getAeCoProcessors() {
        if (transcendentMode) {
            return Integer.MAX_VALUE;
        }
        return totalCoProcessors;
    }

    private void configureCraftingCpuInterfaces() {
        if (getLevel() == null || isRemote()) {
            return;
        }
        long storage = isFormed() ? getAeStorageBytes() : 0L;
        long coProcessors = isFormed() ? getAeCoProcessors() : 0L;
        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof GTNACraftingCPUInterfacePartMachine cpuInterface) {
                cpuInterface.configureCpu(storage, coProcessors);
            }
        }
    }

    private void scheduleInterfaceSync() {
        if (isRemote() || interfaceSyncSubscription != null && interfaceSyncSubscription.isStillSubscribed()) {
            return;
        }
        interfaceSyncTicks = 0;
        interfaceSyncSubscription = subscribeServerTick(this::tickInterfaceSync);
    }

    private void tickInterfaceSync() {
        interfaceSyncTicks++;
        boolean foundCpuInterface = false;
        if (isFormed() && hasStructureCache()) {
            recalculateInternalModules();
            configureCraftingCpuInterfaces();
            for (MultiblockPartMachine part : getParts()) {
                if (part instanceof GTNACraftingCPUInterfacePartMachine) {
                    foundCpuInterface = true;
                    break;
                }
            }
        }

        if (foundCpuInterface || interfaceSyncTicks >= 100) {
            if (interfaceSyncSubscription != null) {
                interfaceSyncSubscription.unsubscribe();
                interfaceSyncSubscription = null;
            }
        }
    }

    private boolean hasStructureCache() {
        PatternState state = getDefaultPatternState();
        return state != null && !state.getCache().isEmpty();
    }

    private int getComputationTier() {
        return switch (highestModuleTier) {
            case 1 -> GTValues.EV;
            case 2 -> GTValues.LuV;
            case 3 -> GTValues.UV;
            case 4 -> GTValues.UEV;
            default -> GTValues.ULV;
        };
    }

    private Component getComputationTierName() {
        if (transcendentMode) {
            return Component.translatable("gtna.machine.nexus_me_hypercore.ui.tier_transcendent");
        }
        if (highestModuleTier <= 0) {
            return Component.translatable("gtna.machine.nexus_me_hypercore.ui.offline");
        }
        return Component.literal(GTValues.VN[getComputationTier()]);
    }

    private String formatBytes(long bytes) {
        if (transcendentMode) {
            return "∞";
        }
        if (bytes <= 0L) {
            return "0B";
        }
        String[] units = { "B", "K", "M", "G", "T", "P", "E" };
        double value = bytes;
        int unitIndex = 0;
        while (value >= 1024.0D && unitIndex < units.length - 1) {
            value /= 1024.0D;
            unitIndex++;
        }
        if (value >= 100.0D || Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.format(Locale.US, "%.0f%s", value, units[unitIndex]);
        }
        if (value >= 10.0D) {
            return String.format(Locale.US, "%.1f%s", value, units[unitIndex]);
        }
        return String.format(Locale.US, "%.2f%s", value, units[unitIndex]);
    }

    private String formatStat(long value) {
        if (transcendentMode) {
            return "∞";
        }
        if (value < 1_000L) {
            return Long.toString(value);
        }
        if (value < 1_000_000L) {
            return String.format(Locale.US, "%.2fK", value / 1_000.0D);
        }
        if (value < 1_000_000_000L) {
            return String.format(Locale.US, "%.2fM", value / 1_000_000.0D);
        }
        return String.format(Locale.US, "%.2fB", value / 1_000_000_000.0D);
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_me_hypercore", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (!isFormed()) {
            return;
        }

        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.title")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        textList.add(Component.literal("----------------------").withStyle(ChatFormatting.DARK_GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.tier")
                .append(getComputationTierName().copy().withStyle(ChatFormatting.GOLD)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.modules")
                .append(Component.literal(installedModules + "/" + TOTAL_MODULE_SLOTS).withStyle(ChatFormatting.GREEN)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.storage")
                .append(Component.literal(formatBytes(totalStorageBytes)).withStyle(ChatFormatting.YELLOW)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.coprocessors")
                .append(Component.literal(formatStat(totalCoProcessors)).withStyle(ChatFormatting.LIGHT_PURPLE)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.threads")
                .append(Component.literal(formatStat(totalThreads)).withStyle(ChatFormatting.AQUA)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.transcendent")
                .append(Component.translatable(transcendentMode
                        ? "gtna.machine.nexus_me_hypercore.ui.on"
                        : "gtna.machine.nexus_me_hypercore.ui.off")
                        .withStyle(transcendentMode ? ChatFormatting.RED : ChatFormatting.GRAY)));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.matrix_i_ii", matrixI, matrixII));
        textList.add(Component.translatable("gtna.machine.nexus_me_hypercore.ui.matrix_iii_iv", matrixIII, matrixIV));
    }
}
