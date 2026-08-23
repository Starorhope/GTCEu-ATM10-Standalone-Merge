package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.raishxn.gtna.common.data.GTNARecipeType;
import com.raishxn.gtna.common.machine.multiMachineBase.SteamMultiMachineBase;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.CycleButtonWidget;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.Nonnull;

public class SteamManufacturer extends SteamMultiMachineBase {

    @SaveField
    @SyncToClient
    private int targetParallel = 16;

    public SteamManufacturer(BlockEntityCreationInfo holder, Object... args) {
        super(holder, false, args);
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (!(machine instanceof SteamManufacturer manufacturer)) {
            return ModifierFunction.NULL;
        }
        if (recipe.getType() != GTNARecipeType.HYDRAULIC_MANUFACTURING) {
            return ModifierFunction.NULL;
        }
        int maxParallel = manufacturer.targetParallel;
        int parallels = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, maxParallel);
        if (parallels == 0) return ModifierFunction.NULL;

        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels)
                .parallels(parallels)
                .build();
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        IntSyncValue parallelIndex = new IntSyncValue(
                () -> Integer.numberOfTrailingZeros(Math.max(1, Math.min(16, targetParallel))),
                index -> {
                    targetParallel = 1 << Math.max(0, Math.min(4, index));
                    getSyncDataHolder().markClientSyncFieldDirty("targetParallel");
                }).allowC2S();
        syncManager.syncValue("gtna_target_parallel", parallelIndex);
        widgets.add(Text.dynamic(() -> Component.translatable("gtna.multiblock.parallel_amount",
                1 << parallelIndex.getIntValue()).withStyle(ChatFormatting.BLUE)).asWidget());
        widgets.add(new CycleButtonWidget().value(parallelIndex).stateCount(5).size(18));
        return widgets;
    }
}
