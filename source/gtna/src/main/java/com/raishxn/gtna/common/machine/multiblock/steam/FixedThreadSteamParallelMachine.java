package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import com.raishxn.gtna.api.machine.IThreadModifierMachine;
import com.raishxn.gtna.common.machine.multiblock.part.ThreadPartMachine;
import com.raishxn.gtna.common.machine.trait.GTNAMultipleRecipesLogic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FixedThreadSteamParallelMachine extends AdjustableSteamParallelMachine implements IThreadModifierMachine {

    private final int fixedThreads;

    public FixedThreadSteamParallelMachine(BlockEntityCreationInfo holder, GTRecipeType recipeType, int defaultParallel,
                                           int maxParallel, double durationMultiplier, int fixedThreads,
                                           Object... args) {
        super(holder, recipeType, defaultParallel, maxParallel, durationMultiplier, false,
                new GTNAMultipleRecipesLogic());
        this.fixedThreads = Math.max(1, fixedThreads);
    }

    @Override
    public @NotNull GTNAMultipleRecipesLogic getRecipeLogic() {
        return (GTNAMultipleRecipesLogic) super.getRecipeLogic();
    }

    @Override
    public int getAdditionalThread() {
        return fixedThreads - 1;
    }

    public RecipeModifier getRecipeModifier() {
        return (machine, recipe) -> ModifierFunction.IDENTITY;
    }

    @Override
    public @Nullable ThreadPartMachine getThreadPartMachine() {
        return null;
    }

    @Override
    public void setThreadPartMachine(@Nullable ThreadPartMachine threadModifierPart) {}

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            GTNAMultipleRecipesLogic logic = getRecipeLogic();
            textList.add(Component.translatable("gtna.multiblock.active_threads.label").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(logic.getActiveRecipeCount() + " / " + logic.getMaxThreads())
                            .withStyle(ChatFormatting.AQUA)));
            textList.addAll(logic.getRecipeDisplayInfo());
        }
    }
}
