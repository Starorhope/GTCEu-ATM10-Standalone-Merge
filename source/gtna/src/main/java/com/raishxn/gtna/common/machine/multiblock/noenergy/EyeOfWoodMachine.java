package com.raishxn.gtna.common.machine.multiblock.noenergy;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.recipe.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.machine.trait.GTNABatchRecipeLogic;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.util.ArrayList;
import java.util.List;

public class EyeOfWoodMachine extends WorkableMultiblockMachine implements IMuiMachine {

    private static final int STANDARD_WATER = 256_000;
    private static final int STANDARD_LAVA = 256_000;
    private static final int DURATION = 1200;
    private static final int SUCCESS_BASE = 7500;
    private static final double SUCCESS_SUBSTRATE = Math.pow(2_000_000_000d, 1d / 256d);
    private static final Fluid[] WATER_FLUIDS = new Fluid[] { Fluids.WATER, GTMaterials.Water.getFluid() };
    private static final Fluid[] LAVA_FLUIDS = new Fluid[] { Fluids.LAVA, GTMaterials.Lava.getFluid() };

    private static final OutputEntry[] OUTPUT_POOL = new OutputEntry[] {
            new OutputEntry(12, GTMaterials.Iron, new Material[] { GTMaterials.Nickel, GTMaterials.Tin }),
            new OutputEntry(12, GTMaterials.Copper, new Material[] { GTMaterials.Gold, GTMaterials.Nickel }),
            new OutputEntry(10, GTMaterials.Gold, new Material[] { GTMaterials.Copper, GTMaterials.Silver }),
            new OutputEntry(10, GTMaterials.Tin, new Material[] { GTMaterials.Iron, GTMaterials.Copper }),
            new OutputEntry(9, GTMaterials.Cobalt, new Material[] { GTMaterials.Iron, GTMaterials.Nickel }),
            new OutputEntry(9, GTMaterials.Redstone, new Material[] { GTMaterials.Cinnabar, GTMaterials.Ruby }),
            new OutputEntry(8, GTMaterials.Lapis, new Material[] { GTMaterials.Sodalite, GTMaterials.Lazurite }),
            new OutputEntry(8, GTMaterials.Coal, new Material[] { GTMaterials.Diamond }),
            new OutputEntry(6, GTMaterials.Diamond, new Material[] { GTMaterials.Coal, GTMaterials.Graphite }),
            new OutputEntry(6, GTMaterials.Emerald, new Material[] { GTMaterials.Beryllium, GTMaterials.Aluminium }),
            new OutputEntry(5, GTMaterials.Ruby, new Material[] { GTMaterials.Chromium, GTMaterials.Redstone }),
            new OutputEntry(5, GTMaterials.Sapphire, new Material[] { GTMaterials.Aluminium, GTMaterials.GreenSapphire }),
            new OutputEntry(4, GTMaterials.Silver, new Material[] { GTMaterials.Gold, GTMaterials.Lead }),
            new OutputEntry(4, GTMaterials.Lead, new Material[] { GTMaterials.Silver, GTMaterials.Sulfur })
    };

    @SaveField
    @SyncToClient
    private int storedWater;
    @SaveField
    @SyncToClient
    private int storedLava;
    @SaveField
    @SyncToClient
    private int successChance;
    @SaveField
    @SyncToClient
    private boolean lastRollSucceeded;

    public EyeOfWoodMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, new GTNABatchRecipeLogic());
        getRecipeLogic().setRecipeSupplier(this::buildRecipe);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            subscribeServerTick(this::updateInternalFluidStorage);
        }
    }

    @Override
    public @NotNull GTNABatchRecipeLogic getRecipeLogic() {
        return (GTNABatchRecipeLogic) super.getRecipeLogic();
    }

    private void setSuccessChance(int value) {
        if (successChance != value) {
            successChance = value;
            getSyncDataHolder().markClientSyncFieldDirty("successChance");
        }
    }

    private @Nullable GTRecipe buildRecipe() {
        if (!isFormed() || !isWorkingEnabled()) {
            return null;
        }
        if (getLevel() == null || !getLevel().dimension().equals(Level.OVERWORLD)) {
            setSuccessChance(0);
            return null;
        }

        if (storedWater < STANDARD_WATER || storedLava < STANDARD_LAVA) {
            setSuccessChance(0);
            return null;
        }

        setSuccessChance(calculateSuccessChance());
        boolean rollSucceeded = GTValues.RNG.nextInt(10_000) < successChance;
        if (lastRollSucceeded != rollSucceeded) {
            lastRollSucceeded = rollSucceeded;
            getSyncDataHolder().markClientSyncFieldDirty("lastRollSucceeded");
        }
        if (storedWater != 0) {
            storedWater = 0;
            getSyncDataHolder().markClientSyncFieldDirty("storedWater");
        }
        if (storedLava != 0) {
            storedLava = 0;
            getSyncDataHolder().markClientSyncFieldDirty("storedLava");
        }

        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw().recipeType(GTRecipeTypes.DUMMY_RECIPES).duration(DURATION);
        if (lastRollSucceeded) {
            for (ItemStack output : generateOutputs()) {
                builder.outputItems(output);
            }
        } else {
            builder.outputFluids(GTMaterials.Steam.getFluid(getFailSteamOutput()));
        }
        return builder.build();
    }

    private void updateInternalFluidStorage() {
        if (!isFormed() || getOffsetTimer() % 20 != 0) {
            return;
        }
        int drainedWater = drainFluidFromInputs(STANDARD_WATER - storedWater, WATER_FLUIDS);
        int drainedLava = drainFluidFromInputs(STANDARD_LAVA - storedLava, LAVA_FLUIDS);
        if (drainedWater != 0) {
            storedWater += drainedWater;
            getSyncDataHolder().markClientSyncFieldDirty("storedWater");
        }
        if (drainedLava != 0) {
            storedLava += drainedLava;
            getSyncDataHolder().markClientSyncFieldDirty("storedLava");
        }
        setSuccessChance(storedWater >= STANDARD_WATER && storedLava >= STANDARD_LAVA ? calculateSuccessChance() : 0);
        if (drainedWater > 0 || drainedLava > 0) {
            GTNACORE.LOGGER.debug("Eye of Wood at {} drained water={}, lava={}, storedWater={}, storedLava={}, chance={}",
                    getBlockPos(), drainedWater, drainedLava, storedWater, storedLava, successChance);
        }
    }

    private int drainFluidFromInputs(int maxAmount, Fluid... fluids) {
        if (maxAmount <= 0) {
            return 0;
        }
        int collected = 0;
        boolean foundFluidHandler = false;

        for (var part : getParts()) {
            for (var handlerList : part.getRecipeHandlers()) {
                if (!handlerList.isValid(IO.IN)) {
                    continue;
                }
                for (Object handlerObj : handlerList.getCapability(FluidRecipeCapability.CAP)) {
                    if (handlerObj instanceof NotifiableFluidTank tank) {
                        foundFluidHandler = true;
                        collected += drainFromTank(tank, maxAmount - collected, fluids);
                    } else if (handlerObj instanceof IFluidHandler handler) {
                        foundFluidHandler = true;
                        collected += drainFromHandler(handler, maxAmount - collected, fluids);
                    } else {
                        GTNACORE.LOGGER.debug("Eye of Wood at {} found non-fluid input handler {}",
                                getBlockPos(), handlerObj.getClass().getName());
                        continue;
                    }
                    if (collected >= maxAmount) {
                        break;
                    }
                }
                if (collected >= maxAmount) {
                    break;
                }
            }
            if (collected >= maxAmount) {
                break;
            }
        }
        if (!foundFluidHandler) {
            GTNACORE.LOGGER.debug("Eye of Wood at {} found no IFluidHandler input hatches in formed parts", getBlockPos());
        } else if (collected == 0) {
            GTNACORE.LOGGER.debug("Eye of Wood at {} found input fluid handlers, but drained no matching fluid",
                    getBlockPos());
        }
        return collected;
    }

    private int drainFromTank(NotifiableFluidTank tank, int maxAmount, Fluid... fluids) {
        int collected = 0;
        for (int index = 0; index < tank.getTanks() && collected < maxAmount; index++) {
            FluidStack stored = tank.getFluidInTank(index);
            if (stored.isEmpty() || !matchesAny(stored, fluids)) {
                continue;
            }
            FluidStack request = stored.copy();
            request.setAmount(Math.min(stored.getAmount(), maxAmount - collected));
            FluidStack drained = tank.drainInternal(request, IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                collected += drained.getAmount();
            }
        }
        return collected;
    }

    private int drainFromHandler(IFluidHandler handler, int maxAmount, Fluid... fluids) {
        int collected = 0;
        for (int index = 0; index < handler.getTanks() && collected < maxAmount; index++) {
            FluidStack stored = handler.getFluidInTank(index);
            if (stored.isEmpty() || !matchesAny(stored, fluids)) {
                continue;
            }
            FluidStack request = stored.copy();
            request.setAmount(Math.min(stored.getAmount(), maxAmount - collected));
            FluidStack drained = handler.drain(request,
                    IFluidHandler.FluidAction.EXECUTE);
            if (!drained.isEmpty()) {
                collected += drained.getAmount();
            }
        }
        return collected;
    }

    private boolean matchesAny(FluidStack stack, Fluid... fluids) {
        for (Fluid fluid : fluids) {
            if (stack.getFluid().isSame(fluid)) {
                return true;
            }
        }
        return false;
    }

    private int calculateSuccessChance() {
        if (storedWater == STANDARD_WATER && storedLava == STANDARD_LAVA) {
            return SUCCESS_BASE;
        }

        int waterDifference = Math.abs(storedWater - STANDARD_WATER) / 1000;
        int lavaDifference = Math.abs(storedLava - STANDARD_LAVA) / 1000;
        if (waterDifference >= STANDARD_WATER / 1000 || lavaDifference >= STANDARD_LAVA / 1000) {
            return 1;
        }

        double waterMultiplier = 1d / Math.pow(SUCCESS_SUBSTRATE, waterDifference);
        double lavaMultiplier = 1d / Math.pow(SUCCESS_SUBSTRATE, lavaDifference);
        return Math.max(1, (int) (SUCCESS_BASE - 7499 * (1d - waterMultiplier * lavaMultiplier)));
    }

    private int getFailSteamOutput() {
        return 36_000 * Math.max(1, successChance);
    }

    private List<ItemStack> generateOutputs() {
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            OutputEntry entry = rollEntry();
            addOreStyleOutputs(outputs, entry);
        }
        return outputs;
    }

    private OutputEntry rollEntry() {
        int totalWeight = 0;
        for (OutputEntry entry : OUTPUT_POOL) {
            totalWeight += entry.weight();
        }
        int roll = GTValues.RNG.nextInt(totalWeight);
        int cursor = 0;
        for (OutputEntry entry : OUTPUT_POOL) {
            cursor += entry.weight();
            if (roll < cursor) {
                return entry;
            }
        }
        return OUTPUT_POOL[0];
    }

    private void addOreStyleOutputs(List<ItemStack> outputs, OutputEntry entry) {
        addMaterialStack(outputs, TagPrefix.dust, entry.primary(), 64);
        for (Material byproduct : entry.byproducts()) {
            addMaterialStack(outputs, TagPrefix.dust, byproduct, entry.byproducts().length == 1 ? 48 : 32);
        }
        if (hasMaterialItem(TagPrefix.gem, entry.primary())) {
            if (hasMaterialItem(TagPrefix.gemExquisite, entry.primary())) {
                addMaterialStack(outputs, TagPrefix.gemExquisite, entry.primary(), 16);
                addMaterialStack(outputs, TagPrefix.gemFlawless, entry.primary(), 32);
                addMaterialStack(outputs, TagPrefix.gem, entry.primary(), 32);
            } else {
                addMaterialStack(outputs, TagPrefix.gem, entry.primary(), 64);
            }
        }
    }

    private boolean hasMaterialItem(TagPrefix prefix, Material material) {
        ItemStack stack = ChemicalHelper.get(prefix, material);
        return stack != null && !stack.isEmpty();
    }

    private void addMaterialStack(List<ItemStack> outputs, TagPrefix prefix, Material material, int amount) {
        ItemStack stack = ChemicalHelper.get(prefix, material);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int remaining = amount;
        while (remaining > 0) {
            int split = Math.min(64, remaining);
            outputs.add(copyWithCount(stack, split));
            remaining -= split;
        }
    }

    private ItemStack copyWithCount(ItemStack stack, int count) {
        ItemStack copy = stack.copy();
        copy.setCount(count);
        return copy;
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_eye_wood", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (isFormed()) {
                    textList.add(Component.translatable("gtna.machine.eye_of_wood.water", storedWater, STANDARD_WATER)
                            .withStyle(ChatFormatting.BLUE));
                    textList.add(Component.translatable("gtna.machine.eye_of_wood.lava", storedLava, STANDARD_LAVA)
                            .withStyle(ChatFormatting.RED));
                    textList.add(Component.translatable("gtna.machine.eye_of_wood.chance", successChance)
                            .withStyle(ChatFormatting.GOLD));
                    textList.add(Component.translatable("gtna.machine.eye_of_wood.last_result",
                                    Component.translatable(lastRollSucceeded ?
                                            "gtna.machine.eye_of_wood.result.success" :
                                            "gtna.machine.eye_of_wood.result.fail"))
                            .withStyle(lastRollSucceeded ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
    }

    private record OutputEntry(int weight, Material primary, Material[] byproducts) {}

    public boolean didLastRollSucceed() {
        return lastRollSucceeded;
    }

    public boolean shouldRenderEyeModel() {
        return isFormed() && getRecipeLogic().isActive();
    }
}
