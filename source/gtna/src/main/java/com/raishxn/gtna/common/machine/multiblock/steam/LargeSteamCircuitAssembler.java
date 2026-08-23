package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.common.machine.multiMachineBase.SteamMultiMachineBase;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.Nonnull;

public class LargeSteamCircuitAssembler extends SteamMultiMachineBase {

    private static final int MAX_PARALLELS = 4;
    private static final int ENGRAVING_NEEDED = 16;
    private static final int OUTPUT_MULTIPLIER = 2;
    private static final int DURATION_MULTIPLIER = 4;

    @SaveField
    private boolean multiplyMode = true;

    @SaveField
    private ItemStack engravedCircuit = ItemStack.EMPTY;

    @SaveField
    private int engravedCount;

    public LargeSteamCircuitAssembler(BlockEntityCreationInfo holder, Object... args) {
        super(holder, false, args);
    }

    @Nullable
    @Override
    protected GTRecipe getRealRecipe(@Nonnull GTRecipe recipe) {
        if (recipe.getType() != GTRecipeTypes.CIRCUIT_ASSEMBLER_RECIPES || engravedCircuit.isEmpty() ||
                engravedCount < ENGRAVING_NEEDED) {
            return null;
        }
        List<Content> itemOutputs = recipe.outputs.get(ItemRecipeCapability.CAP);
        if (itemOutputs == null || itemOutputs.isEmpty()) {
            return null;
        }

        ItemStack[] outputStacks = ItemRecipeCapability.CAP.of(itemOutputs.get(0).content()).getItems();
        if (outputStacks.length == 0) {
            return null;
        }
        ItemStack output = outputStacks[0];
        if (output.getItem() != engravedCircuit.getItem()) {
            return null;
        }

        int parallels = ParallelLogic.getParallelAmount(this, recipe, MAX_PARALLELS);
        if (parallels == 0) {
            return null;
        }

        GTRecipe modified = recipe.copy();
        if (multiplyMode) {
            modified.outputs.put(ItemRecipeCapability.CAP, List.of(
                    itemOutputs.get(0).copy(ItemRecipeCapability.CAP, ContentModifier.multiplier(OUTPUT_MULTIPLIER))));
        }

        modified = ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .parallels(parallels)
                .durationMultiplier(multiplyMode ? DURATION_MULTIPLIER : 1)
                .build()
                .apply(modified);
        return modified;
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (isFormed()) {
            textList.add(ComponentPanelWidget.withButton(
                    Component.translatable("gtna.machine.large_steam_circuit_assembler.engrave_circuit"),
                    "engraveCircuit"));
            textList.add(Component.translatable("gtna.machine.large_steam_circuit_assembler.circuit",
                    engravedCircuit.isEmpty() ? Component.translatable("gtna.generic.none") : engravedCircuit.getHoverName()));
            if (!engravedCircuit.isEmpty() && engravedCount < ENGRAVING_NEEDED) {
                textList.add(Component.translatable("gtna.machine.large_steam_circuit_assembler.remaining",
                        ENGRAVING_NEEDED - engravedCount).withStyle(ChatFormatting.YELLOW));
            }
            textList.add(Component.translatable("gtna.machine.large_steam_circuit_assembler.multiply_mode",
                    multiplyMode ? Component.translatable("gtna.machine.on") : Component.translatable("gtna.machine.off"))
                    .append(ComponentPanelWidget.withButton(Component.translatable("gtna.gui.toggle"), "toggleMultiplyMode")));
        }
    }

    @Override
    public void handleDisplayClick(String componentData, ClickData clickData) {
        if (clickData.isRemote) {
            return;
        }
        if ("toggleMultiplyMode".equals(componentData)) {
            multiplyMode = !multiplyMode;
            return;
        }
        if (!"engraveCircuit".equals(componentData)) {
            return;
        }
        for (MultiblockPartMachine part : getParts()) {
            if (!(part instanceof ItemBusPartMachine bus)) {
                continue;
            }
            NotifiableItemStackHandler inventory = bus.getInventory();
            IO io = inventory.getHandlerIO();
            if (io != IO.IN && io != IO.BOTH) {
                continue;
            }
            for (int i = 0; i < inventory.getSlots(); i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (stack.isEmpty() || !isCircuit(stack)) {
                    continue;
                }
                int taken;
                if (stack.getItem() == engravedCircuit.getItem()) {
                    taken = Math.min(ENGRAVING_NEEDED - engravedCount, stack.getCount());
                    engravedCount += taken;
                } else {
                    taken = Math.min(ENGRAVING_NEEDED, stack.getCount());
                    engravedCircuit = stack.copyWithCount(1);
                    engravedCount = taken;
                }
                inventory.extractItemInternal(i, taken, false);
                if (engravedCount >= ENGRAVING_NEEDED) {
                    return;
                }
            }
        }
    }

    private static boolean isCircuit(ItemStack stack) {
        for (TagKey<Item> tagKey : stack.getTags().toList()) {
            if (tagKey.location().toString().contains("gtceu:circuits/")) {
                return true;
            }
        }
        return false;
    }
}
