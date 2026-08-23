package com.raishxn.gtna.common.machine.multiblock.noenergy;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;

import org.jetbrains.annotations.Nullable;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

public class InfernalCokeOven extends WorkableMultiblockMachine implements IMuiMachine {

    @SaveField
    @SyncToClient
    private long continuousWorkingTicks = 0;

    // Constantes
    private static final int TICKS_PER_STAGE = 10 * 60 * 20; // 10 min
    private static final int BASE_PARALLEL = 8;
    private static final int PARALLEL_INCREASE = 16;
    private static final int MAX_PARALLEL_CAP = 256;

    private static final long BASE_STEAM_PER_TICK = 320;
    private static final long STEAM_INCREASE_PER_STAGE = 80;

    public InfernalCokeOven(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    // --- LÓGICA DE AQUECIMENTO E RESFRIAMENTO ---

    @Override
    public void onLoad() {
        super.onLoad();
        // Inscreve a função coolingTick para rodar a cada tick do servidor
        if (!isRemote()) {
            subscribeServerTick(this::coolingTick);
        }
    }

    // Lógica personalizada que roda SEMPRE, independente do RecipeLogic dormir
    private void coolingTick() {
        if (!recipeLogic.isWorking() && this.continuousWorkingTicks > 0) {
            long decay = (long) (this.continuousWorkingTicks * 0.0005); // 0.05% decay
            this.continuousWorkingTicks -= Math.max(1, decay);
            markWorkingTicksDirtyPeriodically();
        }
    }

    @Override
    public boolean onWorking() {
        boolean result = super.onWorking();
        // Se estiver trabalhando, aquece
        if (result && this.continuousWorkingTicks < Long.MAX_VALUE - 100) {
            this.continuousWorkingTicks++;
            markWorkingTicksDirtyPeriodically();
        }
        return result;
    }

    private void markWorkingTicksDirtyPeriodically() {
        if (continuousWorkingTicks == 0 || getOffsetTimer() % 20 == 0) {
            getSyncDataHolder().markClientSyncFieldDirty("continuousWorkingTicks");
        }
    }

    public int getCurrentStage() {
        return (int) (this.continuousWorkingTicks / TICKS_PER_STAGE);
    }

    public double getSpeedBonus() {
        double bonus = 1.0 + ((double) continuousWorkingTicks / 100.0) * 0.01;
        return Math.min(10.0, bonus);
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (machine instanceof InfernalCokeOven ico) {
            int stage = ico.getCurrentStage();

            int maxPossibleParallel = Math.min(MAX_PARALLEL_CAP, BASE_PARALLEL + (stage * PARALLEL_INCREASE));
            int actualParallel = ParallelLogic.getParallelAmount(machine, recipe, maxPossibleParallel);

            if (actualParallel == 0) return ModifierFunction.NULL;

            double speedBonus = ico.getSpeedBonus();
            double durationMultiplier = 1.0 / speedBonus;
            long steamPerTick = BASE_STEAM_PER_TICK + ((long) stage * STEAM_INCREASE_PER_STAGE);

            var baseModifier = ModifierFunction.builder()
                    .parallels(actualParallel)
                    .modifyAllContents(ContentModifier.multiplier(actualParallel))
                    .durationMultiplier(durationMultiplier)
                    .build();

            return r -> {
                GTRecipe modified = baseModifier.apply(r);
                if (modified != null) {
                    long totalSteamLong = steamPerTick * modified.duration;
                    int totalSteamInt = (int) Math.min(Integer.MAX_VALUE, totalSteamLong);
                    FluidIngredient steamIng = FluidIngredient.of(GTMaterials.Steam.getFluid(totalSteamInt));

                    modified.inputs.computeIfAbsent(FluidRecipeCapability.CAP, k -> new ArrayList<>())
                            .add(new Content(steamIng, ChanceLogic.getMaxChancedValue(),
                                    ChanceLogic.getMaxChancedValue(), 0));
                }
                return modified;
            };
        }
        return ModifierFunction.NULL;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_infernal_coke", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (isFormed()) {
            int stage = getCurrentStage();
            long nextStageTicks = (long) (stage + 1) * TICKS_PER_STAGE - continuousWorkingTicks;
            long steamPerTick = BASE_STEAM_PER_TICK + ((long) stage * STEAM_INCREASE_PER_STAGE);

            int maxStageParallel = Math.min(MAX_PARALLEL_CAP, BASE_PARALLEL + (stage * PARALLEL_INCREASE));
            double speedPercent = (getSpeedBonus() * 100);

            textList.add(Component.translatable("gtna.multiblock.infernal_coke.speed",
                    Component.literal(String.format("%.0f%%", speedPercent)).withStyle(ChatFormatting.RED)));

            textList.add(Component.translatable("gtna.multiblock.infernal_coke.stage",
                    Component.literal(String.valueOf(stage)).withStyle(ChatFormatting.GOLD),
                    Component.literal(String.valueOf(nextStageTicks / 20)).withStyle(ChatFormatting.GRAY)));

            textList.add(Component.translatable("gtna.multiblock.infernal_coke.max_parallels",
                    Component.literal(String.valueOf(maxStageParallel)).withStyle(ChatFormatting.BLUE)));

            textList.add(Component.translatable("gtna.multiblock.infernal_coke.steam_required",
                    Component.literal(String.valueOf(steamPerTick * 20)).withStyle(ChatFormatting.GRAY)));
        }
    }
}
