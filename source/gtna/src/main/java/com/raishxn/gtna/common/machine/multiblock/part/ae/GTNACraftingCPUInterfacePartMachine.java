package com.raishxn.gtna.common.machine.multiblock.part.ae;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.events.GridCraftingCpuChange;
import appeng.api.networking.security.IActionHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import com.raishxn.gtna.common.machine.multiblock.energy.NexusMEHyperCoreMachine;
import com.raishxn.gtna.integration.ae2.crafting.IGTNACraftingCPUCluster;

import java.util.List;

public class GTNACraftingCPUInterfacePartMachine extends MEBusPartMachine implements IActionHost {

    private static final String CPU_TAG = "NexusCraftingCpu";
    private static final String STORAGE_TAG = "NexusCpuStorage";
    private static final String COPROCESSORS_TAG = "NexusCpuCoProcessors";

    private final MachineSource machineSource = new MachineSource(this);
    private CraftingCPUCluster cluster;
    private CompoundTag pendingClusterTag;
    private long storageBytes;
    private int coProcessors;
    @SaveField
    private final CpuState cpuState = new CpuState();
    private TickableSubscription reconnectSubscription;
    private int reconnectTicks;

    public GTNACraftingCPUInterfacePartMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder, IO.IN, new NotifiableItemStackHandler(1, IO.IN, IO.NONE));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuildCluster();
        scheduleCpuReconnect();
    }

    @Override
    public void onUnload() {
        if (reconnectSubscription != null) {
            reconnectSubscription.unsubscribe();
            reconnectSubscription = null;
        }
        super.onUnload();
    }

    @Override
    public void addedToController(MultiblockControllerMachine controller, String substructureName) {
        super.addedToController(controller, substructureName);
        configureFromController(controller);
    }

    @Override
    public void removedFromController(MultiblockControllerMachine controller) {
        super.removedFromController(controller);
        configureCpu(0L, 0);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        rebuildCluster();
        scheduleCpuReconnect();
    }

    public void configureCpu(long storageBytes, long coProcessors) {
        this.storageBytes = Math.max(0L, storageBytes);
        this.coProcessors = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, coProcessors));
        rebuildCluster();
        notifyCpuChanged();
        scheduleCpuReconnect();
    }

    public List<CraftingCPUCluster> getClusters() {
        if (!isFormed() || !getMainNode().isActive() || cluster == null || storageBytes <= 0L || coProcessors <= 0) {
            return List.of();
        }
        return List.of(cluster);
    }

    public void onChanged() {
        markAsChanged();
    }

    @Override
    public IGridNode getActionableNode() {
        return getMainNode().getNode();
    }

    private void configureFromController(MultiblockControllerMachine controller) {
        if (controller instanceof NexusMEHyperCoreMachine hyperCore) {
            configureCpu(hyperCore.getAeStorageBytes(), hyperCore.getAeCoProcessors());
        }
    }

    private void scheduleCpuReconnect() {
        if (isRemote() || reconnectSubscription != null && reconnectSubscription.isStillSubscribed()) {
            return;
        }
        reconnectTicks = 0;
        reconnectSubscription = subscribeServerTick(this::tickCpuReconnect);
    }

    private void tickCpuReconnect() {
        reconnectTicks++;
        if (isFormed()) {
            for (MultiblockControllerMachine controller : getControllers()) {
                configureFromController(controller);
                break;
            }
        }
        rebuildCluster();
        notifyCpuChanged();

        IGridNode node = getMainNode().getNode();
        if (node != null && node.getGrid() != null || reconnectTicks >= 100) {
            if (reconnectSubscription != null) {
                reconnectSubscription.unsubscribe();
                reconnectSubscription = null;
            }
        }
    }

    private void rebuildCluster() {
        if (isRemote() || storageBytes <= 0L || coProcessors <= 0) {
            cluster = null;
            return;
        }
        if (cluster == null) {
            cluster = IGTNACraftingCPUCluster.create(this, machineSource, storageBytes, coProcessors);
            if (pendingClusterTag != null) {
                cluster.readFromNBT(pendingClusterTag, getLevel().registryAccess());
                pendingClusterTag = null;
            }
        } else {
            IGTNACraftingCPUCluster bridge = IGTNACraftingCPUCluster.of(cluster);
            bridge.gtna$setMachine(this);
            bridge.gtna$setMachineSource(machineSource);
            bridge.gtna$setStorage(storageBytes);
            bridge.gtna$setAccelerator(coProcessors);
        }
    }

    private void notifyCpuChanged() {
        IGridNode node = getMainNode().getNode();
        if (node != null && node.getGrid() != null) {
            node.getGrid().postEvent(new GridCraftingCpuChange(node));
        }
    }

    private final class CpuState implements INBTSerializable<CompoundTag> {

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putLong(STORAGE_TAG, storageBytes);
            tag.putInt(COPROCESSORS_TAG, coProcessors);
            if (cluster != null) {
                CompoundTag cpuTag = new CompoundTag();
                cluster.writeToNBT(cpuTag, provider);
                tag.put(CPU_TAG, cpuTag);
            } else if (pendingClusterTag != null) {
                tag.put(CPU_TAG, pendingClusterTag.copy());
            }
            return tag;
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
            storageBytes = Math.max(0L, tag.getLong(STORAGE_TAG));
            coProcessors = Math.max(0, tag.getInt(COPROCESSORS_TAG));
            pendingClusterTag = tag.contains(CPU_TAG) ? tag.getCompound(CPU_TAG).copy() : null;
        }
    }
}
