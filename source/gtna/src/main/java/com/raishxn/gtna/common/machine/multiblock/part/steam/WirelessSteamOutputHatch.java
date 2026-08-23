package com.raishxn.gtna.common.machine.multiblock.part.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.raishxn.gtna.api.capability.SteamWirelessNetworkManager;
import com.raishxn.gtna.config.ConfigHolder;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessSteamOutputHatch extends FluidHatchPartMachine {

    private final long transferRate;
    private final boolean isSteel;

    public WirelessSteamOutputHatch(BlockEntityCreationInfo holder, boolean isSteel, Object... args) {
        super(holder, 0, IO.OUT, createOutputTank(isSteel));
        this.isSteel = isSteel;
        this.transferRate = isSteel ? ConfigHolder.INSTANCE.wirelessSteam.steelTransferRate :
                ConfigHolder.INSTANCE.wirelessSteam.bronzeTransferRate;
        this.setWorkingEnabled(false);
        if (this.isSteel) {
            if (this.tank.getStorages().length > 0) {
                this.tank.getStorages()[0].setCapacity(ConfigHolder.INSTANCE.wirelessSteam.steelBuffer);
            }
        } else if (this.tank.getStorages().length > 0) {
            this.tank.getStorages()[0].setCapacity(ConfigHolder.INSTANCE.wirelessSteam.bronzeBuffer);
        }
    }

    private static NotifiableFluidTank createOutputTank(boolean isSteel) {
        int configuredCapacity = isSteel ? ConfigHolder.INSTANCE.wirelessSteam.steelBuffer :
                ConfigHolder.INSTANCE.wirelessSteam.bronzeBuffer;
        return new NotifiableFluidTank(1, configuredCapacity, IO.OUT)
                .setFilter(fluidStack -> fluidStack.getFluid().is(GTMaterials.Steam.getFluidTag()));
    }

    @Override
    public boolean swapIO() {
        return false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() != null && !getLevel().isClientSide) {
            this.subscribeServerTick(this::updateWireless);
        }
    }

    private void updateWireless() {
        if (!ConfigHolder.INSTANCE.wirelessSteam.enabled) {
            return;
        }
        if (getLevel() instanceof ServerLevel serverLevel) {
            UUID ownerId = getOwnerUUID();
            if (ownerId == null) return;

            long currentSteam = tank.getFluidInTank(0).getAmount();

            if (currentSteam > 0) {
                int toPush = (int) Math.min(currentSteam, transferRate);

                boolean success = SteamWirelessNetworkManager.addSteamToGlobalSteamMap(serverLevel, ownerId, toPush);

                if (success) {
                    tank.drain(toPush, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

}
