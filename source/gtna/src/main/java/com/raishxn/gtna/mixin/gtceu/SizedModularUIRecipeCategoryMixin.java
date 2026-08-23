package com.raishxn.gtna.mixin.gtceu;

import com.gregtechceu.gtceu.integration.recipeviewer.jei.SizedModularUIRecipeCategory;
import com.raishxn.gtna.client.jei.GTNAJeiSlotDiagnostics;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SizedModularUIRecipeCategory.class, remap = false)
public abstract class SizedModularUIRecipeCategoryMixin {

    @Inject(
            method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ljava/lang/Object;"
                    + "Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At("HEAD"),
            remap = false)
    private void gtna$beginJeiRecipeSlotAudit(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses,
                                              CallbackInfo ci) {
        GTNAJeiSlotDiagnostics.beginRecipe(recipe);
    }

    @Inject(
            method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Ljava/lang/Object;"
                    + "Lmezz/jei/api/recipe/IFocusGroup;)V",
            at = @At("RETURN"),
            remap = false)
    private void gtna$endJeiRecipeSlotAudit(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses,
                                            CallbackInfo ci) {
        GTNAJeiSlotDiagnostics.endRecipe();
    }
}
