package com.raishxn.gtna.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;

import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GTNAPatternBufferSlotConfig implements INBTSerializable<CompoundTag>, IContentChangeAware {

    private static final int GHOST_GRID_SIZE = 9;
    private static final String SPECIAL_ITEMS_TAG = "specialItems";
    private static final String SPECIAL_FLUIDS_TAG = "specialFluids";

    private Runnable onContentsChanged = () -> {};

    private final ItemStackTransfer specialItems = new ItemStackTransfer(GHOST_GRID_SIZE);
    private final FluidTank[] specialFluids = new FluidTank[GHOST_GRID_SIZE];

    private int circuitConfig = -1;
    private String preferredModeId = "";
    private String derivedModeId = "";
    private String cachedRecipeId = "";

    public GTNAPatternBufferSlotConfig() {
        specialItems.setOnContentsChanged(this::onContentsChanged);
        for (int i = 0; i < GHOST_GRID_SIZE; i++) {
            specialFluids[i] = new FluidTank(Integer.MAX_VALUE) {

                @Override
                protected void onContentsChanged() {
                    GTNAPatternBufferSlotConfig.this.onContentsChanged();
                }
            };
        }
    }

    public Runnable getOnContentsChanged() {
        return onContentsChanged;
    }

    public void setOnContentsChanged(Runnable onContentsChanged) {
        if (onContentsChanged == null) {
            return;
        }
        Runnable previous = this.onContentsChanged;
        this.onContentsChanged = () -> {
            previous.run();
            onContentsChanged.run();
        };
    }

    public void onContentsChanged() {
        onContentsChanged.run();
    }

    public ItemStackTransfer getSpecialItems() {
        return specialItems;
    }

    public FluidTank[] getSpecialFluids() {
        return specialFluids;
    }

    public int getCircuitConfig() {
        return circuitConfig;
    }

    public void setCircuitConfig(int circuitConfig) {
        this.circuitConfig = circuitConfig < 0 ? -1 : circuitConfig;
        onContentsChanged();
    }

    public String getPreferredModeId() {
        return preferredModeId;
    }

    public void setPreferredModeId(String preferredModeId) {
        this.preferredModeId = preferredModeId == null ? "" : preferredModeId.trim();
        onContentsChanged();
    }

    public String getCachedRecipeId() {
        return cachedRecipeId;
    }

    public void setCachedRecipeId(String cachedRecipeId) {
        this.cachedRecipeId = cachedRecipeId == null ? "" : cachedRecipeId.trim();
    }

    public String getDerivedModeId() {
        return derivedModeId;
    }

    public void setDerivedModeId(String derivedModeId) {
        this.derivedModeId = derivedModeId == null ? "" : derivedModeId.trim();
    }

    public void clearRecipeCache() {
        clearRecipeCacheSilently();
    }

    public void clearRecipeCacheSilently() {
        this.cachedRecipeId = "";
        this.derivedModeId = "";
    }

    public void clearSpecialization() {
        for (int i = 0; i < specialItems.getSlots(); i++) {
            specialItems.setStackInSlot(i, ItemStack.EMPTY);
        }
        for (FluidTank specialFluid : specialFluids) {
            specialFluid.setFluid(FluidStack.EMPTY);
        }
        this.circuitConfig = -1;
        this.preferredModeId = "";
        this.derivedModeId = "";
        this.cachedRecipeId = "";
        onContentsChanged();
    }

    public boolean hasSpecialItem() {
        for (int i = 0; i < specialItems.getSlots(); i++) {
            if (!specialItems.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSpecialFluid() {
        for (FluidTank specialFluid : specialFluids) {
            if (!specialFluid.getFluid().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCircuit() {
        return circuitConfig >= 0;
    }

    public @Nullable ItemStack getCircuitStack() {
        return hasCircuit() ? IntCircuitBehaviour.stack(circuitConfig) : null;
    }

    public List<ItemStack> getVirtualItemStacks() {
        List<ItemStack> stacks = new ArrayList<>(specialItems.getSlots() + 1);
        for (int i = 0; i < specialItems.getSlots(); i++) {
            ItemStack stack = specialItems.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        ItemStack circuitStack = getCircuitStack();
        if (circuitStack != null && !circuitStack.isEmpty()) {
            stacks.add(circuitStack);
        }
        return stacks;
    }

    public List<net.neoforged.neoforge.fluids.FluidStack> getVirtualFluidStacks() {
        List<net.neoforged.neoforge.fluids.FluidStack> stacks = new ArrayList<>(specialFluids.length);
        for (FluidTank specialFluid : specialFluids) {
            FluidStack fluidStack = specialFluid.getFluid();
            if (!fluidStack.isEmpty()) {
                stacks.add(fluidStack.copy());
            }
        }
        return stacks;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put(SPECIAL_ITEMS_TAG, specialItems.serializeNBT(provider));
        ListTag fluidTag = new ListTag();
        for (int i = 0; i < specialFluids.length; i++) {
            CompoundTag entry = specialFluids[i].writeToNBT(provider, new CompoundTag());
            entry.putInt("slot", i);
            fluidTag.add(entry);
        }
        tag.put(SPECIAL_FLUIDS_TAG, fluidTag);
        tag.putInt("circuitConfig", circuitConfig);
        if (!preferredModeId.isBlank()) {
            tag.putString("preferredModeId", preferredModeId);
        }
        if (!derivedModeId.isBlank()) {
            tag.putString("derivedModeId", derivedModeId);
        }
        if (!cachedRecipeId.isBlank()) {
            tag.putString("cachedRecipeId", cachedRecipeId);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        if (tag.contains(SPECIAL_ITEMS_TAG, Tag.TAG_COMPOUND)) {
            specialItems.deserializeNBT(provider, tag.getCompound(SPECIAL_ITEMS_TAG));
        } else {
            specialItems.deserializeNBT(provider, new CompoundTag());
            migrateLegacySingleItem(tag);
        }

        for (FluidTank specialFluid : specialFluids) {
            specialFluid.readFromNBT(provider, new CompoundTag());
        }
        if (tag.contains(SPECIAL_FLUIDS_TAG, Tag.TAG_LIST)) {
            ListTag fluidTag = tag.getList(SPECIAL_FLUIDS_TAG, Tag.TAG_COMPOUND);
            for (Tag entry : fluidTag) {
                if (entry instanceof CompoundTag compoundTag) {
                    int slot = compoundTag.getInt("slot");
                    if (slot >= 0 && slot < specialFluids.length) {
                        specialFluids[slot].readFromNBT(provider, compoundTag);
                    }
                }
            }
        } else {
            migrateLegacySingleFluid(tag);
        }

        this.circuitConfig = tag.contains("circuitConfig") ? tag.getInt("circuitConfig") : -1;
        this.preferredModeId = tag.getString("preferredModeId");
        this.derivedModeId = tag.getString("derivedModeId");
        this.cachedRecipeId = tag.getString("cachedRecipeId");
    }

    private void migrateLegacySingleItem(CompoundTag tag) {
        String specialItemId = tag.getString("specialItemId");
        if (specialItemId.isBlank()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(specialItemId);
        if (id == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            return;
        }
        int count = Math.max(1, tag.getInt("specialItemCount"));
        specialItems.setStackInSlot(0, new ItemStack(item, count));
    }

    private void migrateLegacySingleFluid(CompoundTag tag) {
        String specialFluidId = tag.getString("specialFluidId");
        if (specialFluidId.isBlank()) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(specialFluidId);
        if (id == null) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(id).orElse(null);
        if (fluid == null) {
            return;
        }
        int amount = Math.max(1, tag.getInt("specialFluidAmount"));
        specialFluids[0].setFluid(new FluidStack(fluid, amount));
    }
}
