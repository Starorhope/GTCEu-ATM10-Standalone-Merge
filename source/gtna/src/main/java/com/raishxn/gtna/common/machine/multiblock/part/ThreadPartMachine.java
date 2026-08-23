package com.raishxn.gtna.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;

import com.raishxn.gtna.api.machine.IThreadModifierMachine;
import com.raishxn.gtna.config.GTNABalance;

public class ThreadPartMachine extends TieredIOPartMachine implements IMuiMachine {

    private final int threadCount;

    public ThreadPartMachine(BlockEntityCreationInfo holder, int tier, Object... args) {
        super(holder, tier, IO.NONE);
        // Lógica Exponencial: 2^(Tier - 6) - 1
        // ZPM (Tier 7): 2^(1) - 1 = 1
        // UV (Tier 8): 2^(2) - 1 = 3
        // ...
        // MAX (Tier 14): 2^(8) - 1 = 255
        this.threadCount = GTNABalance.getThreadCount(tier);
    }

    public int getThreadCount() {
        return this.threadCount;
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        if (controller instanceof IThreadModifierMachine threadMachine) {
            threadMachine.setThreadPartMachine(this);
        }
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        if (controller instanceof IThreadModifierMachine threadMachine) {
            if (threadMachine.getThreadPartMachine() == this) {
                threadMachine.setThreadPartMachine(null);
            }
        }
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        mainWidget.child(Text.lang("gtna.machine.thread_hatch.ui.threads", getThreadCount()).asWidget().center());
    }
}
