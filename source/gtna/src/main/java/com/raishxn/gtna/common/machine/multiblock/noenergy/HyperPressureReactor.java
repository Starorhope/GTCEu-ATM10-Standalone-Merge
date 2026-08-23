package com.raishxn.gtna.common.machine.multiblock.noenergy;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.feature.ITieredMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;

import com.raishxn.gtna.common.data.GTNAMachines;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class HyperPressureReactor extends WorkableMultiblockMachine
                                  implements ITieredMachine, IMuiMachine {

    public HyperPressureReactor(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Override
    public int getTier() {
        return 3; // HV
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @Nonnull GTRecipe recipe) {
        if (machine instanceof HyperPressureReactor) {
            boolean isCompact = machine.getDefinition() == GTNAMachines.COMPACT_HYPER_PRESSURE_REACTOR;
            int maxParallel = isCompact ? 512 : 1;

            int parallels = ParallelLogic.getParallelAmountWithoutEU(machine, recipe, maxParallel);

            if (parallels == 0) return ModifierFunction.NULL;

            return ModifierFunction.builder()
                    .parallels(parallels)
                    .modifyAllContents(ContentModifier.multiplier(parallels))
                    .build();
        }
        return ModifierFunction.NULL;
    }

}
