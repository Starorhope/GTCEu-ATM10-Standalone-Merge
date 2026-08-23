package yuuki1293.pccard.mixins;

import appeng.api.stacks.GenericStack;
import com.gregtechceu.gtceu.common.data.GTItems;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tamaized.ae2jeiintegration.integration.modules.jei.GenericEntryStackHelper;
import yuuki1293.pccard.ConfigClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Makes the configured GT circuit catalyst part of a JEI-transferred processing pattern. */
@Mixin(value = GenericEntryStackHelper.class, remap = false)
public abstract class GenericEntryStackHelperJeiMixin {
    @Shadow
    private static Stream<GenericStack> ofSlot(IRecipeSlotView slot) {
        throw new AssertionError();
    }

    @Inject(method = "ofInputs", at = @At("RETURN"), cancellable = true)
    private static void pCCard$includeProgrammedCircuit(
        IRecipeSlotsView recipeLayout,
        CallbackInfoReturnable<List<List<GenericStack>>> cir) {
        if (!ConfigClient.getJeiIntegration()) {
            return;
        }

        var circuitItem = GTItems.PROGRAMMED_CIRCUIT.asStack();
        var catalyst = recipeLayout.getSlotViews(RecipeIngredientRole.CATALYST).stream()
            .filter(slot -> ItemStack.isSameItem(slot.getDisplayedItemStack().orElse(ItemStack.EMPTY), circuitItem))
            .findFirst();
        if (catalyst.isEmpty()) {
            return;
        }

        var circuitAlternatives = ofSlot(catalyst.get()).toList();
        if (circuitAlternatives.isEmpty()) {
            return;
        }

        var inputs = new ArrayList<>(cir.getReturnValue());
        inputs.add(circuitAlternatives);
        cir.setReturnValue(List.copyOf(inputs));
    }
}
