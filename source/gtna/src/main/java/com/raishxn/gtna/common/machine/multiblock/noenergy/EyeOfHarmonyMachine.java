package com.raishxn.gtna.common.machine.multiblock.noenergy;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.ExtendedUseOnContext;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.raishxn.gtna.api.capability.WirelessEnergyManager;
import com.raishxn.gtna.utils.MachineIO;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import com.raishxn.gtna.utils.datastructure.Int128;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EyeOfHarmonyMachine extends WorkableMultiblockMachine implements IMuiMachine {

    private static final long FLUID_BATCH = 100_000_000L;
    private static final long REQUIRED_GAS = 1_024_000_000L;
    private static final Int128 BASE_STARTUP = Int128.fromBigInteger(BigInteger.valueOf(5_277_655_810_867_200L));

    @SaveField
    @SyncToClient
    private int overclockLevel = 0;
    @SaveField
    @SyncToClient
    private long hydrogen = 0;
    @SaveField
    @SyncToClient
    private long helium = 0;
    @SaveField
    @SyncToClient
    private UUID networkOwner;

    public EyeOfHarmonyMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
    }

    private void setNetworkOwner(UUID owner) {
        if (!Objects.equals(networkOwner, owner)) {
            networkOwner = owner;
            getSyncDataHolder().markClientSyncFieldDirty("networkOwner");
        }
    }

    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        if (player != null) {
            setNetworkOwner(player.getUUID());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            subscribeServerTick(this::updateStartupState);
        }
    }

    private void updateStartupState() {
        if (!isFormed() || getOffsetTimer() % 20 != 0) {
            return;
        }
        if (networkOwner == null) {
            setNetworkOwner(getOwnerUUID());
        }

        int previousOverclockLevel = overclockLevel;
        long previousHydrogen = hydrogen;
        long previousHelium = helium;
        overclockLevel = 0;
        if (MachineIO.inputFluid(this, GTMaterials.Hydrogen.getFluid((int) FLUID_BATCH))) {
            hydrogen += FLUID_BATCH;
        }
        if (MachineIO.inputFluid(this, GTMaterials.Helium.getFluid((int) FLUID_BATCH))) {
            helium += FLUID_BATCH;
        }
        if (MachineIO.notConsumableCircuit(this, 4)) {
            overclockLevel = 4;
        } else if (MachineIO.notConsumableCircuit(this, 3)) {
            overclockLevel = 3;
        } else if (MachineIO.notConsumableCircuit(this, 2)) {
            overclockLevel = 2;
        } else if (MachineIO.notConsumableCircuit(this, 1)) {
            overclockLevel = 1;
        }
        if (overclockLevel != previousOverclockLevel) {
            getSyncDataHolder().markClientSyncFieldDirty("overclockLevel");
        }
        if (hydrogen != previousHydrogen) {
            getSyncDataHolder().markClientSyncFieldDirty("hydrogen");
        }
        if (helium != previousHelium) {
            getSyncDataHolder().markClientSyncFieldDirty("helium");
        }
    }

    public Int128 getStartupEnergy() {
        if (overclockLevel <= 0) {
            return Int128.ZERO();
        }
        return BASE_STARTUP.copy().multiply((long) Math.pow(8, overclockLevel - 1));
    }

    @Nullable
    public static ModifierFunction recipeModifier(MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof EyeOfHarmonyMachine harmonyMachine)) {
            return ModifierFunction.NULL;
        }
        if (!(harmonyMachine.getLevel() instanceof ServerLevel serverLevel) ||
                harmonyMachine.networkOwner == null ||
                harmonyMachine.hydrogen < REQUIRED_GAS ||
                harmonyMachine.helium < REQUIRED_GAS ||
                harmonyMachine.overclockLevel <= 0) {
            return ModifierFunction.NULL;
        }

        Int128 startupEnergy = harmonyMachine.getStartupEnergy();
        if (WirelessEnergyManager.getEnergy(serverLevel, harmonyMachine.networkOwner).compareTo(startupEnergy) < 0) {
            return ModifierFunction.NULL;
        }

        return ModifierFunction.builder()
                .durationMultiplier(4800.0 / Math.pow(2, harmonyMachine.overclockLevel) / recipe.duration)
                .build();
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!super.beforeWorking(recipe) ||
                !(getLevel() instanceof ServerLevel serverLevel) ||
                networkOwner == null ||
                hydrogen < REQUIRED_GAS ||
                helium < REQUIRED_GAS ||
                overclockLevel <= 0 ||
                !WirelessEnergyManager.consumeEnergy(serverLevel, networkOwner, getStartupEnergy())) {
            return false;
        }

        hydrogen -= REQUIRED_GAS;
        helium -= REQUIRED_GAS;
        getSyncDataHolder().markClientSyncFieldDirty("hydrogen");
        getSyncDataHolder().markClientSyncFieldDirty("helium");
        return true;
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        if (networkOwner == null) {
            setNetworkOwner(player.getUUID());
        }
        return true;
    }

    @Override
    public InteractionResult onUse(ExtendedUseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player != null && context.getItemInHand().is(GTItems.TOOL_DATA_STICK.asItem())) {
            setNetworkOwner(player.getUUID());
            if (!world.isClientSide) {
                player.sendSystemMessage(Component.translatable("gtna.machine.eye_of_harmony.rebound"));
            }
            return InteractionResult.sidedSuccess(world.isClientSide);
        }
        return super.onUse(context);
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_eye_harmony", this::addCustomDisplayText));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (isFormed()) {
            String ownerName = networkOwner == null ? "-" : resolvePlayerName(networkOwner);
            Int128 stored = getLevel() instanceof ServerLevel serverLevel && networkOwner != null ?
                    WirelessEnergyManager.getEnergy(serverLevel, networkOwner) : Int128.ZERO();
            textList.add(Component.translatable("gtna.machine.eye_of_harmony.owner", ownerName));
            textList.add(Component.translatable("gtna.machine.eye_of_harmony.network_eu",
                    FormattingUtil.formatNumbers(stored.toString())));
            textList.add(Component.translatable("gtna.machine.eye_of_harmony.startup_eu",
                    FormattingUtil.formatNumbers(getStartupEnergy().toString())));
            textList.add(Component.translatable("gtna.machine.eye_of_harmony.hydrogen",
                    FormattingUtil.formatNumbers(hydrogen)));
            textList.add(Component.translatable("gtna.machine.eye_of_harmony.helium",
                    FormattingUtil.formatNumbers(helium)));
        }
    }

    private String resolvePlayerName(UUID uuid) {
        if (getLevel() instanceof ServerLevel serverLevel) {
            Player player = serverLevel.getServer().getPlayerList().getPlayer(uuid);
            if (player != null) {
                return player.getName().getString();
            }
        }
        return uuid.toString().substring(0, 8) + "...";
    }
}
