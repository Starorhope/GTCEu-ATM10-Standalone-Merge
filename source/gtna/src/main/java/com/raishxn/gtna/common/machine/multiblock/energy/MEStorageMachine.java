package com.raishxn.gtna.common.machine.multiblock.energy;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import com.raishxn.gtna.common.block.MEStorageCoreBlock;
import com.raishxn.gtna.common.data.GTNAItems;
import com.raishxn.gtna.common.machine.multiblock.part.ae.GTNAMEStorageAccessPartMachine;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import org.jetbrains.annotations.Nullable;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.api.widget.IWidget;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;

import java.util.List;

public class MEStorageMachine extends WorkableMultiblockMachine implements IMuiMachine {

    private static final long INFINITE_THRESHOLD_BYTES = 1_000_000_000_000L;

    @SaveField
    private final NotifiableItemStackHandler machineStorage;

    @SyncToClient
    private long capacityBytes;
    @SyncToClient
    private boolean infinite;
    @SyncToClient
    private long usedBytes;
    @SyncToClient
    private int storedTypes;
    @SyncToClient
    private boolean accessOnline;
    @SyncToClient
    private String accessMode = "";

    @Nullable
    private GTNAMEStorageAccessPartMachine accessPart;

    public MEStorageMachine(BlockEntityCreationInfo holder, Object... args) {
        super(holder);
        this.machineStorage = attachTrait(new NotifiableItemStackHandler(1, IO.IN, IO.IN));
        this.machineStorage.setFilter(stack -> stack.is(GTNAItems.INFINITE_CELL_COMPONENT.asItem()));
        this.machineStorage.storage.setOnContentsChanged(this::onMachineStorageChanged);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            subscribeServerTick(this::updateAccessStatus);
        }
    }

    @Override
    public void formStructure(String substructureName) {
        super.formStructure(substructureName);
        configureAccessHatch();
    }

    @Override
    public void invalidateStructure(String substructureName) {
        clearAccessHatch();
        super.invalidateStructure(substructureName);
    }

    @Override
    public void onUnload() {
        clearAccessHatch();
        super.onUnload();
    }

    @Override
    public List<IWidget> getWidgetsForDisplay(PanelSyncManager syncManager) {
        List<IWidget> widgets = super.getWidgetsForDisplay(syncManager);
        widgets.add(MUI2MachineDisplay.syncedLines(syncManager, "gtna_me_storage", this::addCustomDisplayText));
        widgets.add(Flow.row().coverChildren().childPadding(4)
                .child(new ItemSlot().slot(SyncHandlers.itemSlot(machineStorage, 0).singletonSlotGroup()))
                .child(Text.lang("gtna.machine.me_storage.infinite_cell_slot").asWidget()));
        return widgets;
    }

    private void addCustomDisplayText(List<Component> textList) {
        if (!isFormed()) {
            textList.add(Component.translatable("gtna.machine.me_storage.unformed").withStyle(ChatFormatting.GRAY));
            return;
        }

        textList.add(Component.translatable("gtna.machine.me_storage.title")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        if (accessPart == null) {
            textList.add(Component.translatable("gtna.machine.me_storage.no_access").withStyle(ChatFormatting.RED));
            return;
        }

        textList.add(Component.translatable("gtna.machine.me_storage.access",
                Component.translatable(accessMode),
                Component.translatable(accessOnline ? "gtna.machine.me_storage.online" :
                        "gtna.machine.me_storage.offline"))
                .withStyle(accessOnline ? ChatFormatting.GREEN : ChatFormatting.RED));
        textList.add(Component.translatable("gtna.machine.me_storage.capacity",
                infinite ? Component.translatable("gtna.machine.me_storage.infinite") : formatBytes(capacityBytes))
                .withStyle(ChatFormatting.GRAY));
        textList.add(Component.translatable("gtna.machine.me_storage.used",
                formatBytes(usedBytes), FormattingUtil.formatNumbers(storedTypes)).withStyle(ChatFormatting.GRAY));
        ItemStack componentStack = machineStorage.getStackInSlot(0);
        textList.add(Component.translatable("gtna.machine.me_storage.infinite_status",
                componentStack.getCount(), Component.translatable(infinite ? "gtna.machine.me_storage.enabled" :
                        "gtna.machine.me_storage.disabled")).withStyle(
                        infinite ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
    }

    private void onMachineStorageChanged() {
        configureAccessHatch();
        setChanged();
    }

    private void configureAccessHatch() {
        if (isRemote() || !isFormed()) {
            return;
        }
        GTNAMEStorageAccessPartMachine foundAccessPart = findAccessPart();
        long capacity = calculateCapacity();
        boolean hasInfiniteStack = machineStorage.getStackInSlot(0).getCount() >= 64;
        boolean infiniteMode = capacity >= INFINITE_THRESHOLD_BYTES && hasInfiniteStack;

        clearAccessHatch();
        accessPart = foundAccessPart;
        if (capacityBytes != capacity) {
            capacityBytes = capacity;
            getSyncDataHolder().markClientSyncFieldDirty("capacityBytes");
        }
        if (infinite != infiniteMode) {
            infinite = infiniteMode;
            getSyncDataHolder().markClientSyncFieldDirty("infinite");
        }
        if (accessPart != null) {
            accessPart.configureStorage(capacityBytes, infinite);
        }
        updateAccessStatus();
    }

    private void clearAccessHatch() {
        if (accessPart != null) {
            accessPart.clearStorageMount();
        }
        accessPart = null;
        if (accessOnline) {
            accessOnline = false;
            getSyncDataHolder().markClientSyncFieldDirty("accessOnline");
        }
        if (!accessMode.isEmpty()) {
            accessMode = "";
            getSyncDataHolder().markClientSyncFieldDirty("accessMode");
        }
    }

    private void updateAccessStatus() {
        if (accessPart == null && isFormed()) {
            accessPart = findAccessPart();
            if (accessPart != null) {
                accessPart.configureStorage(capacityBytes, infinite);
            }
        }
        if (accessPart == null) {
            updateSyncedAccessStatus(0L, 0, false, accessMode);
            return;
        }
        updateSyncedAccessStatus(
                accessPart.getUsedBytes(),
                accessPart.getStoredTypes(),
                accessPart.getMainNode().isOnline(),
                accessPart.getMode().translationKey());
    }

    private void updateSyncedAccessStatus(long newUsedBytes, int newStoredTypes, boolean newAccessOnline,
                                          String newAccessMode) {
        if (usedBytes != newUsedBytes) {
            usedBytes = newUsedBytes;
            getSyncDataHolder().markClientSyncFieldDirty("usedBytes");
        }
        if (storedTypes != newStoredTypes) {
            storedTypes = newStoredTypes;
            getSyncDataHolder().markClientSyncFieldDirty("storedTypes");
        }
        if (accessOnline != newAccessOnline) {
            accessOnline = newAccessOnline;
            getSyncDataHolder().markClientSyncFieldDirty("accessOnline");
        }
        if (!accessMode.equals(newAccessMode)) {
            accessMode = newAccessMode;
            getSyncDataHolder().markClientSyncFieldDirty("accessMode");
        }
    }

    @Nullable
    private GTNAMEStorageAccessPartMachine findAccessPart() {
        for (MultiblockPartMachine part : getParts()) {
            if (part instanceof GTNAMEStorageAccessPartMachine storageAccessPart) {
                return storageAccessPart;
            }
        }
        return null;
    }

    private long calculateCapacity() {
        if (getLevel() == null || getDefaultPatternState() == null) {
            return 0L;
        }
        long capacity = 0L;
        for (long packedPos : getDefaultPatternState().getCache().keySet()) {
            BlockPos pos = BlockPos.of(packedPos);
            Block block = getLevel().getBlockState(pos).getBlock();
            if (block instanceof MEStorageCoreBlock storageCore && !storageCore.isCraftingCore()) {
                capacity = saturatedAdd(capacity, storageCore.getCapacity());
            }
        }
        return capacity;
    }

    private static long saturatedAdd(long a, long b) {
        long result = a + b;
        return result < 0L || result < a ? Long.MAX_VALUE : result;
    }

    private static String formatBytes(long bytes) {
        String[] units = { "B", "KiB", "MiB", "GiB", "TiB", "PiB", "EiB" };
        double value = bytes;
        int unit = 0;
        while (value >= 1024.0D && unit < units.length - 1) {
            value /= 1024.0D;
            unit++;
        }
        return String.format(java.util.Locale.US, value == Math.rint(value) ? "%.0f %s" : "%.2f %s", value,
                units[unit]);
    }
}
