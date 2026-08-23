package com.raishxn.gtna.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredientExtensions;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.item.GTDataComponents;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.common.mui.GTMuiMachineUtil;
import com.gregtechceu.gtceu.common.mui.widgets.PopupPanel;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternContainer;
import brachy.modularui.api.IPanelHandler;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.drawable.DrawableStack;
import brachy.modularui.drawable.DynamicDrawable;
import brachy.modularui.drawable.ItemDrawable;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.RichTooltip;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.FluidSlotSyncHandler;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.PhantomItemSlotSyncHandler;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.FluidSlot;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.PhantomItemSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import brachy.modularui.widgets.textfield.TextFieldWidget;
import com.mojang.blaze3d.platform.InputConstants;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.machine.feature.IPatternBufferModeHost;
import com.raishxn.gtna.api.machine.feature.IPatternBufferModeProvider;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GTNAMEPatternBufferPartMachine extends MEBusPartMachine
                                            implements ICraftingProvider, PatternContainer, IDataStickInteractable,
                                            IPatternBufferModeProvider {

    private static final String PATTERN_RECIPE_ID_TAG = "gtnaPatternRecipeId";
    private static final String PATTERN_MODE_ID_TAG = "gtnaPatternModeId";

    @Getter
    private final int maxPatternCount;

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

    @Getter
    @SaveField
    @SyncToClient
    private final CustomItemStackHandler patternInventory;

    @Getter
    @SaveField
    protected final NotifiableItemStackHandler shareInventory;

    @Getter
    @SaveField
    protected final NotifiableFluidTank shareTank;

    @Getter
    @SaveField
    protected final InternalSlot[] internalInventory;

    @Getter
    @SaveField
    protected final GTNAPatternBufferSlotConfig[] slotConfigs;

    private final BiMap<IPatternDetails, InternalSlot> detailsSlotMap;

    @Getter
    protected final GTNAPatternBufferRecipeHandler internalRecipeHandler;

    @SyncToClient
    @SaveField
    private String customName = "";

    public void setCustomName(String customName) {
        if (!Objects.equals(this.customName, customName)) {
            this.customName = customName;
            getSyncDataHolder().markClientSyncFieldDirty("customName");
        }
    }

    private boolean needPatternSync;
    private int selectedSlot;
    @SyncToClient
    private String availableModeIds = "";
    private final SelectedConfigItemTransfer selectedConfigItems = new SelectedConfigItemTransfer();
    private final SelectedConfigFluidTank[] selectedConfigFluids = new SelectedConfigFluidTank[9];

    @Nullable
    protected TickableSubscription updateSubs;

    public GTNAMEPatternBufferPartMachine(BlockEntityCreationInfo holder, int maxPatternCount, Object... args) {
        super(holder, IO.IN, new NotifiableItemStackHandler(9, IO.IN, IO.NONE));
        this.maxPatternCount = Math.max(1, maxPatternCount);
        this.patternInventory = new CustomItemStackHandler(this.maxPatternCount);
        this.patternInventory.setOnContentsChanged(
                () -> getSyncDataHolder().markClientSyncFieldDirty("patternInventory"));
        this.patternInventory.setFilter(AEItems.PROCESSING_PATTERN::is);
        this.internalInventory = new InternalSlot[this.maxPatternCount];
        this.slotConfigs = new GTNAPatternBufferSlotConfig[this.maxPatternCount];
        this.detailsSlotMap = HashBiMap.create(this.maxPatternCount);
        for (int i = 0; i < this.maxPatternCount; i++) {
            this.internalInventory[i] = new InternalSlot();
            this.slotConfigs[i] = new GTNAPatternBufferSlotConfig();
            int slotIndex = i;
            this.slotConfigs[i].setOnContentsChanged(() -> onSlotConfigurationChanged(slotIndex));
        }
        for (int i = 0; i < selectedConfigFluids.length; i++) {
            selectedConfigFluids[i] = new SelectedConfigFluidTank(i);
        }
        getMainNode().addService(ICraftingProvider.class, this);
        this.shareInventory = attachTrait(new NotifiableItemStackHandler(9, IO.IN, IO.NONE));
        this.shareTank = attachTrait(new NotifiableFluidTank(9, 8 * FluidType.BUCKET_VOLUME, IO.IN, IO.NONE));
        this.internalRecipeHandler = new GTNAPatternBufferRecipeHandler(this, this.internalInventory, this.slotConfigs);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        refreshAvailableModesCache();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(1, this::rebuildPatternMap));
        }
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        refreshAvailableModesCache();
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        refreshAvailableModesCache();
    }

    @Override
    public List<RecipeHandlerList> getRecipeHandlers() {
        return internalRecipeHandler.getSlotHandlers();
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    @Override
    public boolean isDistinct() {
        return true;
    }

    @Override
    public void setDistinct(boolean ignored) {}

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        updateSubscription();
    }

    protected void updateSubscription() {
        if (getMainNode().isOnline()) {
            if (updateSubs == null || !updateSubs.isStillSubscribed()) {
                updateSubs = subscribeServerTick(this::update);
            }
        } else if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    protected void update() {
        if (needPatternSync) {
            ICraftingProvider.requestUpdate(getMainNode());
            needPatternSync = false;
        }
    }

    public GTNAPatternBufferSlotConfig getSlotConfig(int slot) {
        return slotConfigs[slot];
    }

    public void invalidateSlotCache(int slot) {
        if (slot >= 0 && slot < slotConfigs.length) {
            slotConfigs[slot].clearRecipeCacheSilently();
            clearPatternRecipeMetadata(slot);
        }
    }

    @Override
    public @Nullable String gtna$getPreferredModeForRecipe(GTRecipe recipe) {
        SlotMatch match = findMatchingSlot(recipe);
        if (match == null) {
            return null;
        }
        GTNAPatternBufferSlotConfig config = slotConfigs[match.slot()];
        if (!config.getPreferredModeId().isBlank()) {
            return config.getPreferredModeId();
        }
        return config.getDerivedModeId().isBlank() ? null : config.getDerivedModeId();
    }

    @Override
    public void gtna$onRecipeStarted(GTRecipe recipe) {
        SlotMatch match = findMatchingSlot(recipe);
        if (match == null) {
            return;
        }
        cacheResolvedRecipe(match.slot(), recipe);
        if (match.slot() == selectedSlot) {
            refreshSelectedConfigPreview();
        }
        markAsChanged();
    }

    private void rebuildPatternMap() {
        detailsSlotMap.clear();
        for (int i = 0; i < patternInventory.getSlots(); i++) {
            ItemStack pattern = patternInventory.getStackInSlot(i);
            IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, getLevel());
            if (details != null) {
                detailsSlotMap.forcePut(details, internalInventory[i]);
            }
            loadPatternRecipeMetadata(i, pattern);
        }
        needPatternSync = true;
    }

    private void onSlotConfigurationChanged(int slot) {
        invalidateSlotCache(slot);
        resolveAndCacheSlotRecipe(slot);
        needPatternSync = true;
        if (slot == selectedSlot) {
            refreshSelectedConfigPreview();
        }
        markAsChanged();
    }

    private void onPatternChange(int index) {
        if (isRemote()) return;
        InternalSlot internalSlot = internalInventory[index];
        ItemStack newPattern = patternInventory.getStackInSlot(index);
        IPatternDetails newPatternDetails = PatternDetailsHelper.decodePattern(newPattern, getLevel());
        IPatternDetails oldPatternDetails = detailsSlotMap.inverse().get(internalSlot);
        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetails)) {
            internalSlot.refund();
        }
        if (newPatternDetails == null) {
            detailsSlotMap.inverse().remove(internalSlot);
        } else {
            detailsSlotMap.forcePut(newPatternDetails, internalSlot);
        }
        invalidateSlotCache(index);
        loadPatternRecipeMetadata(index, newPattern);
        resolveAndCacheSlotRecipe(index);
        needPatternSync = true;
    }

    private void refundAll() {
        if (isRemote()) return;
        for (InternalSlot internalSlot : internalInventory) {
            internalSlot.refund();
        }
    }

    public boolean canRefund() {
        for (InternalSlot internalSlot : internalInventory) {
            if (!internalSlot.isItemEmpty() || !internalSlot.isFluidEmpty()) return true;
        }
        return false;
    }

    @Override
    public MachineUIPanelBuilder getPanelBuilder(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        IPanelHandler renamingPanel = syncManager.syncedPanel("gtna_renaming", true,
                (panelSync, handler) -> PopupPanel.createPopupPanel("gtna_renaming_panel", 110, 40)
                        .child(Flow.col().coverChildren()
                                .child(Text.lang("gtceu.gui.pattern_buffer.set_custom_name").asWidget())
                                .child(new TextFieldWidget().size(90, 20)
                                        .value(SyncHandlers.string(() -> customName, this::setCustomName)))
                                .margin(5)));

        IPanelHandler configPanel = syncManager.syncedPanel("gtna_slot_config", true,
                (panelSync, handler) -> createSlotConfigPanel(panelSync));

        IPanelHandler sharedItemsPanel = syncManager.syncedPanel("gtna_shared_items", true,
                (panelSync, handler) -> {
                    SlotGroup slots = new SlotGroup("gtna_shared_item_slots", 3, false);
                    return PopupPanel.createPopupPanel("gtna_shared_items_panel", 80, 86)
                            .child(Text.lang("gui.gtceu.share_inventory.title").asWidget().margin(4))
                            .child(new Grid().top(26).height(54).minElementMargin(0, 0)
                                    .minColWidth(18).minRowHeight(18).leftRel(0.5f)
                                    .gridOfSizeWidth(9, 3, (x, y, index) -> new ItemSlot()
                                            .slot(SyncHandlers.itemSlot(shareInventory, index)
                                                    .slotGroup(slots).accessibility(true, true))));
                });

        IPanelHandler sharedFluidsPanel = syncManager.syncedPanel("gtna_shared_fluids", true,
                (panelSync, handler) -> PopupPanel.createPopupPanel("gtna_shared_fluids_panel", 85, 86)
                        .child(Text.lang("gui.gtceu.share_tank.title").asWidget().margin(4))
                        .child(GTMuiMachineUtil.createSlotGroupFromInventory(panelSync, shareTank,
                                "gtna_shared_fluid_slots", 9, 'F', GTMuiMachineUtil.createSquareMatrix(9, 'F'))
                                .top(26).leftRel(0.5f)));

        BooleanSyncValue canRefund = SyncHandlers.bool(this::canRefund, ignored -> {});
        syncManager.syncValue("gtna_can_refund", canRefund);
        syncManager.registerServerSyncedAction("gtna_refund", packet -> refundAll());

        return MachineUIPanelBuilder.panelBuilder(this).leftConfigurators(flow -> flow
                .child(panelButton(configPanel, Text.str("⚙"), "gtna.machine.pattern_buffer.middle_click_hint"))
                .child(panelButton(sharedItemsPanel, GTGuiTextures.BUTTON_ITEM_OUTPUT,
                        "gui.gtceu.share_inventory.desc.0"))
                .child(panelButton(sharedFluidsPanel, GTGuiTextures.BUTTON_FLUID_OUTPUT,
                        "gui.gtceu.share_tank.desc.0"))
                .child(new ButtonWidget<>().size(18)
                        .onMousePressed((context, button) -> {
                            if (button == InputConstants.MOUSE_BUTTON_LEFT && canRefund.getBoolValue()) {
                                syncManager.callSyncedAction("gtna_refund");
                                return true;
                            }
                            return false;
                        })
                        .overlay(new DynamicDrawable(() -> canRefund.getBoolValue() ?
                                GTGuiTextures.REFUND_OVERLAY.asIcon().size(16) :
                                new DrawableStack(GTGuiTextures.REFUND_OVERLAY, new ItemDrawable(Items.BARRIER))
                                        .asIcon().size(16)))
                        .tooltip(new RichTooltip().addLine(Text.lang("gui.gtceu.refund_all.desc"))))
                .child(panelButton(renamingPanel, Text.str("✎"), "gui.gtceu.rename.desc")));
    }

    private ButtonWidget<?> panelButton(IPanelHandler panel, brachy.modularui.api.drawable.IDrawable icon,
                                        String tooltipKey) {
        return new ButtonWidget<>().size(18)
                .onMousePressed((context, button) -> {
                    if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                        panel.openPanel();
                        return true;
                    }
                    return false;
                })
                .overlay(icon)
                .tooltip(new RichTooltip().addLine(Text.lang(tooltipKey)));
    }

    private ModularPanel<?> createSlotConfigPanel(PanelSyncManager syncManager) {
        syncManager.syncValue("gtna_selected_slot",
                SyncHandlers.intNumber(() -> selectedSlot, this::selectSlot));
        syncManager.registerServerSyncedAction("gtna_previous_slot", packet ->
                selectSlot(Math.floorMod(selectedSlot - 1, maxPatternCount)));
        syncManager.registerServerSyncedAction("gtna_next_slot", packet ->
                selectSlot((selectedSlot + 1) % maxPatternCount));
        syncManager.registerServerSyncedAction("gtna_cycle_mode", packet -> cycleSelectedMode());
        syncManager.registerServerSyncedAction("gtna_clear_specialization", packet -> clearSelectedSpecialization());
        syncManager.registerServerSyncedAction("gtna_clear_cache", packet -> clearSelectedRecipeCache());

        var panel = PopupPanel.createPopupPanel("gtna_slot_config_panel", 142, 218);
        panel.child(Text.dynamic(() -> Component.translatable("gtna.machine.pattern_buffer.selected_slot",
                selectedSlot + 1)).asWidget().pos(31, 6));
        panel.child(actionButton(syncManager, "gtna_previous_slot", "<", 8, 4,
                "gtna.machine.pattern_buffer.selected_slot"));
        panel.child(actionButton(syncManager, "gtna_next_slot", ">", 116, 4,
                "gtna.machine.pattern_buffer.selected_slot"));

        panel.child(Text.lang("gtna.machine.pattern_buffer.circuit_field").asWidget().pos(8, 28));
        panel.child(new TextFieldWidget().size(44, 18).pos(87, 24)
                .value(SyncHandlers.intNumber(
                        () -> getSelectedConfig() == null ? -1 : getSelectedConfig().getCircuitConfig(),
                        value -> {
                            GTNAPatternBufferSlotConfig config = getSelectedConfig();
                            if (config != null) config.setCircuitConfig(value);
                        }))
                .setNumbers(-1, IntCircuitBehaviour.CIRCUIT_MAX));

        panel.child(Text.lang("gtna.machine.pattern_buffer.item_field").asWidget().pos(8, 48));
        panel.child(new Grid().pos(8, 60).height(54).minElementMargin(0, 0)
                .minColWidth(18).minRowHeight(18)
                .gridOfSizeWidth(9, 3, (x, y, index) -> new PhantomItemSlot().size(18)
                        .syncHandler(new PhantomItemSlotSyncHandler(new ModularSlot(selectedConfigItems, index)
                                .ignoreMaxStackSize(true)
                                .accessibility(true, false)
                                .changeListener((inserted, removed, changed, init) -> onSelectedConfigWidgetChanged())))));

        panel.child(Text.lang("gtna.machine.pattern_buffer.fluid_field").asWidget().pos(78, 48));
        panel.child(new Grid().pos(78, 60).height(54).minElementMargin(0, 0)
                .minColWidth(18).minRowHeight(18)
                .gridOfSizeWidth(9, 3, (x, y, index) -> new FluidSlot().size(18)
                        .syncHandler(new FluidSlotSyncHandler(selectedConfigFluids[index])
                                .controlsAmount(true).phantom(true))));

        panel.child(Text.dynamic(() -> Component.translatable("gtna.machine.pattern_buffer.cached_recipe_short",
                compactDisplay(getSelectedConfig() == null ? "" : getSelectedConfig().getCachedRecipeId(), 22)))
                .asWidget().pos(8, 122));
        panel.child(Text.dynamic(() -> Component.translatable("gtna.machine.pattern_buffer.derived_mode_short",
                compactDisplay(getSelectedConfig() == null ? "" : getSelectedConfig().getDerivedModeId(), 22)))
                .asWidget().pos(8, 136));
        panel.child(Text.dynamic(() -> Component.translatable("gtna.machine.pattern_buffer.mode_button.current",
                getSelectedModeButtonText())).asWidget().pos(8, 150));

        panel.child(actionButton(syncManager, "gtna_cycle_mode", "↻", 8, 168,
                "gtna.machine.pattern_buffer.mode_button.tooltip"));
        panel.child(actionButton(syncManager, "gtna_clear_specialization", "×", 38, 168,
                "gtna.machine.pattern_buffer.clear_specialization"));
        panel.child(actionButton(syncManager, "gtna_clear_cache", "C", 68, 168,
                "gtna.machine.pattern_buffer.clear_cache"));
        return panel;
    }

    private ButtonWidget<?> actionButton(PanelSyncManager syncManager, String action, String label,
                                         int x, int y, String tooltipKey) {
        return new ButtonWidget<>().size(24, 18).pos(x, y)
                .onMousePressed((context, button) -> {
                    if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                        syncManager.callSyncedAction(action);
                        return true;
                    }
                    return false;
                })
                .overlay(Text.str(label).asIcon().size(14))
                .tooltip(new RichTooltip().addLine(Text.lang(tooltipKey)));
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        int rows = Math.max(1, (int) Math.ceil(maxPatternCount / 9.0));
        SlotGroup patternSlots = new SlotGroup("gtna_pattern_slots", 9, 0, true);
        BooleanSyncValue isOnlineValue = new BooleanSyncValue(this::isOnline, this::setOnline);
        syncManager.syncValue("gtna_is_online", isOnlineValue);

        var flow = Flow.col().coverChildren();
        flow.child(Text.dynamic(() -> isOnlineValue.getBoolValue() ?
                        Component.translatable("gtceu.gui.me_network.online") :
                        Component.translatable("gtceu.gui.me_network.offline"))
                .asWidget().marginTop(2).marginBottom(4));
        flow.child(new Grid().height(18 * rows).minElementMargin(0, 0)
                .minColWidth(18).minRowHeight(18).leftRel(0.5f)
                .gridOfSizeWidth(maxPatternCount, Math.min(9, maxPatternCount), (x, y, index) -> new ItemSlot()
                        .slot(SyncHandlers.itemSlot(patternInventory, index)
                                .slotGroup(patternSlots)
                                .accessibility(true, true)
                                .filter(AEItems.PROCESSING_PATTERN::is)
                                .changeListener((inserted, removed, changed, init) -> onPatternChange(index)))
                        .background(GTGuiTextures.SLOT, GTGuiTextures.PATTERN_OVERLAY)
                        .tooltip(new RichTooltip().addLine(Text.lang(
                                "gtna.machine.pattern_buffer.middle_click_hint")))));
        mainWidget.child(flow.center());
    }

    private void selectSlot(int slot) {
        this.selectedSlot = Math.max(0, Math.min(maxPatternCount - 1, slot));
        refreshSelectedConfigPreview();
    }

    private @Nullable GTNAPatternBufferSlotConfig getSelectedConfig() {
        return selectedSlot >= 0 && selectedSlot < slotConfigs.length ? slotConfigs[selectedSlot] : null;
    }

    private void clearSelectedSpecialization() {
        GTNAPatternBufferSlotConfig config = getSelectedConfig();
        if (config != null) {
            config.clearSpecialization();
        }
    }

    private void clearSelectedRecipeCache() {
        GTNAPatternBufferSlotConfig config = getSelectedConfig();
        if (config != null) {
            config.clearRecipeCache();
            clearPatternRecipeMetadata(selectedSlot);
            if (selectedSlot >= 0) {
                needPatternSync = true;
                refreshSelectedConfigPreview();
                markAsChanged();
            }
        }
    }

    private void refreshSelectedConfigPreview() {
        // MUI2 sync handlers read the selected configuration directly.
    }

    private void loadPatternRecipeMetadata(int slot, ItemStack pattern) {
        if (slot < 0 || slot >= slotConfigs.length || pattern.isEmpty()) {
            return;
        }
        CompoundTag tag = pattern.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        GTNAPatternBufferSlotConfig config = slotConfigs[slot];
        if (tag.contains(PATTERN_RECIPE_ID_TAG, Tag.TAG_STRING)) {
            config.setCachedRecipeId(tag.getString(PATTERN_RECIPE_ID_TAG));
        }
        if (config.getPreferredModeId().isBlank() && tag.contains(PATTERN_MODE_ID_TAG, Tag.TAG_STRING)) {
            config.setDerivedModeId(tag.getString(PATTERN_MODE_ID_TAG));
        }
    }

    private void clearPatternRecipeMetadata(int slot) {
        if (slot < 0 || slot >= patternInventory.getSlots()) {
            return;
        }
        ItemStack pattern = patternInventory.getStackInSlot(slot);
        if (pattern.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, pattern, tag -> {
            tag.remove(PATTERN_RECIPE_ID_TAG);
            tag.remove(PATTERN_MODE_ID_TAG);
        });
    }

    private void cacheResolvedRecipe(int slot, GTRecipe recipe) {
        if (slot < 0 || slot >= slotConfigs.length || recipe.id == null) {
            return;
        }
        GTNAPatternBufferSlotConfig config = slotConfigs[slot];
        config.setCachedRecipeId(recipe.id.toString());
        String resolvedMode = config.getPreferredModeId().isBlank() ? resolveDerivedMode(recipe) :
                config.getPreferredModeId();
        if (config.getPreferredModeId().isBlank()) {
            config.setDerivedModeId(resolvedMode == null ? "" : resolvedMode);
        }
        persistPatternRecipeMetadata(slot, recipe, resolvedMode);
    }

    private void persistPatternRecipeMetadata(int slot, GTRecipe recipe, @Nullable String modeId) {
        if (slot < 0 || slot >= patternInventory.getSlots() || recipe.id == null) {
            return;
        }
        ItemStack pattern = patternInventory.getStackInSlot(slot);
        if (pattern.isEmpty()) {
            return;
        }
        CustomData.update(DataComponents.CUSTOM_DATA, pattern, tag -> {
            tag.putString(PATTERN_RECIPE_ID_TAG, recipe.id.toString());
            if (modeId == null || modeId.isBlank()) {
                tag.remove(PATTERN_MODE_ID_TAG);
            } else {
                tag.putString(PATTERN_MODE_ID_TAG, modeId);
            }
        });
    }

    private void resolveAndCacheSlotRecipe(int slot) {
        if (slot < 0 || slot >= maxPatternCount || isRemote()) {
            return;
        }
        ItemStack pattern = patternInventory.getStackInSlot(slot);
        if (pattern.isEmpty()) {
            return;
        }

        com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder holder = null;
        GTRecipeType[] recipeTypes = new GTRecipeType[0];
        if (isFormed() && !getControllers().isEmpty()) {
            MultiblockControllerMachine controller = getControllers().first();
            if (controller instanceof IRecipeLogicMachine recipeMachine &&
                    controller instanceof com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder controllerHolder) {
                holder = controllerHolder;
                recipeTypes = recipeMachine.getRecipeTypes();
                if (recipeTypes == null || recipeTypes.length == 0) {
                    recipeTypes = new GTRecipeType[] { recipeMachine.getRecipeType() };
                }
            }
        }

        if (holder == null &&
                this instanceof com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder selfHolder) {
            holder = selfHolder;
        }
        if (holder == null) {
            return;
        }

        GTRecipeType[] searchTypes = getRecipeTypesForSlotSearch(slot, recipeTypes);
        GTRecipe resolved = findPatternResolvedRecipeForSlot(slot, holder, searchTypes);
        if (resolved == null) {
            resolved = findResolvedRecipeForSlot(slot, holder, searchTypes);
        }
        if (resolved == null) {
            GTRecipeType[] fallbackTypes = getGlobalRecipeTypesForSlotSearch(slot, searchTypes);
            resolved = findPatternResolvedRecipeForSlot(slot, holder, fallbackTypes);
            if (resolved == null) {
                resolved = findResolvedRecipeForSlot(slot, holder, fallbackTypes);
            }
        }
        if (resolved != null) {
            GTNAPatternBufferSlotConfig config = slotConfigs[slot];
            if (!config.getPreferredModeId().isBlank() && !matchesModeId(config.getPreferredModeId(), resolved)) {
                GTNACORE.LOGGER.warn(
                        "[GTNA][PatternBuffer] slot={} clearing stale preferred mode {} because detected recipe {} belongs to {}",
                        slot,
                        config.getPreferredModeId(),
                        resolved.id,
                        resolved.getType() == null || resolved.getType().registryName == null ? "unknown" :
                                resolved.getType().registryName);
                config.setPreferredModeId("");
            }
            cacheResolvedRecipe(slot, resolved);
            GTNACORE.LOGGER.info("[GTNA][PatternBuffer] slot={} detected recipe={} mode={} preferred={}",
                    slot,
                    resolved.id,
                    slotConfigs[slot].getDerivedModeId(),
                    slotConfigs[slot].getPreferredModeId());
            notifyControllerModeChange(slot, resolved);
        } else {
            logPatternDetectionFailure(slot, pattern, searchTypes);
        }
    }

    /**
     * Notifica o controller do multibloco para trocar o activeRecipeType
     * imediatamente quando um pattern
     * ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©
     * inserido e a
     * receita
     * ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©
     * resolvida.
     * Isso garante que o multibloco
     * jÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Â ÃƒÂ¢Ã¢â€šÂ¬Ã¢â€žÂ¢ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â‚¬Å¾Ã‚Â¢ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¡ÃƒÆ’Ã†â€™ÃƒÂ¢Ã¢â€šÂ¬Ã…Â¡ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¡
     * esteja no modo
     * correto ANTES da receita
     * executar.
     */
    private void notifyControllerModeChange(int slot, GTRecipe recipe) {
        if (!isFormed() || getControllers().isEmpty()) return;
        MultiblockControllerMachine controller = getControllers().first();

        GTNAPatternBufferSlotConfig config = slotConfigs[slot];
        String modeId = !config.getPreferredModeId().isBlank() ? config.getPreferredModeId() :
                config.getDerivedModeId();

        if (modeId == null || modeId.isBlank()) return;

        if (controller instanceof IPatternBufferModeHost host) {
            host.gtna$applyPatternBufferMode(modeId, recipe);
        } else if (controller instanceof IRecipeLogicMachine recipeMachine) {
            var recipeTypes = recipeMachine.getRecipeTypes();
            if (recipeTypes != null && recipeTypes.length > 1) {
                for (int i = 0; i < recipeTypes.length; i++) {
                    if (modeMatches(modeId, recipeTypes[i])) {
                        if (recipeMachine.getActiveRecipeType() != i) {
                            recipeMachine.setActiveRecipeType(i);
                        }
                        return;
                    }
                }
            }
        }
    }

    private GTRecipeType[] getRecipeTypesForSlotSearch(int slot, GTRecipeType[] recipeTypes) {
        if (recipeTypes == null || recipeTypes.length == 0) {
            return recipeTypes;
        }
        GTNAPatternBufferSlotConfig config = slotConfigs[slot];
        if (config.getPreferredModeId().isBlank() || recipeTypes.length <= 1) {
            return recipeTypes;
        }
        List<GTRecipeType> ordered = new ArrayList<>(recipeTypes.length);
        for (GTRecipeType recipeType : recipeTypes) {
            if (modeMatches(config.getPreferredModeId(), recipeType)) {
                ordered.add(recipeType);
            }
        }
        return ordered.isEmpty() ? recipeTypes : ordered.toArray(GTRecipeType[]::new);
    }

    private GTRecipeType[] getGlobalRecipeTypesForSlotSearch(int slot, GTRecipeType[] alreadySearched) {
        GTNAPatternBufferSlotConfig config = slotConfigs[slot];
        List<GTRecipeType> ordered = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        if (alreadySearched != null) {
            for (GTRecipeType recipeType : alreadySearched) {
                if (recipeType != null && recipeType.registryName != null) {
                    seen.add(recipeType.registryName.toString());
                }
            }
        }

        for (GTRecipeType recipeType : GTRegistries.RECIPE_TYPES) {
            if (recipeType == null || recipeType.registryName == null) {
                continue;
            }
            String id = recipeType.registryName.toString();
            if (seen.contains(id)) {
                continue;
            }
            if (!config.getPreferredModeId().isBlank() && !modeMatches(config.getPreferredModeId(), recipeType)) {
                continue;
            }
            ordered.add(recipeType);
        }
        return ordered.toArray(GTRecipeType[]::new);
    }

    private @Nullable GTRecipe findResolvedRecipeForSlot(int slot,
                                                         com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder holder,
                                                         GTRecipeType[] recipeTypes) {
        String cachedRecipeId = slotConfigs[slot].getCachedRecipeId();
        GTRecipe fallback = null;
        for (GTRecipeType recipeType : recipeTypes) {
            if (recipeType == null) {
                continue;
            }
            var iterator = recipeType.searchRecipe(holder, recipe -> true);
            int searchLimit = 256;
            while (iterator.hasNext() && searchLimit-- > 0) {
                GTRecipe recipe = iterator.next();
                if (recipe == null || !matchesSlot(slot, recipe)) {
                    continue;
                }
                if (recipe.id != null && recipe.id.toString().equals(cachedRecipeId)) {
                    return recipe;
                }
                if (fallback == null) {
                    fallback = recipe;
                }
            }
        }
        return fallback;
    }

    private @Nullable GTRecipe findPatternResolvedRecipeForSlot(int slot,
                                                                com.gregtechceu.gtceu.api.capability.recipe.IRecipeCapabilityHolder holder,
                                                                GTRecipeType[] recipeTypes) {
        ItemStack pattern = patternInventory.getStackInSlot(slot);
        IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, getLevel());
        if (details == null || getLevel() == null) {
            GTNACORE.LOGGER.info("[GTNA][PatternBuffer] slot={} failed to decode pattern item={}",
                    slot, pattern.getItem().getDescriptionId());
            return null;
        }
        String cachedRecipeId = slotConfigs[slot].getCachedRecipeId();
        GTRecipe fallback = null;
        int scanned = 0;
        for (GTRecipeType recipeType : recipeTypes) {
            if (recipeType == null) {
                continue;
            }
            int searchLimit = 512;
            for (var recipeHolder : getLevel().getRecipeManager().getAllRecipesFor(recipeType)) {
                GTRecipe recipe = recipeHolder.value();
                if (searchLimit-- <= 0) {
                    break;
                }
                scanned++;
                if (recipe == null || !matchesPatternDetails(slot, recipe, details)) {
                    continue;
                }
                if (recipe.id != null && recipe.id.toString().equals(cachedRecipeId)) {
                    GTNACORE.LOGGER.info(
                            "[GTNA][PatternBuffer] slot={} matched cached recipe={} after scanning {} recipes",
                            slot, recipe.id, scanned);
                    return recipe;
                }
                if (fallback == null) {
                    fallback = recipe;
                }
            }
        }
        if (fallback != null) {
            GTNACORE.LOGGER.info("[GTNA][PatternBuffer] slot={} matched recipe={} after scanning {} recipes",
                    slot, fallback.id, scanned);
        } else {
            GTNACORE.LOGGER.info(
                    "[GTNA][PatternBuffer] slot={} scanned {} recipes but found no match for pattern inputs={} fluids={} outputs={} fluidOutputs={}",
                    slot,
                    scanned,
                    summarizeItemStacks(collectPatternItemInputs(details)),
                    summarizeFluidStacks(collectPatternFluidInputs(details)),
                    summarizeItemStacks(collectPatternItemOutputs(details)),
                    summarizeFluidStacks(collectPatternFluidOutputs(details)));
        }
        return fallback;
    }

    private void logPatternDetectionFailure(int slot, ItemStack pattern, GTRecipeType[] recipeTypes) {
        List<String> typeIds = new ArrayList<>();
        if (recipeTypes != null) {
            for (GTRecipeType recipeType : recipeTypes) {
                if (recipeType != null && recipeType.registryName != null) {
                    typeIds.add(recipeType.registryName.toString());
                }
            }
        }
        GTNACORE.LOGGER.warn(
                "[GTNA][PatternBuffer] slot={} no mode detected for pattern={} preferred={} cachedRecipe={} triedTypes={}",
                slot,
                pattern.getItem().getDescriptionId(),
                slotConfigs[slot].getPreferredModeId(),
                slotConfigs[slot].getCachedRecipeId(),
                typeIds);
    }

    private void onSelectedConfigWidgetChanged() {
        refreshSelectedConfigPreview();
    }

    private List<ModeOption> getAvailableModeOptions() {
        List<ModeOption> options = new ArrayList<>();
        options.add(new ModeOption("", Component.translatable("gtna.machine.pattern_buffer.mode.auto")));

        Set<String> seen = new LinkedHashSet<>();
        for (String modeId : getCachedAvailableModeIds()) {
            if (seen.add(modeId)) {
                options.add(new ModeOption(modeId, formatModeLabel(modeId)));
            }
        }
        if (isFormed() && !getControllers().isEmpty()) {
            MultiblockControllerMachine controller = getControllers().first();
            if (controller instanceof IRecipeLogicMachine recipeMachine) {
                GTRecipeType[] recipeTypes = recipeMachine.getRecipeTypes();
                if (recipeTypes == null || recipeTypes.length == 0) {
                    recipeTypes = new GTRecipeType[] { recipeMachine.getRecipeType() };
                }
                for (GTRecipeType recipeType : recipeTypes) {
                    if (recipeType == null || recipeType.registryName == null) {
                        continue;
                    }
                    String id = recipeType.registryName.toString();
                    if (seen.add(id)) {
                        options.add(new ModeOption(id, formatModeLabel(recipeType)));
                    }
                }
            }
        }

        GTNAPatternBufferSlotConfig config = getSelectedConfig();
        if (config != null && !config.getPreferredModeId().isBlank() && seen.add(config.getPreferredModeId())) {
            options.add(new ModeOption(config.getPreferredModeId(),
                    Component.translatable("gtna.machine.pattern_buffer.mode.legacy",
                            compactDisplay(config.getPreferredModeId(), 18))));
        }
        if (config != null && config.getPreferredModeId().isBlank() && !config.getDerivedModeId().isBlank() &&
                seen.add(config.getDerivedModeId())) {
            options.add(new ModeOption(config.getDerivedModeId(), formatModeLabel(config.getDerivedModeId())));
        }
        return options;
    }

    private void cycleSelectedMode() {
        GTNAPatternBufferSlotConfig config = getSelectedConfig();
        if (config == null) {
            return;
        }
        List<ModeOption> options = getAvailableModeOptions();
        int currentIndex = getCurrentModeOptionIndex(options, config.getPreferredModeId());
        ModeOption next = options.get((currentIndex + 1) % options.size());
        config.setPreferredModeId(next.id());
    }

    private int getCurrentModeOptionIndex(List<ModeOption> options, String preferredModeId) {
        String current = preferredModeId == null ? "" : preferredModeId.trim();
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).id(), current)) {
                return i;
            }
        }
        return 0;
    }

    private Component getSelectedModeButtonText() {
        GTNAPatternBufferSlotConfig config = getSelectedConfig();
        if (config == null) {
            return Component.translatable("gtna.machine.pattern_buffer.mode.none");
        }
        List<ModeOption> options = getAvailableModeOptions();
        return options.get(getCurrentModeOptionIndex(options, config.getPreferredModeId())).label();
    }

    private void refreshModeSelector() {
        // Dynamic MUI2 text reflects mode changes without rebuilding widgets.
    }

    private static String compactDisplay(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.length() <= maxLength ? value : value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private @Nullable String resolveDerivedMode(GTRecipe recipe) {
        if (!isFormed() || getControllers().isEmpty()) {
            return null;
        }
        MultiblockControllerMachine controller = getControllers().first();
        if (controller instanceof IPatternBufferModeHost host) {
            return host.gtna$resolvePatternBufferMode(recipe);
        }
        if (controller instanceof IRecipeLogicMachine recipeMachine &&
                recipeMachine.getRecipeTypes() != null &&
                recipeMachine.getRecipeTypes().length > 1 &&
                recipe.getType() != null &&
                recipe.getType().registryName != null) {
            return recipe.getType().registryName.toString();
        }
        return null;
    }

    private static boolean matchesPreferredMode(GTNAPatternBufferSlotConfig config, GTRecipe recipe) {
        return matchesModeId(config.getPreferredModeId(), recipe);
    }

    private static boolean matchesDerivedMode(GTNAPatternBufferSlotConfig config, GTRecipe recipe) {
        return matchesModeId(config.getDerivedModeId(), recipe);
    }

    private static boolean matchesModeId(String modeId, GTRecipe recipe) {
        if (modeId == null || modeId.isBlank() || recipe.getType() == null || recipe.getType().registryName == null) {
            return false;
        }
        return modeMatches(modeId, recipe.getType());
    }

    private static boolean modeMatches(String modeId, @Nullable GTRecipeType recipeType) {
        if (modeId == null || modeId.isBlank() || recipeType == null || recipeType.registryName == null) {
            return false;
        }
        String requested = modeId.trim().toLowerCase(Locale.ROOT);
        String fullId = recipeType.registryName.toString().toLowerCase(Locale.ROOT);
        String path = recipeType.registryName.getPath().toLowerCase(Locale.ROOT);
        String requestedNormalized = requested.replace('_', '/');
        String pathNormalized = path.replace('_', '/');
        if (requested.equals(fullId) || requested.equals(path) ||
                requestedNormalized.equals(fullId) || requestedNormalized.equals(pathNormalized)) {
            return true;
        }
        if (path.endsWith("_" + requested) || path.endsWith("/" + requested) ||
                pathNormalized.endsWith("/" + requestedNormalized)) {
            return true;
        }
        return ("saw".equals(requested) || "cutting_saw".equals(requested)) &&
                (path.contains("cutter") || path.contains("saw"));
    }

    private static Component formatModeLabel(GTRecipeType recipeType) {
        if (recipeType == null || recipeType.registryName == null) {
            return Component.literal("-");
        }
        return recipeType.getName();
    }

    private static Component formatModeLabel(String modeId) {
        if (modeId == null || modeId.isBlank()) {
            return Component.literal("-");
        }
        for (GTRecipeType recipeType : GTRegistries.RECIPE_TYPES) {
            if (recipeType != null && recipeType.registryName != null &&
                    modeId.equals(recipeType.registryName.toString())) {
                return recipeType.getName();
            }
        }
        return Component.translatable("gtna.machine.pattern_buffer.mode.legacy", compactDisplay(modeId, 18));
    }

    private List<String> getCachedAvailableModeIds() {
        if (availableModeIds == null || availableModeIds.isBlank()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String token : availableModeIds.split("\\|")) {
            String trimmed = token == null ? "" : token.trim();
            if (!trimmed.isBlank()) {
                ids.add(trimmed);
            }
        }
        return ids;
    }

    private void refreshAvailableModesCache() {
        Set<String> ids = new LinkedHashSet<>();
        if (isFormed() && !getControllers().isEmpty()) {
            for (MultiblockControllerMachine controller : getControllers()) {
                if (!(controller instanceof IRecipeLogicMachine recipeMachine)) {
                    continue;
                }
                GTRecipeType[] recipeTypes = recipeMachine.getRecipeTypes();
                if (recipeTypes == null || recipeTypes.length == 0) {
                    recipeTypes = new GTRecipeType[] { recipeMachine.getRecipeType() };
                }
                for (GTRecipeType recipeType : recipeTypes) {
                    if (recipeType != null && recipeType.registryName != null) {
                        ids.add(recipeType.registryName.toString());
                    }
                }
            }
        }
        String newAvailableModeIds = String.join("|", ids);
        if (!Objects.equals(availableModeIds, newAvailableModeIds)) {
            availableModeIds = newAvailableModeIds;
            getSyncDataHolder().markClientSyncFieldDirty("availableModeIds");
        }
    }

    private @Nullable SlotMatch findMatchingSlot(GTRecipe recipe) {
        String recipeId = recipe.id == null ? "" : recipe.id.toString();
        for (int i = 0; i < maxPatternCount; i++) {
            GTNAPatternBufferSlotConfig config = slotConfigs[i];
            if (!recipeId.isBlank() && recipeId.equals(config.getCachedRecipeId())) {
                return new SlotMatch(i);
            }
        }
        for (int i = 0; i < maxPatternCount; i++) {
            GTNAPatternBufferSlotConfig config = slotConfigs[i];
            if (!config.getPreferredModeId().isBlank() && !matchesPreferredMode(config, recipe)) {
                continue;
            }
            if (config.getPreferredModeId().isBlank() && !config.getDerivedModeId().isBlank() &&
                    !matchesDerivedMode(config, recipe)) {
                continue;
            }
            IPatternDetails details = getPatternDetailsForSlot(i);
            if (details != null && matchesPatternDetails(i, recipe, details)) {
                return new SlotMatch(i);
            }
            if (matchesSlot(i, recipe)) {
                return new SlotMatch(i);
            }
        }
        return null;
    }

    private @Nullable IPatternDetails getPatternDetailsForSlot(int slot) {
        if (slot < 0 || slot >= patternInventory.getSlots()) {
            return null;
        }
        ItemStack pattern = patternInventory.getStackInSlot(slot);
        if (pattern.isEmpty()) {
            return null;
        }
        return PatternDetailsHelper.decodePattern(pattern, getLevel());
    }

    private boolean matchesSlot(int slotIndex, GTRecipe recipe) {
        List<SizedIngredient> itemInputs = copyItemInputs(recipe);
        List<SizedFluidIngredient> fluidInputs = copyFluidInputs(recipe);
        itemInputs = consumeCircuitInventory(itemInputs);
        itemInputs = consumeVirtualItems(slotConfigs[slotIndex], itemInputs);
        fluidInputs = consumeVirtualFluids(slotConfigs[slotIndex], fluidInputs);
        itemInputs = internalInventory[slotIndex].handleItemInternal(itemInputs, true);
        fluidInputs = internalInventory[slotIndex].handleFluidInternal(fluidInputs, true);
        boolean itemsMatched = itemInputs == null || itemInputs.isEmpty();
        boolean fluidsMatched = fluidInputs == null || fluidInputs.isEmpty();
        return itemsMatched && fluidsMatched;
    }

    private boolean matchesPatternDetails(int slotIndex, GTRecipe recipe, IPatternDetails details) {
        List<SizedIngredient> itemInputs = copyItemInputs(recipe);
        List<SizedFluidIngredient> fluidInputs = copyFluidInputs(recipe);
        List<ItemStack> itemOutputs = copyItemOutputs(recipe);
        List<FluidStack> fluidOutputs = copyFluidOutputs(recipe);

        GTNAPatternBufferSlotConfig config = slotConfigs[slotIndex];
        itemInputs = consumeConfiguredCircuit(config, itemInputs);
        itemInputs = consumeVirtualItems(config, itemInputs);
        fluidInputs = consumeVirtualFluids(config, fluidInputs);
        itemInputs = consumePatternItems(collectPatternItemInputs(details), itemInputs);
        fluidInputs = consumePatternFluids(collectPatternFluidInputs(details), fluidInputs);

        boolean inputsMatched = (itemInputs == null || itemInputs.isEmpty()) &&
                (fluidInputs == null || fluidInputs.isEmpty());
        if (!inputsMatched) {
            return false;
        }

        List<ItemStack> patternItemOutputs = collectPatternItemOutputs(details);
        List<FluidStack> patternFluidOutputs = collectPatternFluidOutputs(details);
        return compareItemStacks(itemOutputs, patternItemOutputs) &&
                compareFluidStacks(fluidOutputs, patternFluidOutputs);
    }

    private List<SizedIngredient> copyItemInputs(GTRecipe recipe) {
        List<SizedIngredient> copied = new ArrayList<>();
        for (Content content : recipe.getInputContents(ItemRecipeCapability.CAP)) {
            Object inner = content.content();
            if (inner instanceof SizedIngredient ingredient) {
                copied.add(SizedIngredientExtensions.copy(ingredient));
            } else if (inner instanceof ItemStack stack && !stack.isEmpty()) {
                copied.add(SizedIngredient.of(stack.getItem(), stack.getCount()));
            }
        }
        return copied;
    }

    private List<ItemStack> copyItemOutputs(GTRecipe recipe) {
        List<ItemStack> copied = new ArrayList<>();
        for (Content content : recipe.getOutputContents(ItemRecipeCapability.CAP)) {
            Object inner = content.content();
            if (inner instanceof ItemStack stack && !stack.isEmpty()) {
                copied.add(stack.copy());
            } else if (inner instanceof SizedIngredient ingredient) {
                ItemStack[] items = ingredient.getItems();
                if (items.length > 0 && !items[0].isEmpty()) {
                    copied.add(items[0].copy());
                }
            }
        }
        return copied;
    }

    private List<SizedFluidIngredient> copyFluidInputs(GTRecipe recipe) {
        List<SizedFluidIngredient> copied = new ArrayList<>();
        for (Content content : recipe.getInputContents(FluidRecipeCapability.CAP)) {
            Object inner = content.content();
            if (inner instanceof SizedFluidIngredient ingredient) {
                copied.add(SizedIngredientExtensions.copy(ingredient));
            } else if (inner instanceof FluidStack stack && !stack.isEmpty()) {
                copied.add(SizedFluidIngredient.of(stack.copy()));
            }
        }
        return copied;
    }

    private List<FluidStack> copyFluidOutputs(GTRecipe recipe) {
        List<FluidStack> copied = new ArrayList<>();
        for (Content content : recipe.getOutputContents(FluidRecipeCapability.CAP)) {
            Object inner = content.content();
            if (inner instanceof SizedFluidIngredient ingredient) {
                FluidStack[] stacks = ingredient.getFluids();
                if (stacks.length > 0 && !stacks[0].isEmpty()) {
                    copied.add(stacks[0].copy());
                }
            } else if (inner instanceof FluidStack stack && !stack.isEmpty()) {
                copied.add(stack.copy());
            }
        }
        return copied;
    }

    private List<SizedIngredient> consumeCircuitInventory(List<SizedIngredient> left) {
        if (left == null || left.isEmpty() || !getCircuitSlot().isEnabled()) {
            return left;
        }
        ItemStack circuitStack = getCircuitSlot().storage.getStackInSlot(0);
        if (circuitStack.isEmpty()) {
            return left;
        }
        return consumeVirtualItemList(List.of(circuitStack), left);
    }

    private List<SizedIngredient> consumeConfiguredCircuit(GTNAPatternBufferSlotConfig config,
                                                            List<SizedIngredient> left) {
        if (left == null || left.isEmpty()) {
            return left;
        }
        ItemStack circuitStack = config.getCircuitStack();
        if (circuitStack == null || circuitStack.isEmpty()) {
            return left;
        }
        return consumeVirtualItemList(List.of(circuitStack), left);
    }

    private List<SizedIngredient> consumeVirtualItems(GTNAPatternBufferSlotConfig config,
                                                       List<SizedIngredient> left) {
        if (left == null || left.isEmpty()) {
            return left;
        }
        return consumeVirtualItemList(config.getVirtualItemStacks(), left);
    }

    private List<SizedIngredient> consumeVirtualItemList(List<ItemStack> virtualStacks,
                                                          List<SizedIngredient> left) {
        if (virtualStacks.isEmpty()) {
            return left;
        }
        for (var it = left.listIterator(); it.hasNext();) {
            SizedIngredient ingredient = it.next();
            if (ingredient == null || ingredient.ingredient().hasNoItems()) {
                it.remove();
                continue;
            }
            int amountLeft = ingredient.count();
            if (amountLeft <= 0) {
                it.remove();
                continue;
            }
            for (ItemStack stack : virtualStacks) {
                if (stack.isEmpty() || !ingredient.test(stack)) {
                    continue;
                }
                amountLeft -= stack.getCount();
                if (amountLeft <= 0) {
                    it.remove();
                    break;
                }
            }
            if (amountLeft > 0) {
                it.set(SizedIngredientExtensions.copyWithCount(ingredient, amountLeft));
            }
        }
        return left;
    }

    private List<SizedIngredient> consumePatternItems(List<ItemStack> patternItems, List<SizedIngredient> left) {
        return consumeVirtualItemList(patternItems, left);
    }

    private List<SizedFluidIngredient> consumeVirtualFluids(GTNAPatternBufferSlotConfig config, List<SizedFluidIngredient> left) {
        if (left == null || left.isEmpty()) {
            return left;
        }
        List<FluidStack> configuredFluids = config.getVirtualFluidStacks();
        if (configuredFluids.isEmpty()) {
            return left;
        }
        for (var it = left.listIterator(); it.hasNext();) {
            SizedFluidIngredient ingredient = it.next();
            if (ingredient == null || ingredient.ingredient().hasNoFluids()) {
                it.remove();
                continue;
            }
            int amountLeft = ingredient.amount();
            for (FluidStack configuredFluid : configuredFluids) {
                if (configuredFluid.isEmpty() || !ingredient.test(configuredFluid)) {
                    continue;
                }
                amountLeft -= configuredFluid.getAmount();
                if (amountLeft <= 0) {
                    break;
                }
            }
            if (amountLeft <= 0) {
                it.remove();
            } else {
                it.set(SizedIngredientExtensions.copyWithAmount(ingredient, amountLeft));
            }
        }
        return left;
    }

    private List<SizedFluidIngredient> consumePatternFluids(List<FluidStack> patternFluids, List<SizedFluidIngredient> left) {
        if (left == null || left.isEmpty() || patternFluids.isEmpty()) {
            return left;
        }
        for (var it = left.listIterator(); it.hasNext();) {
            SizedFluidIngredient ingredient = it.next();
            if (ingredient == null || ingredient.ingredient().hasNoFluids()) {
                it.remove();
                continue;
            }
            int amountLeft = ingredient.amount();
            for (FluidStack patternFluid : patternFluids) {
                if (patternFluid.isEmpty() || !ingredient.test(patternFluid)) {
                    continue;
                }
                amountLeft -= patternFluid.getAmount();
                if (amountLeft <= 0) {
                    break;
                }
            }
            if (amountLeft <= 0) {
                it.remove();
            } else {
                it.set(SizedIngredientExtensions.copyWithAmount(ingredient, amountLeft));
            }
        }
        return left;
    }

    private List<ItemStack> collectPatternItemInputs(IPatternDetails details) {
        List<ItemStack> items = new ArrayList<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            if (input == null) {
                continue;
            }
            GenericStack selected = null;
            for (GenericStack candidate : input.getPossibleInputs()) {
                if (candidate != null && candidate.what() instanceof AEItemKey) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null || !(selected.what() instanceof AEItemKey itemKey)) {
                continue;
            }
            long amount = selected.amount() * Math.max(1L, input.getMultiplier());
            ItemStack stack = itemKey.toStack(GTMath.saturatedCast(amount));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    private List<FluidStack> collectPatternFluidInputs(IPatternDetails details) {
        List<FluidStack> fluids = new ArrayList<>();
        for (IPatternDetails.IInput input : details.getInputs()) {
            if (input == null) {
                continue;
            }
            GenericStack selected = null;
            for (GenericStack candidate : input.getPossibleInputs()) {
                if (candidate != null && candidate.what() instanceof AEFluidKey) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null || !(selected.what() instanceof AEFluidKey fluidKey)) {
                continue;
            }
            long amount = selected.amount() * Math.max(1L, input.getMultiplier());
            FluidStack stack = fluidKey.toStack(GTMath.saturatedCast(amount));
            if (!stack.isEmpty()) {
                fluids.add(stack);
            }
        }
        return fluids;
    }

    private List<ItemStack> collectPatternItemOutputs(IPatternDetails details) {
        List<ItemStack> items = new ArrayList<>();
        for (GenericStack output : details.getOutputs()) {
            if (output == null || !(output.what() instanceof AEItemKey itemKey)) {
                continue;
            }
            ItemStack stack = itemKey.toStack(GTMath.saturatedCast(output.amount()));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }
        return items;
    }

    private List<FluidStack> collectPatternFluidOutputs(IPatternDetails details) {
        List<FluidStack> fluids = new ArrayList<>();
        for (GenericStack output : details.getOutputs()) {
            if (output == null || !(output.what() instanceof AEFluidKey fluidKey)) {
                continue;
            }
            FluidStack stack = fluidKey.toStack(GTMath.saturatedCast(output.amount()));
            if (!stack.isEmpty()) {
                fluids.add(stack);
            }
        }
        return fluids;
    }

    private boolean compareItemStacks(List<ItemStack> expected, List<ItemStack> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        List<ItemStack> remaining = new ArrayList<>();
        for (ItemStack stack : actual) {
            if (!stack.isEmpty()) {
                remaining.add(stack.copy());
            }
        }
        for (ItemStack expectedStack : expected) {
            if (expectedStack.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (var it = remaining.listIterator(); it.hasNext();) {
                ItemStack actualStack = it.next();
                if (!ItemStack.isSameItemSameComponents(expectedStack, actualStack)) {
                    continue;
                }
                if (actualStack.getCount() != expectedStack.getCount()) {
                    continue;
                }
                it.remove();
                matched = true;
                break;
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    private boolean compareFluidStacks(List<FluidStack> expected, List<FluidStack> actual) {
        if (expected.size() != actual.size()) {
            return false;
        }
        List<FluidStack> remaining = new ArrayList<>();
        for (FluidStack stack : actual) {
            if (!stack.isEmpty()) {
                remaining.add(stack.copy());
            }
        }
        for (FluidStack expectedStack : expected) {
            if (expectedStack.isEmpty()) {
                continue;
            }
            boolean matched = false;
            for (var it = remaining.listIterator(); it.hasNext();) {
                FluidStack actualStack = it.next();
                if (!FluidStack.isSameFluidSameComponents(actualStack, expectedStack)) {
                    continue;
                }
                if (actualStack.getAmount() != expectedStack.getAmount()) {
                    continue;
                }
                it.remove();
                matched = true;
                break;
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    private String summarizeItemStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return "[]";
        }
        List<String> summary = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                summary.add(stack.getCount() + "x" + stack.getItem().getDescriptionId());
            }
        }
        return summary.toString();
    }

    private String summarizeFluidStacks(List<FluidStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return "[]";
        }
        List<String> summary = new ArrayList<>();
        for (FluidStack stack : stacks) {
            if (!stack.isEmpty()) {
                String fluidId = stack.getFluid().getFluidType().toString();
                summary.add(stack.getAmount() + "mb:" + fluidId);
            }
        }
        return summary.toString();
    }

    private record SlotMatch(int slot) {}

    private record ModeOption(String id, Component label) {}

    private final class SelectedConfigItemTransfer extends ItemStackTransfer {

        private SelectedConfigItemTransfer() {
            super(9);
        }

        @Override
        public int getSlots() {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            return config == null ? 9 : config.getSpecialItems().getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            return config == null ? ItemStack.EMPTY : config.getSpecialItems().getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            if (config != null) {
                config.getSpecialItems().setStackInSlot(slot, stack);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            if (config == null) {
                return stack;
            }
            return config.getSpecialItems().insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            if (config == null) {
                return ItemStack.EMPTY;
            }
            return config.getSpecialItems().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            return config == null ? 64 : config.getSpecialItems().getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !AEItems.PROCESSING_PATTERN.is(stack);
        }
    }

    private final class SelectedConfigFluidTank implements IFluidTank, IFluidHandler {

        private final int slot;

        private SelectedConfigFluidTank(int slot) {
            this.slot = slot;
        }

        private @Nullable net.neoforged.neoforge.fluids.capability.templates.FluidTank delegate() {
            GTNAPatternBufferSlotConfig config = getSelectedConfig();
            return config == null ? null : config.getSpecialFluids()[slot];
        }

        @Override
        public FluidStack getFluid() {
            var tank = delegate();
            return tank == null ? FluidStack.EMPTY : tank.getFluid();
        }

        @Override
        public int getFluidAmount() {
            var tank = delegate();
            return tank == null ? 0 : tank.getFluidAmount();
        }

        @Override
        public int getCapacity() {
            var tank = delegate();
            return tank == null ? Integer.MAX_VALUE : tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            var tank = delegate();
            return tank != null && tank.isFluidValid(stack);
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var tank = delegate();
            return tank == null ? 0 : tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            var tank = delegate();
            return tank == null ? FluidStack.EMPTY : tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            var tank = delegate();
            return tank == null ? FluidStack.EMPTY : tank.drain(maxDrain, action);
        }
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return detailsSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!isFormed() || !getMainNode().isActive() || !detailsSlotMap.containsKey(patternDetails) ||
                !checkInput(inputHolder)) {
            return false;
        }
        InternalSlot slot = detailsSlotMap.get(patternDetails);
        if (slot != null) {
            slot.pushPattern(patternDetails, inputHolder);
            int logicalSlot = getInternalSlotIndex(slot);
            if (logicalSlot >= 0) {
                resolveAndCacheSlotRecipe(logicalSlot);
            }
            return true;
        }
        return false;
    }

    private int getInternalSlotIndex(InternalSlot target) {
        for (int i = 0; i < internalInventory.length; i++) {
            if (internalInventory[i] == target) {
                return i;
            }
        }
        return -1;
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
            if (illegal) return false;
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
        if (isFormed()) {
            MultiblockControllerMachine controller = getControllers().first();
            MultiblockMachineDefinition controllerDefinition = controller.getDefinition();
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()),
                        Component.literal(customName),
                        Collections.emptyList());
            }
            ItemStack circuitStack = getCircuitSlot().isEnabled() ?
                    getCircuitSlot().storage.getStackInSlot(0) : ItemStack.EMPTY;
            int circuitConfiguration = circuitStack.isEmpty() ? -1 :
                    IntCircuitBehaviour.getCircuitConfiguration(circuitStack);
            Component groupName = circuitConfiguration != -1 ?
                    Component.translatable(controllerDefinition.getDescriptionId())
                            .append(" - " + circuitConfiguration) :
                    Component.translatable(controllerDefinition.getDescriptionId());
            return new PatternContainerGroup(
                    AEItemKey.of(controllerDefinition.asStack()),
                    groupName,
                    Collections.emptyList());
        }
        if (!customName.isEmpty()) {
            return new PatternContainerGroup(
                    AEItemKey.of(getDefinition().asStack()),
                    Component.literal(customName),
                    Collections.emptyList());
        }
        return new PatternContainerGroup(
                AEItemKey.of(getDefinition().asStack()),
                getDefinition().getItem().getDescription(),
                Collections.emptyList());
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        patternInventory.dropInventoryInWorld(getLevel(), getBlockPos());
        shareInventory.dropInventoryInWorld();
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        dataStick.set(GTDataComponents.DATA_COPY_POS, getBlockPos());
        return InteractionResult.SUCCESS;
    }

    public record BufferData(Object2LongMap<ItemStack> items, Object2LongMap<FluidStack> fluids) {}

    public BufferData mergeInternalSlots() {
        var items = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
        var fluids = new Object2LongOpenHashMap<FluidStack>();
        for (InternalSlot slot : internalInventory) {
            slot.itemInventory.object2LongEntrySet().fastForEach(e -> items.addTo(e.getKey(), e.getLongValue()));
            slot.fluidInventory.object2LongEntrySet().fastForEach(e -> fluids.addTo(e.getKey(), e.getLongValue()));
        }
        return new BufferData(items, fluids);
    }

    public class InternalSlot implements INBTSerializable<CompoundTag> {

        @Getter
        private Runnable onContentsChanged = () -> {};

        public void setOnContentsChanged(Runnable listener) {
            if (listener == null) return;
            Runnable previous = onContentsChanged;
            onContentsChanged = () -> {
                previous.run();
                listener.run();
            };
        }

        private final Object2LongOpenCustomHashMap<ItemStack> itemInventory = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        private final Object2LongOpenHashMap<FluidStack> fluidInventory = new Object2LongOpenHashMap<>();
        private @Nullable List<ItemStack> itemStacks;
        private @Nullable List<FluidStack> fluidStacks;

        public boolean isItemEmpty() {
            return itemInventory.isEmpty();
        }

        public boolean isFluidEmpty() {
            return fluidInventory.isEmpty();
        }

        public void onContentsChanged() {
            itemStacks = null;
            fluidStacks = null;
            onContentsChanged.run();
        }

        private void add(AEKey what, long amount) {
            if (amount <= 0L) return;
            if (what instanceof AEItemKey itemKey) {
                itemInventory.addTo(itemKey.toStack(), amount);
            } else if (what instanceof AEFluidKey fluidKey) {
                fluidInventory.addTo(fluidKey.toStack(1), amount);
            }
        }

        public List<ItemStack> getItems() {
            if (itemStacks == null) {
                itemStacks = new ArrayList<>();
                itemInventory.object2LongEntrySet().stream()
                        .map(e -> GTMath.splitStacks(e.getKey(), e.getLongValue()))
                        .forEach(itemStacks::addAll);
            }
            return itemStacks;
        }

        public List<FluidStack> getFluids() {
            if (fluidStacks == null) {
                fluidStacks = new ArrayList<>();
                fluidInventory.object2LongEntrySet().stream()
                        .map(e -> GTMath.splitFluidStacks(e.getKey(), e.getLongValue()))
                        .forEach(fluidStacks::addAll);
            }
            return fluidStacks;
        }

        public void refund() {
            IGrid network = getMainNode().getGrid();
            if (network == null) return;
            MEStorage networkInv = network.getStorageService().getInventory();
            var energy = network.getEnergyService();
            for (var it = itemInventory.object2LongEntrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                ItemStack stack = entry.getKey();
                long count = entry.getLongValue();
                if (stack.isEmpty() || count == 0) {
                    it.remove();
                    continue;
                }
                var key = AEItemKey.of(stack);
                if (key == null) continue;
                long inserted = StorageHelper.poweredInsert(energy, networkInv, key, count, actionSource);
                if (inserted > 0) {
                    count -= inserted;
                    if (count == 0) {
                        it.remove();
                    } else {
                        entry.setValue(count);
                    }
                }
            }
            for (var it = fluidInventory.object2LongEntrySet().iterator(); it.hasNext();) {
                var entry = it.next();
                FluidStack stack = entry.getKey();
                long amount = entry.getLongValue();
                if (stack.isEmpty() || amount == 0) {
                    it.remove();
                    continue;
                }
                var key = AEFluidKey.of(stack);
                if (key == null) continue;
                long inserted = StorageHelper.poweredInsert(energy, networkInv, key, amount, actionSource);
                if (inserted > 0) {
                    amount -= inserted;
                    if (amount == 0) {
                        it.remove();
                    } else {
                        entry.setValue(amount);
                    }
                }
            }
            onContentsChanged();
        }

        public void pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            patternDetails.pushInputsToExternalInventory(inputHolder, this::add);
            onContentsChanged();
        }

        public List<SizedIngredient> handleItemInternal(List<SizedIngredient> left, boolean simulate) {
            boolean changed = false;
            for (var it = left.listIterator(); it.hasNext();) {
                SizedIngredient ingredient = it.next();
                if (ingredient.ingredient().hasNoItems()) {
                    it.remove();
                    continue;
                }
                ItemStack[] items = ingredient.getItems();
                if (items.length == 0 || items[0].isEmpty()) {
                    it.remove();
                    continue;
                }
                int amount = ingredient.count();
                for (var it2 = itemInventory.object2LongEntrySet().iterator(); it2.hasNext();) {
                    var entry = it2.next();
                    ItemStack stack = entry.getKey();
                    long count = entry.getLongValue();
                    if (stack.isEmpty() || count == 0) {
                        it2.remove();
                        continue;
                    }
                    if (!ingredient.test(stack)) continue;
                    int extracted = Math.min(GTMath.saturatedCast(count), amount);
                    if (!simulate && extracted > 0) {
                        changed = true;
                        count -= extracted;
                        if (count == 0) {
                            it2.remove();
                        } else {
                            entry.setValue(count);
                        }
                    }
                    amount -= extracted;
                    if (amount <= 0) {
                        it.remove();
                        break;
                    }
                }
                if (amount > 0) {
                    it.set(SizedIngredientExtensions.copyWithCount(ingredient, amount));
                }
            }
            if (changed) onContentsChanged();
            return left;
        }

        public List<SizedFluidIngredient> handleFluidInternal(List<SizedFluidIngredient> left, boolean simulate) {
            boolean changed = false;
            for (var it = left.listIterator(); it.hasNext();) {
                SizedFluidIngredient ingredient = it.next();
                if (ingredient.ingredient().hasNoFluids()) {
                    it.remove();
                    continue;
                }
                FluidStack[] fluids = ingredient.getFluids();
                if (fluids.length == 0 || fluids[0].isEmpty()) {
                    it.remove();
                    continue;
                }
                int amount = ingredient.amount();
                for (var it2 = fluidInventory.object2LongEntrySet().iterator(); it2.hasNext();) {
                    var entry = it2.next();
                    FluidStack stack = entry.getKey();
                    long count = entry.getLongValue();
                    if (stack.isEmpty() || count == 0) {
                        it2.remove();
                        continue;
                    }
                    if (!ingredient.test(stack)) continue;
                    int extracted = Math.min(GTMath.saturatedCast(count), amount);
                    if (!simulate && extracted > 0) {
                        changed = true;
                        count -= extracted;
                        if (count == 0) {
                            it2.remove();
                        } else {
                            entry.setValue(count);
                        }
                    }
                    amount -= extracted;
                    if (amount <= 0) {
                        it.remove();
                        break;
                    }
                }
                if (amount > 0) {
                    it.set(SizedIngredientExtensions.copyWithAmount(ingredient, amount));
                }
            }
            if (changed) onContentsChanged();
            return left;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            for (var entry : itemInventory.object2LongEntrySet()) {
                CompoundTag ct = (CompoundTag) entry.getKey().save(provider);
                ct.putLong("real", entry.getLongValue());
                itemsTag.add(ct);
            }
            if (!itemsTag.isEmpty()) {
                tag.put("inventory", itemsTag);
            }
            ListTag fluidsTag = new ListTag();
            for (var entry : fluidInventory.object2LongEntrySet()) {
                CompoundTag ct = (CompoundTag) entry.getKey().save(provider);
                ct.putLong("real", entry.getLongValue());
                fluidsTag.add(ct);
            }
            if (!fluidsTag.isEmpty()) {
                tag.put("fluidInventory", fluidsTag);
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            itemInventory.clear();
            fluidInventory.clear();
            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            for (Tag t : items) {
                if (!(t instanceof CompoundTag ct)) continue;
                ItemStack stack = ItemStack.parseOptional(provider, ct);
                long count = ct.getLong("real");
                if (!stack.isEmpty() && count > 0) {
                    itemInventory.put(stack, count);
                }
            }
            ListTag fluids = tag.getList("fluidInventory", Tag.TAG_COMPOUND);
            for (Tag t : fluids) {
                if (!(t instanceof CompoundTag ct)) continue;
                FluidStack stack = FluidStack.parseOptional(provider, ct);
                long amount = ct.getLong("real");
                if (!stack.isEmpty() && amount > 0) {
                    fluidInventory.put(stack, amount);
                }
            }
            onContentsChanged();
        }
    }
}
