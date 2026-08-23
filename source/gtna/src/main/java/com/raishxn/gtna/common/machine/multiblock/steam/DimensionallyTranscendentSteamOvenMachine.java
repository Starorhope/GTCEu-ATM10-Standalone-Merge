package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

public class DimensionallyTranscendentSteamOvenMachine extends FixedThreadSteamParallelMachine {

    public DimensionallyTranscendentSteamOvenMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, GTRecipeTypes.FURNACE_RECIPES, 524288, 524288, 0.01, 2, args);
    }
}
