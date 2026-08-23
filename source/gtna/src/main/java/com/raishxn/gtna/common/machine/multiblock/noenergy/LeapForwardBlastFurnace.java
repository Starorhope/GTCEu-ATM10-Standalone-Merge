package com.raishxn.gtna.common.machine.multiblock.noenergy;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.machine.multiblock.primitive.PrimitiveBlastFurnaceMachine;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.List;

import javax.annotation.Nonnull;

public class LeapForwardBlastFurnace extends PrimitiveBlastFurnaceMachine
                                     implements ITieredMachine {

    @SaveField
    @SyncToClient
    private int currentParallel = 8;

    @SaveField
    @SyncToClient
    private int targetDuration = 400;

    @SaveField
    @SyncToClient
    private int extraLayers = 0;

    private static final int MAX_PARALLEL_CAP = 32000;
    private static final int TICKS_PER_LAYER = 400;

    public LeapForwardBlastFurnace(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);

        Level level = this.getLevel();
        BlockPos centerPos = this.getBlockPos();

        // CORREÇÃO: Fixar a Base.
        // O Pattern define o Controller na segunda camada (índice 1).
        // Logo, a base física da máquina é SEMPRE (Y - 1).
        // Isso impede que o scanner leia a terra/chão abaixo da máquina.
        int structMinY = centerPos.getY() - 1;

        // Assumimos inicialmente que o topo é onde o controller está
        int structMaxY = centerPos.getY();

        if (level != null) {
            // Escaneamos APENAS para CIMA (de +1 até +50)
            for (int yRel = 1; yRel <= 50; yRel++) {

                boolean hasBlockInLayer = false;

                // Varredura Radial na camada
                for (int xRel = -4; xRel <= 4; xRel++) {
                    for (int zRel = -4; zRel <= 4; zRel++) {
                        BlockPos checkPos = centerPos.offset(xRel, yRel, zRel);
                        if (!level.getBlockState(checkPos).isAir()) {
                            hasBlockInLayer = true;
                            break;
                        }
                    }
                    if (hasBlockInLayer) break;
                }

                if (hasBlockInLayer) {
                    structMaxY = centerPos.getY() + yRel;
                } else {
                    // Se encontrou uma camada de ar acima do controller, a máquina acabou.
                    break;
                }
            }
        }

        int totalHeight = structMaxY - structMinY + 1;

        // Com a base fixada em (Y-1), a altura 13 agora será lida corretamente como 13.
        int newExtraLayers = Math.max(0, totalHeight - 13);
        int newTargetDuration = 400 + (newExtraLayers * TICKS_PER_LAYER);
        long calcParallel = 8L * (long) Math.pow(2, newExtraLayers);
        int newCurrentParallel = (int) Math.min(calcParallel, MAX_PARALLEL_CAP);

        if (extraLayers != newExtraLayers) {
            extraLayers = newExtraLayers;
            getSyncDataHolder().markClientSyncFieldDirty("extraLayers");
        }
        if (targetDuration != newTargetDuration) {
            targetDuration = newTargetDuration;
            getSyncDataHolder().markClientSyncFieldDirty("targetDuration");
        }
        if (currentParallel != newCurrentParallel) {
            currentParallel = newCurrentParallel;
            getSyncDataHolder().markClientSyncFieldDirty("currentParallel");
        }
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (machine instanceof LeapForwardBlastFurnace pbf) {
            int parallels = ParallelLogic.getParallelAmount(machine, recipe, pbf.currentParallel);

            double originalDuration = Math.max(1, recipe.duration);
            double durationMultiplier = (double) pbf.targetDuration / originalDuration;

            return ModifierFunction.builder()
                    .parallels(parallels)
                    .modifyAllContents(ContentModifier.multiplier(parallels))
                    .durationMultiplier(durationMultiplier)
                    .build();
        }
        return ModifierFunction.NULL;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_leap_pbf", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (isFormed()) {
            textList.add(Component.translatable("gtna.multiblock.leap_pbf.parallel_hud",
                    Component.literal(String.valueOf(currentParallel)).withStyle(ChatFormatting.GOLD)));

            textList.add(Component.translatable("gtna.multiblock.leap_pbf.layers_hud",
                    Component.literal(String.valueOf(extraLayers)).withStyle(ChatFormatting.AQUA)));

            textList.add(Component.translatable("gtna.multiblock.leap_pbf.duration_hud",
                    Component.literal(String.valueOf(targetDuration / 20)).withStyle(ChatFormatting.RED)));
        }
    }

    @Override
    public int getTier() {
        return GTValues.LV;
    }
}
