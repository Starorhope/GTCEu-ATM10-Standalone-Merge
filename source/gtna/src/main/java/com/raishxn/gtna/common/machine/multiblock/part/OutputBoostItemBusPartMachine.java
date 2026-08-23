package com.raishxn.gtna.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;

import com.raishxn.gtna.api.machine.feature.GTNAOutputBoostItemPart;

public class OutputBoostItemBusPartMachine extends ItemBusPartMachine implements GTNAOutputBoostItemPart {

    public OutputBoostItemBusPartMachine(BlockEntityCreationInfo holder, int tier, Object... args) {
        super(holder, tier, IO.OUT);
    }

    @Override
    public int gtna$getOutputMultiplier() {
        return OutputBoostHatchPartMachine.getMultiplierForTier(getTier());
    }
}
