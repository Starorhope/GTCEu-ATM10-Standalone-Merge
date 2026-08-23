package com.raishxn.gtna.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import com.raishxn.gtna.api.machine.feature.GTNANoConsumeFluidPart;

public class InfiniteInputHatchPartMachine extends FluidHatchPartMachine implements GTNANoConsumeFluidPart {

    public InfiniteInputHatchPartMachine(BlockEntityCreationInfo holder, int tier, Object... args) {
        super(holder, tier, IO.IN, INITIAL_TANK_CAPACITY_1X, 1);
    }
}
