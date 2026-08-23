package com.raishxn.gtna.mixin.gtceu;

import brachy.modularui.integration.recipeviewer.entry.item.ItemTagList;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserves every candidate of a compound vanilla ingredient in GTCEu's recipe viewer. */
@Mixin(value = ItemRecipeCapability.class, remap = false)
public abstract class ItemRecipeCapabilityMixin {

    @Inject(method = "tryMapTag", at = @At("HEAD"), cancellable = true, remap = false)
    private static void gtna$doNotCollapseCompoundIngredientToFirstTag(
            Ingredient ingredient, int amount, CallbackInfoReturnable<ItemTagList> cir) {
        // GTCEu's tag fast path inspects only values[0]. For an OR ingredient
        // such as [#tag, explicit_item], that drops every later alternative and
        // can yield an empty JEI slot when the first tag is empty. Returning
        // null selects the existing getItems() fallback, which resolves the
        // complete union while leaving the single-tag fast path untouched.
        if (!ingredient.isCustom() && ingredient.getValues().length > 1) {
            cir.setReturnValue(null);
        }
    }
}
