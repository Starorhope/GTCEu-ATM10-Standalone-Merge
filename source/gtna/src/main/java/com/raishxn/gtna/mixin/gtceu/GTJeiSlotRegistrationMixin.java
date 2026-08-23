package com.raishxn.gtna.mixin.gtceu;

import brachy.modularui.integration.recipeviewer.handlers.IngredientProvider;
import com.gregtechceu.gtceu.integration.recipeviewer.jei.GTJeiSlotRegistration;
import com.raishxn.gtna.client.jei.GTNAJeiSlotDiagnostics;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GTJeiSlotRegistration.class, remap = false)
public abstract class GTJeiSlotRegistrationMixin {

    @Inject(method = "addSlot", at = @At("HEAD"), remap = false)
    private static void gtna$auditJeiRecipeSlot(IRecipeLayoutBuilder builder, IngredientProvider<?> provider,
                                                RecipeIngredientRole role, int index, CallbackInfo ci) {
        GTNAJeiSlotDiagnostics.inspectSlot(provider, role, index);
    }
}
