package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.raishxn.gtna.common.data.GTNAMaterials;
import com.raishxn.gtna.common.machine.multiMachineBase.SteamMultiMachineBase;
import com.raishxn.gtna.config.GTNABalance;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.Nonnull;

public class VoidMinerSteamGateAged extends SteamMultiMachineBase {

    public VoidMinerSteamGateAged(BlockEntityCreationInfo holder, Object... args) {
        super(holder, false, args);
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof VoidMinerSteamGateAged voidMiner)) return ModifierFunction.NULL;

        long outputMult = 1;
        double timeFactor = 1.0;
        double energyFactor = 1.0;

        Fluid insanelySteam = GTNAMaterials.InsanelySupercriticalSteam.getFluid();
        Fluid superHeatedSteam = GTNAMaterials.SuperHeatedSteam.getFluid();
        Fluid denseSteam = GTNAMaterials.DenseSupercriticalSteam.getFluid();

        var denseCfg = GTNABalance.getVoidMinerDenseSteam();
        var superCfg = GTNABalance.getVoidMinerSuperHeatedSteam();
        var insaneCfg = GTNABalance.getVoidMinerInsanelySteam();

        boolean foundTier = false;

        for (MultiblockPartMachine part : voidMiner.getParts()) {
            IFluidHandler handler = part.self().getFluidHandlerCap(null, true);
            if (handler == null) continue;

            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fluidInTank = handler.getFluidInTank(i);
                if (fluidInTank.isEmpty()) continue;

                Fluid fluid = fluidInTank.getFluid();

                if (fluid.isSame(insanelySteam)) {
                    outputMult = insaneCfg.outputMultiplier;
                    timeFactor = 1.0 / insaneCfg.speedMultiplier;
                    energyFactor = insaneCfg.energyMultiplier;
                    foundTier = true;
                    break;
                } else if (fluid.isSame(superHeatedSteam)) {
                    if (outputMult < superCfg.outputMultiplier) {
                        outputMult = superCfg.outputMultiplier;
                        timeFactor = 1.0 / superCfg.speedMultiplier;
                        energyFactor = superCfg.energyMultiplier;
                    }
                } else if (fluid.isSame(denseSteam) && outputMult < denseCfg.outputMultiplier) {
                    outputMult = denseCfg.outputMultiplier;
                    timeFactor = 1.0 / denseCfg.speedMultiplier;
                    energyFactor = denseCfg.energyMultiplier;
                }
            }

            if (foundTier) break;
        }

        if (outputMult == 1) return ModifierFunction.IDENTITY;

        long finalOutputMult = outputMult;
        double finalTimeFactor = timeFactor;
        double finalEnergyFactor = energyFactor;
        return r -> ModifierFunction.builder()
                .outputModifier(ContentModifier.multiplier(finalOutputMult))
                .durationMultiplier(finalTimeFactor)
                .eutMultiplier(finalEnergyFactor)
                .build()
                .apply(r);
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_void_miner_tier", lines ->
                lines.add(Component.translatable("gtna.machine.void_miner.steam_tier")
                        .append(": ")
                        .append(detectSteamTier().copy().withStyle(ChatFormatting.GOLD)))));
        return widgets;
    }

    private Component detectSteamTier() {
        if (!isFormed()) return Component.translatable("gtna.machine.void_miner.steam_tier.unformed");

        Fluid insanelySteam = GTNAMaterials.InsanelySupercriticalSteam.getFluid();
        Fluid superHeatedSteam = GTNAMaterials.SuperHeatedSteam.getFluid();
        Fluid denseSteam = GTNAMaterials.DenseSupercriticalSteam.getFluid();

        var denseCfg = GTNABalance.getVoidMinerDenseSteam();
        var superCfg = GTNABalance.getVoidMinerSuperHeatedSteam();
        var insaneCfg = GTNABalance.getVoidMinerInsanelySteam();

        int currentTier = 0;

        for (MultiblockPartMachine part : getParts()) {
            IFluidHandler handler = part.self().getFluidHandlerCap(null, true);
            if (handler == null) continue;

            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack fs = handler.getFluidInTank(i);
                if (fs.isEmpty()) continue;

                if (fs.getFluid().isSame(insanelySteam)) {
                    return Component.translatable("gtna.machine.void_miner.steam_tier.insanely",
                            insaneCfg.outputMultiplier, String.format("%.0f", insaneCfg.speedMultiplier));
                }

                if (fs.getFluid().isSame(superHeatedSteam)) currentTier = Math.max(currentTier, 2);
                if (fs.getFluid().isSame(denseSteam)) currentTier = Math.max(currentTier, 1);
            }
        }

        return switch (currentTier) {
            case 2 -> Component.translatable("gtna.machine.void_miner.steam_tier.superheated",
                    superCfg.outputMultiplier, String.format("%.0f", superCfg.speedMultiplier));
            case 1 -> Component.translatable("gtna.machine.void_miner.steam_tier.dense",
                    denseCfg.outputMultiplier, String.format("%.0f", denseCfg.speedMultiplier));
            default -> Component.translatable("gtna.machine.void_miner.steam_tier.normal");
        };
    }
}
