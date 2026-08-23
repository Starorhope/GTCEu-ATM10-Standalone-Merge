package com.raishxn.gtna.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IFilteredHandler;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableRecipeHandlerTrait;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerGroupDistinctness;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GTNAPatternBufferRecipeHandler {

    @Getter
    private final List<RecipeHandlerList> slotHandlers;

    public GTNAPatternBufferRecipeHandler(GTNAMEPatternBufferPartMachine buffer,
                                          GTNAMEPatternBufferPartMachine.InternalSlot[] slots,
                                          GTNAPatternBufferSlotConfig[] configs) {
        this.slotHandlers = new ArrayList<>(slots.length);
        for (int i = 0; i < slots.length; i++) {
            slotHandlers.add(new SlotRHL(buffer, slots[i], configs[i], i));
        }
    }

    private static final class SlotRHL extends RecipeHandlerList {

        private SlotRHL(GTNAMEPatternBufferPartMachine buffer,
                        GTNAMEPatternBufferPartMachine.InternalSlot slot,
                        GTNAPatternBufferSlotConfig config,
                        int index) {
            super(IO.IN);
            addHandlers(buffer.getCircuitSlot(),
                    buffer.attachTrait(new SlotSpecialItemHandler(config, index)),
                    buffer.attachTrait(new SlotSpecialFluidHandler(config, index)),
                    buffer.attachTrait(new SlotItemRecipeHandler(slot, index)),
                    buffer.attachTrait(new SlotFluidRecipeHandler(slot, index)));
            setGroup(RecipeHandlerGroupDistinctness.BUS_DISTINCT);
        }

        @Override
        public boolean isDistinct() {
            return true;
        }

        @Override
        protected void setDistinct(boolean distinct, boolean notify) {}
    }

    @Getter
    private static final class SlotItemRecipeHandler extends NotifiableRecipeHandlerTrait<SizedIngredient> {

        private static final MachineTraitType<SlotItemRecipeHandler> TYPE =
                new MachineTraitType<>(SlotItemRecipeHandler.class, true);

        private final GTNAMEPatternBufferPartMachine.InternalSlot slot;
        private final int priority;
        private final int size = 81;
        private final RecipeCapability<SizedIngredient> capability = ItemRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotItemRecipeHandler(GTNAMEPatternBufferPartMachine.InternalSlot slot,
                                      int index) {
            super();
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;
            slot.setOnContentsChanged(this::notifyListeners);
        }

        @Override
        public MachineTraitType<SlotItemRecipeHandler> getTraitType() {
            return TYPE;
        }

        @Override
        public List<SizedIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SizedIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN || slot.isItemEmpty()) return left;
            return slot.handleItemInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(slot.getItems());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getItems().stream().mapToLong(ItemStack::getCount).sum();
        }
    }

    @Getter
    private static final class SlotFluidRecipeHandler extends NotifiableRecipeHandlerTrait<SizedFluidIngredient> {

        private static final MachineTraitType<SlotFluidRecipeHandler> TYPE =
                new MachineTraitType<>(SlotFluidRecipeHandler.class, true);

        private final GTNAMEPatternBufferPartMachine.InternalSlot slot;
        private final int priority;
        private final int size = 81;
        private final RecipeCapability<SizedFluidIngredient> capability = FluidRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotFluidRecipeHandler(GTNAMEPatternBufferPartMachine.InternalSlot slot,
                                       int index) {
            super();
            this.slot = slot;
            this.priority = IFilteredHandler.HIGH + index + 1;
            slot.setOnContentsChanged(this::notifyListeners);
        }

        @Override
        public MachineTraitType<SlotFluidRecipeHandler> getTraitType() {
            return TYPE;
        }

        @Override
        public List<SizedFluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SizedFluidIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN || slot.isFluidEmpty()) return left;
            return slot.handleFluidInternal(left, simulate);
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(slot.getFluids());
        }

        @Override
        public double getTotalContentAmount() {
            return slot.getFluids().stream().mapToLong(FluidStack::getAmount).sum();
        }
    }

    @Getter
    private static final class SlotSpecialItemHandler extends NotifiableRecipeHandlerTrait<SizedIngredient> {

        private static final MachineTraitType<SlotSpecialItemHandler> TYPE =
                new MachineTraitType<>(SlotSpecialItemHandler.class, true);

        private final GTNAPatternBufferSlotConfig config;
        private final int priority;
        private final int size = 10;
        private final RecipeCapability<SizedIngredient> capability = ItemRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotSpecialItemHandler(GTNAPatternBufferSlotConfig config,
                                       int index) {
            super();
            this.config = config;
            this.priority = IFilteredHandler.HIGH + 1000 + index;
            config.setOnContentsChanged(this::notifyListeners);
        }

        @Override
        public MachineTraitType<SlotSpecialItemHandler> getTraitType() {
            return TYPE;
        }

        @Override
        public List<SizedIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SizedIngredient> left,
                                                       boolean simulate) {
            if (io != IO.IN || left == null || left.isEmpty()) {
                return left;
            }

            List<ItemStack> virtualStacks = getVirtualStacks();
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
                    it.set(new SizedIngredient(ingredient.ingredient(), amountLeft));
                }
            }

            return left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(getVirtualStacks());
        }

        @Override
        public double getTotalContentAmount() {
            return getVirtualStacks().stream().mapToLong(ItemStack::getCount).sum();
        }

        private List<ItemStack> getVirtualStacks() {
            return new ArrayList<>(config.getVirtualItemStacks());
        }

    }

    @Getter
    private static final class SlotSpecialFluidHandler extends NotifiableRecipeHandlerTrait<SizedFluidIngredient> {

        private static final MachineTraitType<SlotSpecialFluidHandler> TYPE =
                new MachineTraitType<>(SlotSpecialFluidHandler.class, true);

        private final GTNAPatternBufferSlotConfig config;
        private final int priority;
        private final int size = 9;
        private final RecipeCapability<SizedFluidIngredient> capability = FluidRecipeCapability.CAP;
        private final IO handlerIO = IO.IN;
        private final boolean isDistinct = true;

        private SlotSpecialFluidHandler(GTNAPatternBufferSlotConfig config,
                                        int index) {
            super();
            this.config = config;
            this.priority = IFilteredHandler.HIGH + 2000 + index;
            config.setOnContentsChanged(this::notifyListeners);
        }

        @Override
        public MachineTraitType<SlotSpecialFluidHandler> getTraitType() {
            return TYPE;
        }

        @Override
        public List<SizedFluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SizedFluidIngredient> left,
                                                       boolean simulate) {
            List<FluidStack> configuredFluids = config.getVirtualFluidStacks();
            if (io != IO.IN || left == null || left.isEmpty() || configuredFluids.isEmpty()) {
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
                    it.set(new SizedFluidIngredient(ingredient.ingredient(), amountLeft));
                }
            }

            return left;
        }

        @Override
        public @NotNull List<Object> getContents() {
            return new ArrayList<>(config.getVirtualFluidStacks());
        }

        @Override
        public double getTotalContentAmount() {
            return config.getVirtualFluidStacks().stream().mapToLong(FluidStack::getAmount).sum();
        }
    }
}
