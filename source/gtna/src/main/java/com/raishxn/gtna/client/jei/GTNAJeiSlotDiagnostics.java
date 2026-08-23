package com.raishxn.gtna.client.jei;

import brachy.modularui.integration.recipeviewer.handlers.IngredientProvider;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition;
import com.raishxn.gtna.GTNACORE;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Read-only diagnostics for the silent GTCEu -> JEI slot conversion path.
 *
 * <p>The checks deliberately mirror {@code GTJeiSlotRegistration}: empty stack removal followed by
 * JEI typed-ingredient validation. Runtime ingredient-registry membership is intentionally not
 * queried: it does not determine whether an already-built recipe slot can render, and doing that
 * global UID lookup for every candidate makes large GTCEu recipe sets prohibitively slow. No
 * recipe, provider, builder, or JEI collection is mutated here.</p>
 */
public final class GTNAJeiSlotDiagnostics {

    private static final ThreadLocal<RecipeContext> CURRENT_RECIPE = new ThreadLocal<>();
    private static final Set<SlotKey> REPORTED_SLOTS = ConcurrentHashMap.newKeySet();
    private static final int MAX_SAMPLE_COUNT = 3;
    private static final boolean ENABLED = Boolean.getBoolean("gtna.codexJeiSlotAudit");

    private GTNAJeiSlotDiagnostics() {
    }

    public static void beginRecipe(Object candidate) {
        if (!ENABLED) {
            CURRENT_RECIPE.remove();
        } else if (candidate instanceof GTRecipe recipe) {
            ResourceLocation recipeType = recipe.getType().getRegistryName();
            CURRENT_RECIPE.set(new RecipeContext(recipe, recipe.getId(), recipeType));
        } else {
            CURRENT_RECIPE.remove();
        }
    }

    public static void endRecipe() {
        CURRENT_RECIPE.remove();
    }

    public static void inspectSlot(IngredientProvider<?> provider, RecipeIngredientRole role, int slotIndex) {
        RecipeContext context = CURRENT_RECIPE.get();
        if (context == null) {
            return;
        }

        // Diagnostics must never become a new failure mode in JEI recipe layout construction.
        try {
            inspectSlot(context, provider, role, slotIndex);
        } catch (Throwable ignored) {
            // The original GTCEu call still performs its own validation and preserves its exact behavior.
        }
    }

    private static void inspectSlot(RecipeContext context, IngredientProvider<?> provider,
                                    RecipeIngredientRole role, int slotIndex) {
        List<?> rawIngredients = provider.getIngredients().getStacks();
        int rawCount = rawIngredients.size();
        List<Object> nonEmptyIngredients = new ArrayList<>(rawCount);
        for (Object ingredient : rawIngredients) {
            if (!isEmptyIngredient(ingredient)) {
                nonEmptyIngredients.add(ingredient);
            }
        }

        int rawNonEmptyCount = nonEmptyIngredients.size();
        boolean contentExpected = expectsMappedContent(context.recipe(), provider.getName(), role);
        if (rawNonEmptyCount == 0) {
            if (contentExpected) {
                report(context, provider, role, slotIndex, "MAPPED_CONTENT_EMPTY", rawCount,
                        rawNonEmptyCount, rawNonEmptyCount, 0, 0, 0, List.of());
            }
            return;
        }

        IIngredientManager manager = com.gregtechceu.gtceu.integration.recipeviewer.jei.GTJeiSlotRegistration
                .getIngredientManager(brachy.modularui.integration.jei.ModularUIJeiPlugin.getRuntime());
        var checkedType = manager.getIngredientTypeChecked(provider.ingredientClass());
        if (checkedType.isEmpty()) {
            report(context, provider, role, slotIndex, "INGREDIENT_TYPE_UNREGISTERED", rawCount,
                    rawNonEmptyCount, rawNonEmptyCount, 0, 0, rawNonEmptyCount,
                    sampleRawIngredients(nonEmptyIngredients));
            return;
        }

        IngredientState state = inspectIngredientState(manager, checkedType.get(), nonEmptyIngredients);
        if (state.jeiAcceptedCount() == 0) {
            report(context, provider, role, slotIndex, "JEI_FILTERED_ALL", rawCount, rawNonEmptyCount,
                    rawNonEmptyCount, state.jeiAcceptedCount(), state.jeiRegisteredCount(),
                    state.missingFromRegistryCount(), state.samples());
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static IngredientState inspectIngredientState(IIngredientManager manager, IIngredientType<?> ingredientType,
                                                          List<Object> ingredients) {
        IIngredientType rawType = ingredientType;
        IIngredientHelper helper = manager.getIngredientHelper(rawType);
        List<String> samples = new ArrayList<>(MAX_SAMPLE_COUNT);

        for (Object ingredient : ingredients) {
            boolean valid;
            try {
                valid = manager.createTypedIngredient(rawType, ingredient, false).isPresent();
            } catch (RuntimeException exception) {
                valid = false;
            }

            if (valid) {
                // This anomaly check only needs to know whether every candidate was rejected.
                // Stop at the first accepted value so diagnostics stay effectively constant-time
                // for normal slots, including very large tag/compound ingredients.
                return new IngredientState(1, 0, 0, List.of());
            }
            addSample(samples, describe(helper, ingredient));
        }

        return new IngredientState(0, 0, 0, List.copyOf(samples));
    }

    private static boolean expectsMappedContent(GTRecipe recipe, String widgetName, RecipeIngredientRole role) {
        ParsedCapabilitySlot slot = ParsedCapabilitySlot.parse(widgetName);
        if (slot == null) {
            return false;
        }

        // Assembly-line research is injected into the dedicated item_in_16
        // catalyst after ordinary recipe contents are mapped, so it is not in
        // recipe.getInputContents(ItemRecipeCapability.CAP).
        if (role == RecipeIngredientRole.CATALYST && slot.input()
                && slot.capability().equals("item") && slot.capabilityIndex() == 16) {
            return recipe.conditions.stream()
                    .filter(ResearchCondition.class::isInstance)
                    .map(ResearchCondition.class::cast)
                    .anyMatch(condition -> condition.getData().iterator().hasNext());
        }

        RecipeCapability<?> capability = switch (slot.capability()) {
            case "item" -> ItemRecipeCapability.CAP;
            case "fluid" -> FluidRecipeCapability.CAP;
            default -> null;
        };
        if (capability == null) {
            return false;
        }

        int contentCount;
        if (slot.input()) {
            contentCount = recipe.getInputContents(capability).size()
                    + recipe.getTickInputContents(capability).size();
        } else {
            contentCount = recipe.getOutputContents(capability).size()
                    + recipe.getTickOutputContents(capability).size();
        }
        return slot.capabilityIndex() < contentCount;
    }

    private static void report(RecipeContext context, IngredientProvider<?> provider, RecipeIngredientRole role,
                               int slotIndex, String anomaly, int rawCount, int rawNonEmptyCount,
                               int gtFilteredCount, int jeiAcceptedCount, int jeiRegisteredCount,
                               int missingFromRegistryCount, List<String> samples) {
        SlotKey key = new SlotKey(context.recipeId(), context.recipeType(), role, slotIndex);
        if (!REPORTED_SLOTS.add(key)) {
            return;
        }

        GTNACORE.LOGGER.warn(
                "[Codex GTNA JEI Slot Audit] anomaly={} recipe={} type={} role={} slot={} widget={} "
                        + "ingredientClass={} raw={} rawNonEmpty={} gtFiltered={} jeiAccepted={} "
                        + "jeiRegistered={} missingFromRegistry={} samples={}",
                anomaly, context.recipeId(), context.recipeType(), role, slotIndex, provider.getName(),
                provider.ingredientClass().getName(), rawCount, rawNonEmptyCount, gtFilteredCount,
                jeiAcceptedCount, jeiRegisteredCount, missingFromRegistryCount, samples);
    }

    private static List<String> sampleRawIngredients(List<Object> ingredients) {
        List<String> samples = new ArrayList<>(MAX_SAMPLE_COUNT);
        for (Object ingredient : ingredients) {
            addSample(samples, String.valueOf(ingredient));
        }
        return List.copyOf(samples);
    }

    @SuppressWarnings("rawtypes")
    private static String describe(IIngredientHelper helper, Object ingredient) {
        try {
            return helper.getErrorInfo(ingredient);
        } catch (RuntimeException exception) {
            return String.valueOf(ingredient);
        }
    }

    private static void addSample(List<String> samples, String sample) {
        if (samples.size() < MAX_SAMPLE_COUNT && !samples.contains(sample)) {
            samples.add(sample);
        }
    }

    private static boolean isEmptyIngredient(Object ingredient) {
        return ingredient == null
                || ingredient instanceof ItemStack itemStack && itemStack.isEmpty()
                || ingredient instanceof FluidStack fluidStack && fluidStack.isEmpty();
    }

    private record RecipeContext(GTRecipe recipe, ResourceLocation recipeId, ResourceLocation recipeType) {
    }

    private record SlotKey(ResourceLocation recipeId, ResourceLocation recipeType,
                           RecipeIngredientRole role, int slotIndex) {
    }

    private record IngredientState(int jeiAcceptedCount, int jeiRegisteredCount,
                                   int missingFromRegistryCount, List<String> samples) {
    }

    private record ParsedCapabilitySlot(String capability, boolean input, int capabilityIndex) {

        private static ParsedCapabilitySlot parse(String widgetName) {
            if (widgetName == null || widgetName.isEmpty()) {
                return null;
            }

            int namespaceSeparator = widgetName.indexOf(':');
            String localName = namespaceSeparator >= 0 ? widgetName.substring(namespaceSeparator + 1) : widgetName;
            String[] parts = localName.split("_");
            if (parts.length != 3) {
                return null;
            }

            boolean input;
            if (parts[1].equals("in") || parts[1].equals("input")) {
                input = true;
            } else if (parts[1].equals("out") || parts[1].equals("output")) {
                input = false;
            } else {
                return null;
            }

            try {
                return new ParsedCapabilitySlot(parts[0], input, Integer.parseInt(parts[2]));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
