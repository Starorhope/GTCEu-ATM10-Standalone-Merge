package com.raishxn.gtna.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.Direction;

public class InfiniteElectricSingleblockCover extends CoverBehavior {

    public InfiniteElectricSingleblockCover(CoverDefinition definition, ICoverable coverHolder,
                                            Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    public boolean canAttach() {
        if (!super.canAttach()) {
            return false;
        }
        MetaMachine machine = MetaMachine.getMachine(coverHolder.getLevel(), coverHolder.getBlockPos());
        return machine instanceof WorkableTieredMachine && !(machine instanceof MultiblockControllerMachine);
    }
}
