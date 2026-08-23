package com.raishxn.gtna.common.item;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import com.raishxn.gtna.common.data.NexusEnergyNetwork;
import com.raishxn.gtna.utils.datastructure.Int128;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side model for the quantum network terminal.
 *
 * <p>The original terminal was an LDLib 1 UI. GT 8 uses ModularUI 2, so the
 * widget is now built by {@link QuantumNetworkTerminalBehavior}; this class
 * deliberately contains only the status calculation shared by that UI.</p>
 */
public final class QuantumTerminalUI {

    private QuantumTerminalUI() {}

    public static Component createStatus(ItemStack stack, Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return Component.translatable("gtna.terminal.quantum.loading");
        }

        UUID owner = getNetworkOwner(stack, player);
        NexusEnergyNetwork network = NexusEnergyNetwork.get(level);
        Int128 energy = network.getEnergy(owner);
        Int128 maxCapacity = network.getMaxCapacity(owner);
        Int128 inPerTick = network.getLastInputPerTick(owner);
        Int128 outPerTick = network.getLastOutputPerTick(owner);
        boolean safeMode = network.getSafeMode(owner);

        boolean matrixFormed = network.isMatrixFormed(owner);
        long totalCapacitors = network.getTotalCapacitors(owner);
        int averageTier = network.getAverageTier(owner);
        double efficiency = network.getEfficiency(owner);
        Int128 transferLimit = network.getTransferLimit(owner);

        double fillPercentage = 0.0;
        if (!maxCapacity.isZero()) {
            if (maxCapacity.compareTo(Int128.fromBigInteger(BigInteger.valueOf(100_000L))) < 0) {
                fillPercentage = (double) energy.toLong() / maxCapacity.toLong();
            } else {
                fillPercentage = energy.toBigInteger().doubleValue() / maxCapacity.toBigInteger().doubleValue();
            }
        }
        fillPercentage = Math.max(0.0, Math.min(1.0, fillPercentage));

        int barLength = 24;
        int filled = (int) Math.round(fillPercentage * barLength);
        StringBuilder bar = new StringBuilder(80).append("§b[");
        for (int i = 0; i < barLength; i++) {
            bar.append(i < filled ? "§a█" : "§8▒");
        }
        bar.append("§b]");

        Component tierName = averageTier >= 0 && averageTier < GTValues.VN.length ?
                Component.literal(GTValues.VN[averageTier]) :
                Component.translatable("gtna.terminal.quantum.not_available");
        boolean crossDimensional = averageTier >= GTValues.ZPM;

        MutableComponent text = Component.empty();
        line(text, Component.translatable("gtna.terminal.quantum.title"));
        line(text, Component.literal("§8═══════════════════════════════"));
        line(text, Component.translatable("gtna.terminal.quantum.network",
                player.getGameProfile().getName()));
        line(text, Component.translatable("gtna.terminal.quantum.status",
                Component.translatable(safeMode ? "gtna.terminal.quantum.safe_mode" :
                        "gtna.terminal.quantum.online")));
        line(text, Component.translatable("gtna.terminal.quantum.matrix",
                Component.translatable(matrixFormed ? "gtna.terminal.quantum.formed" :
                        "gtna.terminal.quantum.not_formed")));
        line(text, Component.literal("§8───────────────────────────────"));
        line(text, Component.translatable("gtna.terminal.quantum.matrix_stats"));
        line(text, Component.translatable("gtna.terminal.quantum.capacitors", totalCapacitors));
        line(text, Component.translatable("gtna.terminal.quantum.max_capacity",
                maxCapacity.toHumanReadableString()));
        line(text, Component.translatable("gtna.terminal.quantum.average_tier", tierName, averageTier));
        line(text, Component.translatable("gtna.terminal.quantum.efficiency",
                String.format(Locale.US, "%.1f", efficiency * 100.0)));
        line(text, Component.translatable("gtna.terminal.quantum.transfer_limit",
                transferLimit.toHumanReadableString()));
        line(text, Component.translatable("gtna.terminal.quantum.cross_dimension",
                Component.translatable(crossDimensional ? "gtna.terminal.quantum.cross_dimension_enabled" :
                        "gtna.terminal.quantum.cross_dimension_required")));
        line(text, Component.literal("§8───────────────────────────────"));
        line(text, Component.translatable("gtna.terminal.quantum.energy_header"));
        line(text, Component.translatable("gtna.terminal.quantum.fill", Component.literal(bar.toString()),
                String.format(Locale.US, "%.1f", fillPercentage * 100.0)));
        line(text, Component.translatable("gtna.terminal.quantum.energy",
                energy.toHumanReadableString(), maxCapacity.toHumanReadableString()));
        line(text, Component.translatable("gtna.terminal.quantum.average_input",
                inPerTick.toHumanReadableString()));
        line(text, Component.translatable("gtna.terminal.quantum.average_output",
                outPerTick.toHumanReadableString()));
        line(text, Component.translatable("gtna.terminal.quantum.time_to_empty",
                calculateTimeToEmpty(energy, inPerTick, outPerTick)));
        line(text, Component.literal("§8───────────────────────────────"));

        Map<GlobalPos, NexusEnergyNetwork.ConnectionInfo> connections = network.getConnections(owner);
        line(text, Component.translatable("gtna.terminal.quantum.connections", connections.size()));
        for (NexusEnergyNetwork.ConnectionInfo info : connections.values()) {
            Component direction = Component.translatable(info.isInput ?
                    "gtna.terminal.quantum.direction_in" : "gtna.terminal.quantum.direction_out");
            String connectionTier = GTValues.VN[Math.max(0, Math.min(info.tier, GTValues.VN.length - 1))];
            String amount = (info.isInput ? "+" : "-") + info.lastTickEuTransferred.toHumanReadableString();
            Component machineType = info.machineType.startsWith("gtna.") ?
                    Component.translatable(info.machineType) : Component.literal(info.machineType);
            line(text, Component.translatable("gtna.terminal.quantum.connection", direction, info.amperage,
                    connectionTier, machineType, amount));
            line(text, Component.translatable("gtna.terminal.quantum.position",
                    info.pos.pos().toShortString(), info.pos.dimension().location()));
        }
        if (connections.isEmpty()) {
            line(text, Component.translatable("gtna.terminal.quantum.no_connections"));
        }
        return text;
    }

    private static UUID getNetworkOwner(ItemStack stack, Player player) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var tag = data.copyTag();
        return tag.hasUUID("NetworkID") ? tag.getUUID("NetworkID") : player.getUUID();
    }

    private static void line(MutableComponent builder, Component line) {
        if (!builder.getSiblings().isEmpty()) builder.append("\n");
        builder.append(line);
    }

    private static Component calculateTimeToEmpty(Int128 energy, Int128 inPerTick, Int128 outPerTick) {
        if (energy.isZero()) return Component.translatable("gtna.terminal.quantum.duration.empty");

        Int128 netDrain = outPerTick.copy();
        if (netDrain.compareTo(inPerTick) <= 0) {
            return Component.translatable("gtna.terminal.quantum.duration.charging");
        }
        netDrain.subtract(inPerTick);
        if (netDrain.isZero() || netDrain.isNegative()) {
            return Component.translatable("gtna.terminal.quantum.duration.charging");
        }

        BigInteger ticks = energy.toBigInteger().divide(netDrain.toBigInteger());
        return formatTickDuration(ticks.min(BigInteger.valueOf(Long.MAX_VALUE)).longValue());
    }

    private static Component formatTickDuration(long ticks) {
        if (ticks <= 0) return Component.translatable("gtna.terminal.quantum.duration.zero");
        long totalSeconds = ticks / 20;
        long hours = totalSeconds / 3_600;
        long minutes = totalSeconds % 3_600 / 60;
        long seconds = totalSeconds % 60;

        if (hours > 99_999) return Component.translatable("gtna.terminal.quantum.duration.overflow");
        if (hours > 0) return Component.translatable("gtna.terminal.quantum.duration.hms",
                hours, String.format(Locale.US, "%02d", minutes), String.format(Locale.US, "%02d", seconds));
        if (minutes > 0) return Component.translatable("gtna.terminal.quantum.duration.ms",
                minutes, String.format(Locale.US, "%02d", seconds));
        return Component.translatable("gtna.terminal.quantum.duration.seconds", seconds);
    }
}
