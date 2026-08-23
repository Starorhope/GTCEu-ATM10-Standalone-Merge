package com.raishxn.gtna.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.raishxn.gtna.GTNACORE;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GTNACraftPatternPartMachine extends MEBusPartMachine implements ICraftingProvider, PatternContainer {

    @Getter
    private final int maxPatternCount;

    @SaveField
    @SyncToClient
    private String customName = "";

    public void setCustomName(String customName) {
        if (!Objects.equals(this.customName, customName)) {
            this.customName = customName;
            getSyncDataHolder().markClientSyncFieldDirty("customName");
        }
    }

    @Getter
    @SaveField
    private final CustomItemStackHandler patternInventory;

    @Getter
    @SaveField
    private final InternalSlot[] internalInventory;

    private final BiMap<IPatternDetails, InternalSlot> detailsSlotMap;

    private Runnable onContentsChanged = () -> {};

    private boolean needPatternSync;

    @Nullable
    private TickableSubscription updateSubs;

    private final InternalInventory internalPatternInventory = new InternalInventory() {

        @Override
        public int size() {
            return maxPatternCount;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return patternInventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            patternInventory.setStackInSlot(slotIndex, stack);
            patternInventory.onContentsChanged(slotIndex);
            onPatternChange(slotIndex);
        }
    };

    public GTNACraftPatternPartMachine(BlockEntityCreationInfo holder, int maxPatternCount, Object... args) {
        super(holder, IO.IN, new NotifiableItemStackHandler(1, IO.IN, IO.NONE));
        this.maxPatternCount = Math.max(1, maxPatternCount);
        this.patternInventory = new CustomItemStackHandler(this.maxPatternCount);
        this.patternInventory.setOnContentsChanged(
                () -> getSyncDataHolder().markClientSyncFieldDirty("patternInventory"));
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof EncodedPatternItem<?> &&
                !AEItems.PROCESSING_PATTERN.is(stack));
        this.internalInventory = new InternalSlot[this.maxPatternCount];
        this.detailsSlotMap = HashBiMap.create(this.maxPatternCount);
        for (int i = 0; i < this.maxPatternCount; i++) {
            this.internalInventory[i] = new InternalSlot();
        }
        getMainNode().addService(ICraftingProvider.class, this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateSubscription();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(1, this::rebuildPatternMap));
        }
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        notifyController();
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        notifyController();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        updateSubscription();
        notifyController();
    }

    private void updateSubscription() {
        if (getMainNode().isOnline()) {
            if (updateSubs == null || !updateSubs.isStillSubscribed()) {
                updateSubs = subscribeServerTick(this::update);
            }
        } else if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    private void update() {
        if (needPatternSync) {
            ICraftingProvider.requestUpdate(getMainNode());
            needPatternSync = false;
        }
    }

    public void setOnContentsChanged(Runnable onContentsChanged) {
        this.onContentsChanged = onContentsChanged == null ? () -> {} : onContentsChanged;
    }

    private void rebuildPatternMap() {
        detailsSlotMap.clear();
        for (int i = 0; i < patternInventory.getSlots(); i++) {
            ItemStack pattern = patternInventory.getStackInSlot(i);
            IPatternDetails details = decodePattern(pattern);
            if (details != null) {
                detailsSlotMap.forcePut(details, internalInventory[i]);
                GTNACORE.LOGGER.debug(
                        "[GTNA] Hatch {} loaded pattern in slot {}: {}",
                        getBlockPos(),
                        i,
                        details.getDefinition());
            }
        }
        GTNACORE.LOGGER.debug(
                "[GTNA] Hatch {} rebuilt pattern map with {} loaded pattern(s)",
                getBlockPos(),
                detailsSlotMap.size());
        needPatternSync = true;
        notifyController();
    }

    private void onPatternChange(int index) {
        if (isRemote()) {
            return;
        }
        InternalSlot slot = internalInventory[index];
        ItemStack newPattern = patternInventory.getStackInSlot(index);
        IPatternDetails newPatternDetails = decodePattern(newPattern);
        IPatternDetails oldPatternDetails = detailsSlotMap.inverse().get(slot);
        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetails)) {
            slot.clear();
        }
        if (newPatternDetails == null) {
            detailsSlotMap.inverse().remove(slot);
        } else {
            detailsSlotMap.forcePut(newPatternDetails, slot);
        }
        GTNACORE.LOGGER.debug(
                "[GTNA] Hatch {} pattern changed in slot {}: old={}, new={}",
                getBlockPos(),
                index,
                oldPatternDetails == null ? "null" : oldPatternDetails.getDefinition(),
                newPatternDetails == null ? "null" : newPatternDetails.getDefinition());
        needPatternSync = true;
        notifyController();
    }

    private @Nullable IPatternDetails decodePattern(ItemStack pattern) {
        return PatternDetailsHelper.decodePattern(pattern, getLevel());
    }

    private void notifyController() {
        onContentsChanged.run();
    }

    public int getLoadedPatternCount() {
        return detailsSlotMap.size();
    }

    public long getPendingItemCount() {
        long total = 0L;
        for (InternalSlot slot : internalInventory) {
            total += slot.getPendingItemCount();
        }
        return total;
    }

    public void drainPendingOutputs(Object2LongOpenCustomHashMap<ItemStack> target) {
        for (InternalSlot slot : internalInventory) {
            slot.drainTo(target);
        }
        notifyController();
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        int rows = Math.max(1, (int) Math.ceil(maxPatternCount / 9.0));
        SlotGroup patternSlotGroup = new SlotGroup("gtna_craft_pattern_slots", 9, 0, true);
        BooleanSyncValue isOnlineValue = new BooleanSyncValue(this::isOnline, this::setOnline);
        syncManager.syncValue("is_online", isOnlineValue);

        var flow = Flow.col().coverChildren();
        flow.child(Text.dynamic(() -> isOnlineValue.getBoolValue() ?
                        Component.translatable("gtceu.gui.me_network.online") :
                        Component.translatable("gtceu.gui.me_network.offline"))
                .asWidget().marginTop(2).marginBottom(4));
        flow.child(new Grid()
                .height(18 * rows)
                .minElementMargin(0, 0)
                .minColWidth(18)
                .minRowHeight(18)
                .leftRel(0.5f)
                .gridOfSizeWidth(maxPatternCount, Math.min(9, maxPatternCount), (x, y, index) -> new ItemSlot()
                        .slot(SyncHandlers.itemSlot(patternInventory, index)
                                .slotGroup(patternSlotGroup)
                                .accessibility(true, true)
                                .filter(stack -> stack.getItem() instanceof EncodedPatternItem<?> &&
                                        !AEItems.PROCESSING_PATTERN.is(stack))
                                .changeListener((inserted, removed, changed, init) -> onPatternChange(index)))
                        .background(GTGuiTextures.SLOT, GTGuiTextures.PATTERN_OVERLAY)));
        mainWidget.child(flow.center());
    }

    @Override
    public java.util.List<IPatternDetails> getAvailablePatterns() {
        return detailsSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        boolean formed = isFormed();
        boolean nodeActive = getMainNode().isActive();
        boolean knownPattern = containsPattern(patternDetails);
        boolean validInputs = checkInput(inputHolder);
        if (!formed || !nodeActive || !knownPattern || !validInputs) {
            GTNACORE.LOGGER.debug(
                    "[GTNA] Nexus Craft Pattern Hatch rejected pattern push at {}: formed={}, nodeActive={}, knownPattern={}, validInputs={}, outputs={}",
                    getBlockPos(), formed, nodeActive, knownPattern, validInputs, patternDetails.getOutputs().size());
            return false;
        }
        InternalSlot slot = resolveSlot(patternDetails);
        if (slot != null) {
            slot.pushPattern(patternDetails);
            GTNACORE.LOGGER.debug(
                    "[GTNA] Nexus Craft Pattern Hatch queued pattern at {} with {} outputs and {} pending items",
                    getBlockPos(), patternDetails.getOutputs().size(), slot.getPendingItemCount());
            return true;
        }
        GTNACORE.LOGGER.debug("[GTNA] Nexus Craft Pattern Hatch could not resolve internal slot for pattern at {}",
                getBlockPos());
        return false;
    }

    private boolean containsPattern(IPatternDetails patternDetails) {
        if (detailsSlotMap.containsKey(patternDetails)) {
            return true;
        }
        var definition = patternDetails.getDefinition();
        for (IPatternDetails details : detailsSlotMap.keySet()) {
            if (details != null && details.getDefinition().equals(definition)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable InternalSlot resolveSlot(IPatternDetails patternDetails) {
        InternalSlot slot = detailsSlotMap.get(patternDetails);
        if (slot != null) {
            return slot;
        }
        var definition = patternDetails.getDefinition();
        for (var entry : detailsSlotMap.entrySet()) {
            IPatternDetails details = entry.getKey();
            if (details != null && details.getDefinition().equals(definition)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            boolean illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()) && !id.equals(AEKeyType.fluids().getId()));
            if (illegal) {
                return false;
            }
        }
        return true;
    }

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return internalPatternInventory;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        if (isFormed() && !getControllers().isEmpty()) {
            MultiblockControllerMachine controller = getControllers().first();
            MultiblockMachineDefinition controllerDefinition = controller.getDefinition();
            Component groupName = customName.isEmpty() ?
                    Component.translatable(controllerDefinition.getDescriptionId()) :
                    Component.literal(customName);
            return new PatternContainerGroup(
                    AEItemKey.of(controllerDefinition.asStack()),
                    groupName,
                    Collections.emptyList());
        }
        return new PatternContainerGroup(
                AEItemKey.of(getDefinition().asStack()),
                customName.isEmpty() ?
                        Component.translatable(getBlockState().getBlock().getDescriptionId()) :
                        Component.literal(customName),
                java.util.List.of(
                        Component.translatable("gtna.machine.craft_pattern_hatch.slots", maxPatternCount),
                        Component.translatable("gtna.machine.craft_pattern_hatch.patterns")));
    }

    @Override
    public boolean shouldSyncME() {
        return false;
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        patternInventory.dropInventoryInWorld(getLevel(), getBlockPos());
    }

    public final class InternalSlot implements INBTSerializable<CompoundTag> {

        private final Object2LongOpenCustomHashMap<ItemStack> outputInventory = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());

        public long getPendingItemCount() {
            long total = 0L;
            for (long value : outputInventory.values()) {
                total += value;
            }
            return total;
        }

        public boolean isEmpty() {
            return outputInventory.isEmpty();
        }

        public void clear() {
            outputInventory.clear();
            notifyController();
        }

        public void drainTo(Object2LongOpenCustomHashMap<ItemStack> target) {
            if (outputInventory.isEmpty()) {
                return;
            }
            outputInventory.object2LongEntrySet().forEach(entry -> target.addTo(entry.getKey(), entry.getLongValue()));
            outputInventory.clear();
        }

        public void loadFrom(Object2LongOpenCustomHashMap<ItemStack> source) {
            outputInventory.clear();
            source.object2LongEntrySet().forEach(entry -> outputInventory.put(entry.getKey(), entry.getLongValue()));
            notifyController();
        }

        public void pushPattern(IPatternDetails patternDetails) {
            for (GenericStack output : patternDetails.getOutputs()) {
                if (output == null || !(output.what() instanceof AEItemKey itemKey)) {
                    continue;
                }
                ItemStack stack = itemKey.toStack();
                if (!stack.isEmpty() && output.amount() > 0L) {
                    outputInventory.addTo(stack, output.amount());
                }
            }
            notifyController();
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            for (var entry : outputInventory.object2LongEntrySet()) {
                CompoundTag itemTag = (CompoundTag) entry.getKey().save(provider);
                itemTag.putLong("real", entry.getLongValue());
                itemsTag.add(itemTag);
            }
            if (!itemsTag.isEmpty()) {
                tag.put("inventory", itemsTag);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            outputInventory.clear();
            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            for (Tag entry : items) {
                if (!(entry instanceof CompoundTag compoundTag)) {
                    continue;
                }
                ItemStack stack = ItemStack.parseOptional(provider, compoundTag);
                long amount = compoundTag.getLong("real");
                if (!stack.isEmpty() && amount > 0L) {
                    outputInventory.put(stack, amount);
                }
            }
            notifyController();
        }
    }
}
