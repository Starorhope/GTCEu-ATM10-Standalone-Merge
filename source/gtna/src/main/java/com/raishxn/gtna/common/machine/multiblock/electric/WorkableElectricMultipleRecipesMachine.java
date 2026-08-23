package com.raishxn.gtna.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.raishxn.gtna.api.machine.IThreadModifierMachine;
import com.raishxn.gtna.api.machine.feature.IPatternBufferModeHost;
import com.raishxn.gtna.api.machine.multiblock.ParallelMachine;
import com.raishxn.gtna.common.machine.multiblock.part.AccelerateHatchPartMachine;
import com.raishxn.gtna.common.machine.multiblock.part.OutputBoostHatchPartMachine;
import com.raishxn.gtna.common.machine.multiblock.part.OverclockHatchPartMachine;
import com.raishxn.gtna.common.machine.multiblock.part.ThreadPartMachine;
import com.raishxn.gtna.common.machine.trait.GTNAMultipleRecipesLogic;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WorkableElectricMultipleRecipesMachine extends WorkableElectricMultiblockMachine
                                                    implements IThreadModifierMachine, ParallelMachine,
                                                    IPatternBufferModeHost {

    @Nullable
    private ThreadPartMachine threadModifierPart;
    // Listas essenciais para o Logic calcular o tempo
    private final List<AccelerateHatchPartMachine> accelerateHatches = new ArrayList<>();
    private final List<OverclockHatchPartMachine> overclockHatches = new ArrayList<>();
    private final List<OutputBoostHatchPartMachine> outputBoostHatches = new ArrayList<>();

    public WorkableElectricMultipleRecipesMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, new GTNAMultipleRecipesLogic());
    }

    // Mantemos o getRecipeModifier simples e funcional para compatibilidade
    public RecipeModifier getRecipeModifier() {
        return (machine, recipe) -> {
            // 1. Calcula Paralelo
            int parallel = ParallelLogic.getParallelAmount(machine, recipe, getMaxParallel());

            // 2. Constrói o modificador manualmente (já que não existe getModifier no ParallelLogic)
            var modifier = parallel > 1 ?
                    ModifierFunction.builder()
                            .modifyAllContents(ContentModifier.multiplier(parallel))
                            .eutMultiplier(parallel)
                            .parallels(parallel)
                            .build() :
                    ModifierFunction.IDENTITY;

            // 3. Aplica o modificador e depois o Overclock Padrão
            ModifierFunction overclock = GTRecipeModifiers.ELECTRIC_OVERCLOCK
                    .apply(getOverclockingLogic())
                    .getModifier(machine, modifier.apply(recipe));
            return modifier.andThen(overclock);
        };
    }

    @Override
    public int getMaxParallel() {
        return getParallelLimit();
    }

    protected int getParallelLimit() {
        int superParallel = getParallelHatch().map(ParallelHatchPartMachine::getCurrentParallel).orElse(1);
        if (superParallel > 1) return superParallel;
        int maxParallel = 1;
        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof ParallelHatchPartMachine hatch) {
                int current = hatch.getCurrentParallel();
                if (current > maxParallel) {
                    maxParallel = current;
                }
            }
        }
        return maxParallel;
    }

    // ESSENCIAL: Preenche as listas quando a estrutura forma
    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        this.accelerateHatches.clear();
        this.overclockHatches.clear();
        this.outputBoostHatches.clear();

        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof AccelerateHatchPartMachine accelerateHatch) {
                accelerateHatches.add(accelerateHatch);
            }
            if (part instanceof OverclockHatchPartMachine overclockHatch) {
                overclockHatches.add(overclockHatch);
            }
            if (part instanceof OutputBoostHatchPartMachine outputBoostHatch) {
                outputBoostHatches.add(outputBoostHatch);
            }
        }
        if (this.energyContainer == null) {
            this.energyContainer = getEnergyContainer();
        }
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        this.accelerateHatches.clear();
        this.overclockHatches.clear();
        this.outputBoostHatches.clear();
    }

    @Override
    public @NotNull GTNAMultipleRecipesLogic getRecipeLogic() {
        return (GTNAMultipleRecipesLogic) super.getRecipeLogic();
    }

    // Métodos usados pelo GTNAMultipleRecipesLogic para calcular a velocidade final
    public double getDurationMultiplier() {
        double multiplier = 1.0;
        for (AccelerateHatchPartMachine hatch : accelerateHatches) {
            double percentage = hatch.calcDurationPercentage(this.getTier()) / 100.0;
            multiplier *= percentage;
        }
        return Math.max(0.01, multiplier);
    }

    public double getOverclockHatchMultiplier() {
        return getOverclockDurationFactor();
    }

    public double getOverclockDurationFactor() {
        double multiplier = OverclockingLogic.STD_DURATION_FACTOR;
        for (OverclockHatchPartMachine hatch : overclockHatches) {
            multiplier = Math.min(multiplier, hatch.getOverclockMultiplier());
        }
        return multiplier;
    }

    public boolean hasOverclockHatch() {
        return !overclockHatches.isEmpty();
    }

    public OverclockingLogic getOverclockingLogic() {
        if (!hasOverclockHatch()) {
            return OverclockingLogic.NON_PERFECT_OVERCLOCK;
        }
        return OverclockingLogic.create(getOverclockDurationFactor(), OverclockingLogic.STD_VOLTAGE_FACTOR, false);
    }

    public int getOutputBoostMultiplier() {
        int multiplier = 1;
        for (OutputBoostHatchPartMachine hatch : outputBoostHatches) {
            multiplier *= hatch.getOutputMultiplier();
        }
        return Math.max(1, multiplier);
    }

    public void addDisplayText(List<Component> textList) {
        MultiblockDisplayText.builder(textList, getDefaultPatternState())
                .setWorkingStatus(recipeLogic.isWorkingEnabled(), recipeLogic.isActive())
                .addCustom(text -> {
                    GTNAMultipleRecipesLogic logic = getRecipeLogic();
                    long storedEnergy = 0;
                    if (this.energyContainer != null) {
                        storedEnergy = this.energyContainer.getEnergyStored();
                    } else if (getEnergyContainer() != null) {
                        storedEnergy = getEnergyContainer().getEnergyStored();
                    }
                    int tier = getTier();
                    String tierName = GTValues.VN[tier];
                    text.add(Component.translatable("gtna.multiblock.max_eut",
                            Component.literal(String.format(Locale.US, "%,d", storedEnergy))
                                    .withStyle(ChatFormatting.WHITE),
                            Component.literal(tierName).withStyle(ChatFormatting.GOLD))
                            .withStyle(ChatFormatting.GRAY));

                    int parallel = getMaxParallel();
                    if (parallel > 1) {
                        text.add(Component.translatable("gtna.multiblock.parallel_amount",
                                Component.literal(String.valueOf(parallel)).withStyle(ChatFormatting.GREEN))
                                .withStyle(ChatFormatting.GRAY));
                    }

                    // Informações de UI dos Hatches
                    if (hasOverclockHatch()) {
                        double ocMultiplier = getOverclockDurationFactor();
                        text.add(Component.translatable("gtna.multiblock.overclock_hatch",
                                Component.literal(String.format(Locale.US, "%.2fx", ocMultiplier))
                                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                                .withStyle(ChatFormatting.GRAY));
                    }

                    double accMultiplier = getDurationMultiplier();
                    if (accMultiplier < 1.0) {
                        text.add(Component.translatable("gtna.multiblock.accelerate_hatch",
                                Component.literal(String.format(Locale.US, "%.2fx", accMultiplier))
                                        .withStyle(ChatFormatting.LIGHT_PURPLE))
                                .withStyle(ChatFormatting.GRAY));
                    }

                    int outputMultiplier = getOutputBoostMultiplier();
                    if (outputMultiplier > 1) {
                        text.add(Component.translatable("gtna.multiblock.output_boost_hatch",
                                Component.literal(String.format(Locale.US, "%dx", outputMultiplier))
                                        .withStyle(ChatFormatting.AQUA))
                                .withStyle(ChatFormatting.GRAY));
                    }

                    text.add(Component.translatable("gtna.multiblock.active_threads",
                            Component.literal(String.valueOf(logic.getActiveRecipeCount())).withStyle(ChatFormatting.AQUA),
                            Component.literal(String.valueOf(logic.getMaxThreads())).withStyle(ChatFormatting.AQUA))
                            .withStyle(ChatFormatting.GRAY));

                    text.add(Component.empty());
                    List<Component> activeThreadsInfo = logic.getRecipeDisplayInfo();
                    if (!activeThreadsInfo.isEmpty()) text.addAll(activeThreadsInfo);
                    else text
                            .add(Component.translatable("gtna.multiblock.idle_waiting_inputs")
                                    .withStyle(ChatFormatting.DARK_GRAY));
                });
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_multiple_recipes", this::addDisplayText));
        return widgets;
    }

    @Override
    public @Nullable ThreadPartMachine getThreadPartMachine() {
        return this.threadModifierPart;
    }

    @Override
    public void setThreadPartMachine(@Nullable ThreadPartMachine threadModifierPart) {
        this.threadModifierPart = threadModifierPart;
    }

    @Override
    public @Nullable String gtna$resolvePatternBufferMode(com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (getRecipeTypes().length <= 1) {
            return null;
        }
        return recipe.getType().registryName.toString();
    }

    @Override
    public boolean gtna$applyPatternBufferMode(String modeId, com.gregtechceu.gtceu.api.recipe.GTRecipe recipe) {
        if (modeId == null || modeId.isBlank()) {
            return false;
        }
        for (int i = 0; i < getRecipeTypes().length; i++) {
            if (gtna$matchesModeId(modeId, getRecipeTypes()[i])) {
                if (getActiveRecipeType() != i) {
                    setActiveRecipeType(i);
                }
                return true;
            }
        }
        return false;
    }
}
