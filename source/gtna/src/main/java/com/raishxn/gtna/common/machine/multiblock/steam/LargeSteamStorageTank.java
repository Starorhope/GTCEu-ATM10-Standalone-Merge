package com.raishxn.gtna.common.machine.multiblock.steam;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.fluids.PropertyFluidFilter;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.MultiblockTankMachine;

import net.neoforged.neoforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

public class LargeSteamStorageTank extends MultiblockTankMachine {

    private static final int CAPACITY = 120_000_000;

    public LargeSteamStorageTank(BlockEntityCreationInfo holder, Object... args) {
        super(holder, CAPACITY, new SteamOnlyFilter());
    }

    private static final class SteamOnlyFilter extends PropertyFluidFilter {

        private SteamOnlyFilter() {
            super(1, true, false, false, false);
        }

        @Override
        public boolean test(@NotNull FluidStack stack) {
            return stack.getFluid() == GTMaterials.Steam.getFluid();
        }
    }
}
