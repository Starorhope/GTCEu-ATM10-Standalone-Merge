package com.raishxn.gtna.common.machine.multiblock.energy;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.items.IItemHandler;

import com.raishxn.gtna.common.data.GTNARecipeType;
import com.raishxn.gtna.common.machine.multiblock.part.OverclockHatchPartMachine;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class IndustrialSlaughterhouse extends WorkableElectricMultiblockMachine
                                      implements IMuiMachine {

    @SaveField
    private final List<ItemStack> lastDrops = new ArrayList<>();

    private static final String[] MOB_LIST_PASSIVE = new String[] { "chicken", "cow", "pig", "sheep", "rabbit", "horse",
            "goat" };
    private static final String[] MOB_LIST_HOSTILE = new String[] { "zombie", "skeleton", "creeper", "spider",
            "enderman", "witch", "blaze" };
    private static final String[] BOSS_MOBS = new String[] { "warden", "wither", "elder_guardian" };

    public IndustrialSlaughterhouse(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    @Override
    public @NotNull GTRecipeType getRecipeType() {
        return GTNARecipeType.SLAUGHTERHOUSE_RECIPES;
    }

    @Override
    public void afterWorking() {
        super.afterWorking();
        if (getLevel() == null || getLevel().isClientSide) return;

        ServerLevel serverLevel = (ServerLevel) getLevel();
        int circuit = getCircuitFromInputBus();
        long voltage = getEnergyContainer().getInputVoltage();
        int tier = GTUtil.getTierByVoltage(voltage);

        String[] currentMobList = null;
        int baseTier;
        int multiplierBase;

        switch (circuit) {
            case 2 -> {
                currentMobList = MOB_LIST_HOSTILE;
                baseTier = GTValues.MV;
                multiplierBase = 2;
            }
            case 3 -> {
                currentMobList = BOSS_MOBS;
                baseTier = GTValues.ZPM;
                multiplierBase = 3;
            }
            case 4 -> {
                baseTier = GTValues.UHV;
                multiplierBase = 5;
            }
            default -> {
                currentMobList = MOB_LIST_PASSIVE;
                baseTier = GTValues.LV;
                multiplierBase = 2;
            }
        }

        if (tier < baseTier) return;

        int multiplier = (int) Math.pow(multiplierBase, (tier - baseTier));
        int loops = Math.max(1, (tier - 2) * 8);

        List<ItemStack> allGeneratedDrops = new ArrayList<>();

        for (int i = 0; i < loops; i++) {
            List<ItemStack> drops = new ArrayList<>();
            if (circuit == 4) {
                drops.add(new ItemStack(Items.DRAGON_EGG));
                drops.add(new ItemStack(Items.DRAGON_BREATH, 4));
                drops.add(new ItemStack(Items.DRAGON_HEAD));
            } else if (currentMobList != null) {
                int index = GTValues.RNG.nextInt(currentMobList.length);
                ResourceKey<LootTable> lootTableKey = ResourceKey.create(Registries.LOOT_TABLE,
                        ResourceLocation.fromNamespaceAndPath("minecraft", "entities/" + currentMobList[index]));
                LootTable table = serverLevel.getServer().reloadableRegistries().getLootTable(lootTableKey);
                drops.addAll(
                        table.getRandomItems(new LootParams.Builder(serverLevel).create(LootContextParamSets.EMPTY)));
            }

            for (ItemStack stack : drops) {
                if (!stack.isEmpty()) {
                    ItemStack outputStack = stack.copy();
                    outputStack.setCount(
                            (int) Math.min(outputStack.getMaxStackSize(), (long) stack.getCount() * multiplier));
                    allGeneratedDrops.add(outputStack);
                }
            }
        }

        if (!allGeneratedDrops.isEmpty()) {
            for (ItemStack stack : allGeneratedDrops) {
                if (lastDrops.size() >= 5) lastDrops.remove(0);
                lastDrops.add(stack.copy());
            }
            var builder = GTRecipeBuilder.ofRaw();
            builder.recipeType(GTNARecipeType.SLAUGHTERHOUSE_RECIPES);
            for (ItemStack stack : allGeneratedDrops) {
                builder.outputItems(stack);
            }
            GTRecipe tempRecipe = builder.build();
            RecipeHelper.handleRecipeIO(this, tempRecipe, IO.OUT, this.recipeLogic.getChanceCaches());
        }
    }

    private int getOutputBusCount() {
        var ioMap = getCapabilitiesFlat().get(IO.OUT);
        if (ioMap == null) return 0;
        var handlers = ioMap.get(ItemRecipeCapability.CAP);
        if (handlers == null) return 0;

        Set<IItemHandler> uniqueHandlers = new HashSet<>();
        for (Object h : handlers) {
            if (h instanceof IItemHandler handler) uniqueHandlers.add(handler);
        }
        return uniqueHandlers.size();
    }

    private int getCircuitFromInputBus() {
        var capabilities = getCapabilitiesFlat().get(IO.IN);
        if (capabilities != null) {
            var handlers = capabilities.get(ItemRecipeCapability.CAP);
            if (handlers != null) {
                for (var handler : handlers) {
                    if (handler instanceof IItemHandler itemHandler) {
                        for (int i = 0; i < itemHandler.getSlots(); i++) {
                            ItemStack stack = itemHandler.getStackInSlot(i);
                            if (IntCircuitBehaviour.isIntegratedCircuit(stack))
                                return IntCircuitBehaviour.getCircuitConfiguration(stack);
                        }
                    }
                }
            }
        }
        return 0;
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof IndustrialSlaughterhouse slaughterhouse)) return ModifierFunction.NULL;
        OverclockingLogic overclockingLogic = slaughterhouse.getOverclockingLogic();
        return overclockingLogic.getModifier(machine, recipe,
                slaughterhouse.getEnergyContainer().getInputVoltage());
    }

    private OverclockingLogic getOverclockingLogic() {
        double durationFactor = OverclockingLogic.STD_DURATION_FACTOR;
        boolean hasOverclockHatch = false;
        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof OverclockHatchPartMachine hatch) {
                durationFactor = Math.min(durationFactor, hatch.getOverclockMultiplier());
                hasOverclockHatch = true;
            }
        }
        if (!hasOverclockHatch) {
            return OverclockingLogic.NON_PERFECT_OVERCLOCK;
        }
        return OverclockingLogic.create(durationFactor, OverclockingLogic.STD_VOLTAGE_FACTOR, false);
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_slaughterhouse", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        int circuit = getCircuitFromInputBus();
        int tier = GTUtil.getTierByVoltage(getEnergyContainer().getInputVoltage());

        String modeKey;
        int base;
        int mBase;
        switch (circuit) {
            case 2 -> {
                modeKey = "gtna.machine.slaughterhouse.mode.hostile";
                base = GTValues.MV;
                mBase = 2;
            }
            case 3 -> {
                modeKey = "gtna.machine.slaughterhouse.mode.boss";
                base = GTValues.ZPM;
                mBase = 3;
            }
            case 4 -> {
                modeKey = "gtna.machine.slaughterhouse.mode.dragon";
                base = GTValues.UHV;
                mBase = 5;
            }
            default -> {
                modeKey = "gtna.machine.slaughterhouse.mode.passive";
                base = GTValues.LV;
                mBase = 2;
            }
        }

        if (isFormed()) {
                    textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.mode",
                            Component.translatable(modeKey).withStyle(ChatFormatting.AQUA)));

                    if (tier >= base) {
                        int currentMult = (int) Math.pow(mBase, (tier - base));
                        textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.drop_multiplier",
                                Component.literal(currentMult + "x").withStyle(ChatFormatting.GOLD)));
                    } else {
                        textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.required_tier",
                                GTValues.VNF[base]).withStyle(ChatFormatting.RED));
                    }

                    textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.output_buses", getOutputBusCount())
                            .withStyle(getOutputBusCount() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));

                    if (!lastDrops.isEmpty()) {
                        textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.latest_drops")
                                .withStyle(ChatFormatting.YELLOW));
                        for (ItemStack s : lastDrops) {
                            textList.add(Component.translatable("gtna.machine.slaughterhouse.ui.drop_entry",
                                    s.getCount(), s.getHoverName())
                                    .withStyle(ChatFormatting.GRAY));
                        }
                    }
        }
    }
}
