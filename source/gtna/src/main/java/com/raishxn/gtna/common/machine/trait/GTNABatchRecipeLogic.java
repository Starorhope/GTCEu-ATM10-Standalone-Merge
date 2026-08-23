package com.raishxn.gtna.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class GTNABatchRecipeLogic extends RecipeLogic {

    private Supplier<@Nullable GTRecipe> recipeSupplier = () -> null;
    private Runnable recipeFinishedCallback = () -> {};

    public GTNABatchRecipeLogic() {
        super();
    }

    public GTNABatchRecipeLogic setRecipeSupplier(Supplier<@Nullable GTRecipe> recipeSupplier) {
        this.recipeSupplier = recipeSupplier;
        return this;
    }

    public GTNABatchRecipeLogic setRecipeFinishedCallback(Runnable recipeFinishedCallback) {
        this.recipeFinishedCallback = recipeFinishedCallback;
        return this;
    }

    @Override
    public void findAndHandleRecipe() {
        lastFailedMatches = null;
        clearFailureReason();
        lastRecipe = null;
        lastOriginRecipe = null;

        GTRecipe recipe = recipeSupplier.get();
        if (recipe != null) {
            var result = checkRecipe(recipe);
            if (result.isSuccess()) {
                setupRecipe(recipe);
            } else {
                putFailureReason(this, recipe, result.reason());
                setWaiting(result.reason());
            }
        } else {
            setStatus(Status.IDLE);
            consecutiveRecipes = 0;
            progress = 0;
            duration = 0;
            isActive = false;
        }
        recipeDirty = false;
    }

    @Override
    public void onRecipeFinish() {
        getRLMachine().afterWorking();
        if (lastRecipe == null) {
            recipeFinishedCallback.run();
            return;
        }
        runAttempt = 0;
        runDelay = 0;
        handleRecipeIO(lastRecipe, IO.OUT);
        setStatus(Status.IDLE);
        consecutiveRecipes = 0;
        progress = 0;
        duration = 0;
        isActive = false;
        lastRecipe = null;
        lastOriginRecipe = null;
        recipeFinishedCallback.run();
    }

    @Override
    public boolean hasCustomProgressLine() {
        return true;
    }

    @Override
    public @Nullable Component getCustomProgressLine() {
        if (duration <= 0) {
            return null;
        }
        return Component.translatable("gtna.recipe_logic.batch_progress", progress, duration);
    }
}
