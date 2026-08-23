package com.raishxn.gtna.common.machine.multiblock.energy;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import com.raishxn.gtna.common.block.NexusCapacitorBlock;
import com.raishxn.gtna.common.data.NexusEnergyNetwork;
import com.raishxn.gtna.config.ConfigHolder;
import com.raishxn.gtna.config.GTNABalance;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import com.raishxn.gtna.utils.datastructure.Int128;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NexusFluxMatrixMachine extends WorkableMultiblockMachine implements IMuiMachine {

    private long totalCapacitors = 0;
    private Int128 sumCapacities = Int128.ZERO();
    private int sumTiers = 0;

    private Int128 maxCapacity = Int128.ZERO();
    private int averageTier = 1;
    private int maxTier = 1;
    private Int128 transferLimit = Int128.ZERO();
    private double efficiency = 0.85;

    private final List<NexusEnergyNetwork.ConnectionInfo> cachedConnections = new ArrayList<>();

    public NexusFluxMatrixMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        recalculateCapacitors();
        if (getLevel() instanceof ServerLevel serverLevel && getOwnerUUID() != null) {
            NexusEnergyNetwork network = NexusEnergyNetwork.get(serverLevel);
            network.setMaxCapacity(getOwnerUUID(), maxCapacity);
            network.setMatrixStats(getOwnerUUID(), totalCapacitors, averageTier, efficiency, transferLimit, true);
        }
    }

    @Override
    public void invalidateStructure(String substructureName) {
        if (getLevel() instanceof ServerLevel serverLevel && getOwnerUUID() != null) {
            NexusEnergyNetwork network = NexusEnergyNetwork.get(serverLevel);
            network.setMaxCapacity(getOwnerUUID(), Int128.ZERO());
            network.setMatrixStats(getOwnerUUID(), 0, 0, 0.0, Int128.ZERO(), false);
        }
        super.invalidateStructure(substructureName);
        totalCapacitors = 0;
        sumCapacities = Int128.ZERO();
        sumTiers = 0;
        maxCapacity = Int128.ZERO();
        averageTier = 1;
        maxTier = 1;
        transferLimit = Int128.ZERO();
        efficiency = 0.85;
    }

    private void recalculateCapacitors() {
        totalCapacitors = 0;
        sumCapacities = Int128.ZERO();
        sumTiers = 0;
        maxTier = 1;

        if (getLevel() != null) {
            BlockPos startPos = getBlockPos();
            for (int x = -16; x <= 16; x++) {
                for (int y = -16; y <= 35; y++) {
                    for (int z = -16; z <= 16; z++) {
                        BlockPos pos = startPos.offset(x, y, z);
                        BlockState blockState = getLevel().getBlockState(pos);
                        if (blockState.getBlock() instanceof NexusCapacitorBlock capacitor) {
                            totalCapacitors++;
                            long configuredCapacity = GTNABalance
                                    .getNexusCapacitorCapacity(capacitor.getTier(), capacitor.getUnitCapacity());
                            sumCapacities.add(new Int128(configuredCapacity));
                            sumTiers += capacitor.getTier();
                            if (capacitor.getTier() > maxTier) {
                                maxTier = capacitor.getTier();
                            }
                        }
                    }
                }
            }
        }

        if (totalCapacitors <= 0) {
            maxCapacity = Int128.ZERO();
            averageTier = 1;
            transferLimit = Int128.ZERO();
            efficiency = 0.85;
            return;
        }

        var machineCfg = ConfigHolder.INSTANCE.machines.nexusFluxMatrix;
        var balanceCfg = GTNABalance.getNexusFluxMatrix();

        averageTier = machineCfg.useHighestTierForEfficiency ? maxTier : (int) (sumTiers / totalCapacitors);
        if (averageTier < 1) averageTier = 1;

        maxCapacity = sumCapacities.copy();

        double tierRatio = (averageTier - 1) / 13.0;
        double lossPercent = balanceCfg.efficiency.baseLossPercentAtLV * (1.0 - tierRatio);
        efficiency = 1.0 - (lossPercent / 100.0);
        efficiency = Math.max(balanceCfg.efficiency.minimumEfficiency,
                Math.min(balanceCfg.efficiency.maximumEfficiency, efficiency));

        long fallbackTransfer = 2000L * (long) Math.pow(4, Math.max(0, averageTier - 1));
        transferLimit = Int128.fromString(
                GTNABalance.getNexusTransferLimit(averageTier, Long.toString(fallbackTransfer)),
                new Int128(fallbackTransfer));
    }

    public Int128 getMaxCapacity() {
        return maxCapacity;
    }

    public int getAverageTier() {
        return averageTier;
    }

    public Int128 getTransferLimit() {
        return transferLimit;
    }

    public double getEfficiency() {
        return efficiency;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_flux_matrix", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (!isFormed()) return;

        textList.add(Component.translatable("block.gtna.nexus_flux_matrix")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        textList.add(Component.literal("--------------------------------").withStyle(ChatFormatting.DARK_GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.capacitors",
                Component.literal(String.valueOf(totalCapacitors)).withStyle(ChatFormatting.GREEN))
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.max_capacity",
                Component.literal(maxCapacity.toHumanReadableString()).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GRAY));

        String tierName = GTValues.VN[Math.min(averageTier, GTValues.VN.length - 1)];
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.average_tier",
                Component.literal(tierName).withStyle(ChatFormatting.YELLOW), averageTier)
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.efficiency",
                Component.literal(String.format(Locale.US, "%.1f", efficiency * 100))
                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.transfer_limit",
                Component.literal(transferLimit.toHumanReadableString()).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.GRAY));

        boolean crossDim = GTNABalance.isNexusCrossDimensionEnabled(averageTier);
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.cross_dimension",
                Component.translatable(crossDim ? "gtna.machine.nexus_flux_matrix.ui.enabled" :
                        "gtna.machine.nexus_flux_matrix.ui.requires_ev")
                        .withStyle(crossDim ? ChatFormatting.GREEN : ChatFormatting.RED))
                .withStyle(ChatFormatting.GRAY));

        if (getOwnerUUID() == null || !(getLevel() instanceof ServerLevel serverLevel)) return;

        textList.add(Component.literal("--------------------------------").withStyle(ChatFormatting.DARK_GRAY));

        NexusEnergyNetwork network = NexusEnergyNetwork.get(serverLevel);
        Int128 energy = network.getEnergy(getOwnerUUID());
        Int128 maxCap = maxCapacity;
        boolean safeMode = network.getSafeMode(getOwnerUUID());
        Int128 inPerTick = network.getLastInputPerTick(getOwnerUUID());
        Int128 outPerTick = network.getLastOutputPerTick(getOwnerUUID());

        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.status",
                Component.translatable(safeMode ? "gtna.machine.nexus_flux_matrix.ui.safe_mode" :
                        "gtna.machine.nexus_flux_matrix.ui.online")
                        .withStyle(safeMode ? ChatFormatting.RED : ChatFormatting.GREEN))
                .withStyle(ChatFormatting.GRAY));

        double fill = 0.0;
        if (!maxCap.isZero()) {
            try {
                fill = energy.toBigInteger().doubleValue() / maxCap.toBigInteger().doubleValue();
            } catch (Exception ignored) {
                fill = 0.0;
            }
        }

        int barLength = 20;
        int filledCount = (int) Math.round(fill * barLength);
        StringBuilder bar = new StringBuilder("\u00a7b[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filledCount ? "\u00a7a|" : "\u00a78|");
        }
        bar.append("\u00a7b]");

        textList.add(Component.literal(bar + " \u00a7f" + String.format(Locale.US, "%.1f%%", fill * 100.0)));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.energy",
                energy.toHumanReadableString(), maxCap.toHumanReadableString()).withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.input",
                inPerTick.toHumanReadableString()).withStyle(ChatFormatting.GREEN));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.output",
                outPerTick.toHumanReadableString()).withStyle(ChatFormatting.RED));

        Map<GlobalPos, NexusEnergyNetwork.ConnectionInfo> connections = network.getConnections(getOwnerUUID());
        cachedConnections.clear();
        cachedConnections.addAll(connections.values());

        textList.add(Component.literal("--------------------------------").withStyle(ChatFormatting.DARK_GRAY));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.connections", connections.size())
                .withStyle(ChatFormatting.YELLOW));
        textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.connection_coordinates")
                .withStyle(ChatFormatting.GRAY));

        for (NexusEnergyNetwork.ConnectionInfo info : cachedConnections) {
            String connectionTier = GTValues.VN[Math.min(info.tier, GTValues.VN.length - 1)];
            String amount = (info.isInput ? "+" : "-") + info.lastTickEuTransferred.toHumanReadableString();
            String pos = info.pos.pos().toShortString();
            String dimension = info.pos.dimension().location().toString();
            Component direction = Component.translatable(info.isInput ?
                    "gtna.machine.nexus_flux_matrix.ui.direction_in" :
                    "gtna.machine.nexus_flux_matrix.ui.direction_out")
                    .withStyle(info.isInput ? ChatFormatting.GREEN : ChatFormatting.RED);
            Component machineType = info.machineType != null && info.machineType.startsWith("gtna.") ?
                    Component.translatable(info.machineType) : Component.literal(String.valueOf(info.machineType));

            textList.add(Component.translatable("gtna.machine.nexus_flux_matrix.ui.connection",
                    direction, info.amperage, connectionTier, machineType,
                    Component.literal(amount).withStyle(ChatFormatting.GRAY))
                    .withStyle(style -> style
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.translatable("gtna.machine.nexus_flux_matrix.ui.connection_hover",
                                            pos, dimension)))
                            .withColor(ChatFormatting.WHITE))
                    .append(Component.translatable("gtna.machine.nexus_flux_matrix.ui.locate", pos)
                            .withStyle(ChatFormatting.AQUA)));
        }
    }
}
