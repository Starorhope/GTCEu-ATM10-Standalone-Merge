package com.raishxn.gtna.utils; // Ajuste para o seu pacote

import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class MachineIO {

    public static boolean inputItem(WorkableMultiblockMachine machine, ItemStack item) {
        return MachineUtil.inputItem(machine, item);
    }

    public static boolean outputItem(WorkableMultiblockMachine machine, ItemStack item) {
        return MachineUtil.outputItem(machine, item);
    }

    public static boolean notConsumableItem(WorkableMultiblockMachine machine, ItemStack item) {
        return MachineUtil.notConsumableItem(machine, item);
    }

    public static boolean notConsumableCircuit(WorkableMultiblockMachine machine, int configuration) {
        return MachineUtil.notConsumableCircuit(machine, configuration);
    }

    public static boolean inputFluid(WorkableMultiblockMachine machine, FluidStack fluid) {
        return MachineUtil.inputFluid(machine, fluid);
    }

    public static boolean outputFluid(WorkableMultiblockMachine machine, FluidStack fluid) {
        return MachineUtil.outputFluid(machine, fluid);
    }

    public static boolean inputEU(WorkableMultiblockMachine machine, long eu) {
        return MachineUtil.inputEU(machine, eu);
    }
}
