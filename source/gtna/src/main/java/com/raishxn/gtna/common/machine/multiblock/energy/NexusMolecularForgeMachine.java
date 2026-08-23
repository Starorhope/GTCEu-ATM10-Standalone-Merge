package com.raishxn.gtna.common.machine.multiblock.energy;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.machine.multiblock.part.ae.GTNACraftPatternPartMachine;
import com.raishxn.gtna.common.machine.trait.GTNABatchRecipeLogic;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NexusMolecularForgeMachine extends WorkableElectricMultiblockMachine
                                        implements IMuiMachine {

    private static final int BATCH_SETTLE_TICKS = 2;
    private static final int BATCH_MAX_WAIT_TICKS = 20;

    private final List<GTNACraftPatternPartMachine> craftPatternParts = new ArrayList<>();
    private long activeBatchItemCount;
    private int activeBatchDistinctOutputs;
    private long activeBatchEUt;
    private int activeBatchDuration;
    private long pendingBatchStartTick = -1L;
    private long pendingBatchLastChangeTick = -1L;

    public NexusMolecularForgeMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, new GTNABatchRecipeLogic());
        getRecipeLogic()
                .setRecipeSupplier(this::buildBatchRecipe)
                .setRecipeFinishedCallback(this::clearActiveBatch);
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        craftPatternParts.clear();
        for (var part : getParts()) {
            if (part instanceof GTNACraftPatternPartMachine patternPart) {
                craftPatternParts.add(patternPart);
                patternPart.setOnContentsChanged(() -> {
                    markPendingBatchChanged();
                    getRecipeLogic().updateTickSubscription();
                });
            }
        }
    }

    @Override
    public void invalidateStructure(String substructureName) {
        super.invalidateStructure(substructureName);
        craftPatternParts.clear();
        clearActiveBatch();
        clearPendingBatchWindow();
    }

    public int getCraftPatternHatchCount() {
        return craftPatternParts.size();
    }

    public int getLoadedPatternCount() {
        int total = 0;
        for (GTNACraftPatternPartMachine part : craftPatternParts) {
            total += part.getLoadedPatternCount();
        }
        return total;
    }

    public long getQueuedItemCount() {
        long total = 0L;
        for (GTNACraftPatternPartMachine part : craftPatternParts) {
            total += part.getPendingItemCount();
        }
        return total;
    }

    private int getQueuedOutputTypes() {
        if (getLevel() == null) {
            return 0;
        }
        var provider = getLevel().registryAccess();
        Object2LongOpenCustomHashMap<ItemStack> preview = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        for (GTNACraftPatternPartMachine part : craftPatternParts) {
            for (GTNACraftPatternPartMachine.InternalSlot slot : part.getInternalInventory()) {
                slot.serializeNBT(provider).getList("inventory", 10).forEach(tag -> {
                    if (tag instanceof net.minecraft.nbt.CompoundTag compoundTag) {
                        ItemStack stack = ItemStack.parseOptional(provider, compoundTag);
                        long amount = compoundTag.getLong("real");
                        if (!stack.isEmpty() && amount > 0L) {
                            preview.addTo(stack, amount);
                        }
                    }
                });
            }
        }
        return preview.size();
    }

    private void clearActiveBatch() {
        activeBatchItemCount = 0L;
        activeBatchDistinctOutputs = 0;
        activeBatchEUt = 0L;
        activeBatchDuration = 0;
    }

    private void clearPendingBatchWindow() {
        pendingBatchStartTick = -1L;
        pendingBatchLastChangeTick = -1L;
    }

    private void markPendingBatchChanged() {
        if (getLevel() == null || isRemote()) {
            return;
        }
        long gameTime = getLevel().getGameTime();
        if (getQueuedItemCount() <= 0L) {
            clearPendingBatchWindow();
            return;
        }
        if (pendingBatchStartTick < 0L) {
            pendingBatchStartTick = gameTime;
        }
        pendingBatchLastChangeTick = gameTime;
    }

    private @Nullable GTRecipe buildBatchRecipe() {
        long maxEUt = getOverclockVoltage();
        if (maxEUt <= 0L) {
            clearActiveBatch();
            clearPendingBatchWindow();
            return null;
        }

        long queuedItemsPreview = getQueuedItemCount();
        if (queuedItemsPreview <= 0L) {
            clearActiveBatch();
            clearPendingBatchWindow();
            return null;
        }

        if (getLevel() != null) {
            long gameTime = getLevel().getGameTime();
            if (pendingBatchStartTick < 0L) {
                pendingBatchStartTick = gameTime;
            }
            if (pendingBatchLastChangeTick < 0L) {
                pendingBatchLastChangeTick = gameTime;
            }

            boolean settled = gameTime - pendingBatchLastChangeTick >= BATCH_SETTLE_TICKS;
            boolean waitedEnough = gameTime - pendingBatchStartTick >= BATCH_MAX_WAIT_TICKS;
            boolean reachedForgeCeiling = queuedItemsPreview >= maxEUt;
            if (!settled && !waitedEnough && !reachedForgeCeiling) {
                clearActiveBatch();
                return null;
            }
        }

        Object2LongOpenCustomHashMap<ItemStack> outputs = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        for (GTNACraftPatternPartMachine part : craftPatternParts) {
            part.drainPendingOutputs(outputs);
        }
        clearPendingBatchWindow();

        if (outputs.isEmpty()) {
            GTNACORE.LOGGER.debug("[GTNA] Nexus Assembly Forge at {} found no queued outputs to batch", getBlockPos());
            clearActiveBatch();
            return null;
        }

        long totalItems = 0L;
        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().recipeType(GTRecipeTypes.DUMMY_RECIPES);
        for (var entry : outputs.object2LongEntrySet()) {
            ItemStack stack = entry.getKey().copy();
            long amount = entry.getLongValue();
            totalItems += amount;
            while (amount > 0L) {
                int split = (int) Math.min(amount, Integer.MAX_VALUE);
                ItemStack output = stack.copy();
                output.setCount(split);
                builder.outputItems(output);
                amount -= split;
            }
        }

        long eut = Math.max(1L, Math.min(maxEUt, totalItems));
        int duration = Math.max(1, (int) Math.ceil(totalItems / (double) eut));

        activeBatchItemCount = totalItems;
        activeBatchDistinctOutputs = outputs.size();
        activeBatchEUt = eut;
        activeBatchDuration = duration;

        GTNACORE.LOGGER.debug(
                "[GTNA] Nexus Assembly Forge at {} built batch: {} types / {} items / {} EUt / {} t",
                getBlockPos(), activeBatchDistinctOutputs, activeBatchItemCount, activeBatchEUt, activeBatchDuration);

        builder.EUt(eut).duration(duration);
        return builder.build();
    }

    @Override
    public @NotNull GTNABatchRecipeLogic getRecipeLogic() {
        return (GTNABatchRecipeLogic) super.getRecipeLogic();
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_molecular_forge", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        long queuedItems = getQueuedItemCount();
        int queuedTypes = getQueuedOutputTypes();

        if (isFormed()) {
                    textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.pattern_hatches",
                            Component.literal(String.valueOf(getCraftPatternHatchCount()))
                                    .withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));
                    textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.loaded_patterns",
                            Component.literal(String.valueOf(getLoadedPatternCount()))
                                    .withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GRAY));
                    textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.queued_outputs",
                            Component.literal(FormattingUtil.formatNumbers(queuedTypes)).withStyle(ChatFormatting.GOLD),
                            Component.literal(FormattingUtil.formatNumbers(queuedItems)).withStyle(ChatFormatting.GOLD))
                            .withStyle(ChatFormatting.GRAY));

                    if (activeBatchItemCount > 0L) {
                        textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.active_batch",
                                Component.literal(FormattingUtil.formatNumbers(activeBatchDistinctOutputs))
                                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                                Component.literal(FormattingUtil.formatNumbers(activeBatchItemCount))
                                        .withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.GRAY));
                        textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.batch_cost",
                                Component.literal(String.format(Locale.US, "%,d", activeBatchEUt))
                                        .withStyle(ChatFormatting.RED),
                                Component.literal(String.valueOf(activeBatchDuration)).withStyle(ChatFormatting.RED))
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.active_batch_idle",
                                Component.translatable("gtna.multiblock.idle").withStyle(ChatFormatting.DARK_GRAY))
                                .withStyle(ChatFormatting.GRAY));
                    }

                    textList.add(Component.translatable("gtna.machine.nexus_molecular_forge.ui.forge_ceiling",
                            Component.literal(String.format(Locale.US, "%,d", getOverclockVoltage()))
                                    .withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY));
        }
    }
}
