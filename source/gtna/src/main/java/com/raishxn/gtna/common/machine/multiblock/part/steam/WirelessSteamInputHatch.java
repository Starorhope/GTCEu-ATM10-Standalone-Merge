package com.raishxn.gtna.common.machine.multiblock.part.steam;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.multiblock.part.SteamHatchPartMachine;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import com.raishxn.gtna.api.capability.SteamWirelessNetworkManager;
import com.raishxn.gtna.config.ConfigHolder;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessSteamInputHatch extends SteamHatchPartMachine {

    private final long transferRate;
    private final boolean isSteel;

    public WirelessSteamInputHatch(BlockEntityCreationInfo holder, boolean isSteel, Object... args) {
        super(holder);
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

    @Override
    public boolean isWorkingEnabled() {
        return ConfigHolder.INSTANCE.wirelessSteam.enabled && super.isWorkingEnabled();
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
            long capacity = tank.getTankCapacity(0);
            long spaceNeeded = capacity - currentSteam;

            if (spaceNeeded > 0) {
                int toPull = (int) Math.min(spaceNeeded, transferRate);

                if (SteamWirelessNetworkManager.consumeSteamFromGlobalMap(serverLevel, ownerId, toPull)) {
                    FluidStack steamStack = GTMaterials.Steam.getFluid(toPull);
                    tank.fill(steamStack, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

}
