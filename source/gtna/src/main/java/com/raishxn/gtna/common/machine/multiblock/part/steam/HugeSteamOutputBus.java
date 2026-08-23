package com.raishxn.gtna.common.machine.multiblock.part.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import net.minecraft.MethodsReturnNonnullByDefault;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.SlotGroup;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HugeSteamOutputBus extends MultiblockPartMachine implements IMuiMachine {

    @SaveField
    public final NotifiableItemStackHandler inventory;

    public HugeSteamOutputBus(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
        this.inventory = attachTrait(new NotifiableItemStackHandler(64, IO.OUT, IO.OUT));
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        SlotGroup slotGroup = new SlotGroup("huge_steam_bus", 8, 0, true);
        mainWidget.child(new Grid().coverChildren().center().margin(7, 5)
                .gridOfSizeHeight(64, 8, (x, y, index) -> new ItemSlot()
                        .slot(SyncHandlers.itemSlot(inventory, index).slotGroup(slotGroup))));
    }
}
