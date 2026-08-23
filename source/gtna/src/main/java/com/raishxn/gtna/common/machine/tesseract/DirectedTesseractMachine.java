package com.raishxn.gtna.common.machine.tesseract;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait;

import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.me.storage.CompositeStorage;
import appeng.me.storage.ExternalStorageFacade;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multiset;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.integration.ae2.pattern.IParallelPatternDetails;
import com.raishxn.gtna.utils.MUI2MachineDisplay;
import com.mojang.blaze3d.platform.InputConstants;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.StringSyncValue;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DirectedTesseractMachine extends MetaMachine
                                       implements IMuiMachine, ITesseractMarkerInteractable {

    public static final Multiset<ImmutableList<TesseractDirectedTarget>> HIGHLIGHTS = HashMultiset.create();
    private static final String ROUTE_ISSUE_SEPARATOR = "\u001f";
    private static final ResourceLocation PCC_RECIPE_CIRCUIT =
            ResourceLocation.fromNamespaceAndPath("pccard", "recipe_circuit");

    @SaveField
    private final List<String> serializedTargets = new ArrayList<>();

    @SyncToClient
    private int targetCount;

    @SyncToClient
    private String lastRouteIssue = "";

    private final List<PendingInsert> pendingInserts = new ArrayList<>();
    @Nullable
    private TickableSubscription pendingInsertSubscription;

    public DirectedTesseractMachine(BlockEntityCreationInfo holder) {
        super(holder);
    }

    private void setTargetCount(int targetCount) {
        if (this.targetCount != targetCount) {
            this.targetCount = targetCount;
            getSyncDataHolder().markClientSyncFieldDirty("targetCount");
        }
    }

    private void setLastRouteIssue(String lastRouteIssue) {
        if (!this.lastRouteIssue.equals(lastRouteIssue)) {
            this.lastRouteIssue = lastRouteIssue;
            getSyncDataHolder().markClientSyncFieldDirty("lastRouteIssue");
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        setTargetCount(serializedTargets.size());
        updatePendingSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (pendingInsertSubscription != null) {
            pendingInsertSubscription.unsubscribe();
            pendingInsertSubscription = null;
        }
    }

    public boolean pushPatternFromProvider(IPatternDetails patternDetails, KeyCounter[] inputHolder,
                                           Set<AEKey> patternInputs, IActionSource actionSource) {
        setLastRouteIssue("");
        List<TesseractDirectedTarget> targets = getTargets();
        if (targets.isEmpty() || inputHolder.length == 0 || !checkInput(inputHolder)) {
            return false;
        }

        List<OrderedPush> orderedInputs = getOrderedInputs(patternDetails, inputHolder);
        if (orderedInputs.isEmpty() || orderedInputs.size() > targets.size()) {
            GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} rejected pattern {} because {} ordered inputs do not fit {} targets",
                    getBlockPos(), patternDetails.getDefinition(), orderedInputs.size(), targets.size());
            return false;
        }
        long fluidInputs = orderedInputs.stream().filter(push -> push.key() instanceof AEFluidKey).count();
        GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} received {} ordered inputs ({} fluid) for pattern {}",
                getBlockPos(), orderedInputs.size(), fluidInputs, patternDetails.getDefinition());
        for (int i = 0; i < orderedInputs.size(); i++) {
            OrderedPush push = orderedInputs.get(i);
            GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} input[{}] = {} x{}",
                    getBlockPos(), i, push.key(), push.amount());
        }

        List<PendingInsert> readyNow = new ArrayList<>();
        List<PendingInsert> pendingLater = new ArrayList<>();
        Set<TesseractDirectedTarget> routedTargets = new LinkedHashSet<>();
        for (int i = 0; i < orderedInputs.size(); i++) {
            OrderedPush orderedPush = orderedInputs.get(i);
            TesseractDirectedTarget directedTarget = targets.get(i);
            PatternProviderTarget target = resolveTarget(directedTarget, actionSource);
            MEStorage storage = resolveStorage(directedTarget);
            TargetCapabilitySummary capabilitySummary = inspectTargetCapabilities(directedTarget);
            if (target == null) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} failed to resolve target {}",
                        getBlockPos(), describeTarget(directedTarget));
                return false;
            }
            if (storage == null) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} failed to resolve storage {}",
                        getBlockPos(), describeTarget(directedTarget));
                return false;
            }
            if (target.containsPatternInput(patternInputs)) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} ignoring blocked-target check for explicit route {}",
                        getBlockPos(), describeTarget(directedTarget));
            }
            if (!isTargetCompatible(orderedPush.key(), capabilitySummary)) {
                setLastRouteIssue((i + 1) + ROUTE_ISSUE_SEPARATOR + inputKindKey(orderedPush.key()) +
                        ROUTE_ISSUE_SEPARATOR + capabilitySummary.translationKey());
                GTNACORE.LOGGER.warn(
                        "[GTNA] Directed Tesseract {} incompatible route: {} ({} x{} -> {})",
                        getBlockPos(),
                        lastRouteIssue,
                        orderedPush.key(),
                        orderedPush.amount(),
                        describeTarget(directedTarget));
                setChanged();
                return false;
            }

            long amount = orderedPush.amount();
            if (amount <= 0L) {
                continue;
            }

            long inserted = insertIntoTarget(directedTarget, storage, orderedPush.key(), amount, Actionable.SIMULATE,
                    actionSource);
            if (orderedPush.key() instanceof AEFluidKey) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} simulated fluid insert {} / {} into {}",
                        getBlockPos(), inserted, amount, describeTarget(directedTarget));
            }
            if (inserted == amount) {
                readyNow.add(new PendingInsert(directedTarget, orderedPush.key(), amount));
                routedTargets.add(directedTarget);
                continue;
            }

            if (inserted > 0L) {
                readyNow.add(new PendingInsert(directedTarget, orderedPush.key(), inserted));
            }
            pendingLater.add(new PendingInsert(directedTarget, orderedPush.key(), amount - inserted));
            routedTargets.add(directedTarget);
        }

        for (PendingInsert pendingInsert : readyNow) {
            if (!insertNow(pendingInsert, actionSource)) {
                return false;
            }
        }

        if (!pendingLater.isEmpty()) {
            long fluidPendings = pendingLater.stream().filter(insert -> insert.key() instanceof AEFluidKey).count();
            if (fluidPendings > 0) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} queued {} fluid pending insert(s)",
                        getBlockPos(), fluidPendings);
            }
            pendingInserts.addAll(pendingLater);
            updatePendingSubscription();
            setChanged();
        }
        applyProgrammedCircuit(patternDetails, routedTargets);
        return true;
    }

    private void applyProgrammedCircuit(IPatternDetails patternDetails,
                                        Set<TesseractDirectedTarget> routedTargets) {
        if (routedTargets.isEmpty() || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(PCC_RECIPE_CIRCUIT)) {
            return;
        }
        var circuitComponent = BuiltInRegistries.DATA_COMPONENT_TYPE.get(PCC_RECIPE_CIRCUIT);
        Object componentValue = patternDetails.getDefinition().get(circuitComponent);
        if (!(componentValue instanceof Integer circuitNumber) || !(getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (TesseractDirectedTarget target : routedTargets) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(target.pos().dimension());
            if (targetLevel == null) {
                continue;
            }
            MetaMachine machine = MetaMachine.getMachine(targetLevel, target.pos().pos());
            if (machine != null) {
                machine.getTraitOptional(ProgrammableCircuitSlotTrait.TYPE)
                        .ifPresent(trait -> trait.setCurrentCircuit(circuitNumber));
            }
        }
    }

    @Override
    public boolean onMarkerInteract(Player player, List<TesseractDirectedTarget> targets) {
        if (targets.isEmpty()) {
            return false;
        }
        setTargets(targets);
        if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("gtna.machine.directed_tesseract.bind_success"), true);
        }
        return true;
    }

    @Override
    public void buildMainUI(ParentWidget<?> mainWidget, PosGuiData guiData, PanelSyncManager syncManager,
                            UISettings settings) {
        StringSyncValue serialized = new StringSyncValue(() -> String.join("\n", serializedTargets));
        syncManager.syncValue("gtna_tesseract_targets", serialized);

        var status = MUI2MachineDisplay.syncedLines(syncManager, "gtna_tesseract_status", lines -> {
            lines.add(Component.translatable("block.gtna.directed_tesseract_generator"));
            lines.add(Component.translatable("gtna.machine.directed_tesseract.target_count", targetCount));
            lines.add(Component.translatable("gtna.machine.directed_tesseract.pending", pendingInserts.size()));
            if (!lastRouteIssue.isBlank()) {
                lines.add(Component.translatable("gtna.machine.directed_tesseract.issue", getRouteIssueText()));
            }
            lines.addAll(getTargetDisplayText());
        });
        var highlight = new ButtonWidget<>()
                .size(88, 18)
                .onMousePressed((context, button) -> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT || serialized.getStringValue().isBlank()) {
                        return false;
                    }
                    List<TesseractDirectedTarget> targets = serialized.getStringValue().lines()
                            .filter(line -> !line.isBlank())
                            .map(TesseractDirectedTarget::deserialize)
                            .sorted(TesseractDirectedTarget.SORTER)
                            .toList();
                    HIGHLIGHTS.add(ImmutableList.copyOf(targets), 200);
                    return true;
                })
                .child(Text.lang("gtna.machine.directed_tesseract.highlight").asWidget().center());
        mainWidget.child(Flow.column().coverChildren().childPadding(3).child(status).child(highlight));
    }

    public List<TesseractDirectedTarget> getTargets() {
        List<TesseractDirectedTarget> targets = new ArrayList<>(serializedTargets.size());
        for (String serializedTarget : serializedTargets) {
            try {
                targets.add(TesseractDirectedTarget.deserialize(serializedTarget));
            } catch (RuntimeException exception) {
                GTNACORE.LOGGER.warn("[GTNA] Invalid serialized tesseract target '{}'", serializedTarget, exception);
            }
        }
        targets.sort(TesseractDirectedTarget.SORTER);
        return targets;
    }

    public void setTargets(List<TesseractDirectedTarget> targets) {
        serializedTargets.clear();
        targets.stream()
                .sorted(TesseractDirectedTarget.SORTER)
                .map(TesseractDirectedTarget::serialize)
                .forEach(serializedTargets::add);
        setTargetCount(serializedTargets.size());
        setLastRouteIssue("");
        setChanged();
    }

    private void updatePendingSubscription() {
        if (!pendingInserts.isEmpty()) {
            if (pendingInsertSubscription == null || !pendingInsertSubscription.isStillSubscribed()) {
                pendingInsertSubscription = subscribeServerTick(this::flushPendingInserts);
            }
        } else if (pendingInsertSubscription != null) {
            pendingInsertSubscription.unsubscribe();
            pendingInsertSubscription = null;
        }
    }

    private void flushPendingInserts() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < pendingInserts.size(); i++) {
            PendingInsert pendingInsert = pendingInserts.get(i);
            if (insertNow(pendingInsert, IActionSource.empty())) {
                pendingInserts.remove(i--);
            }
        }
        updatePendingSubscription();
        if (pendingInserts.isEmpty()) {
            setChanged();
        }
    }

    private boolean insertNow(PendingInsert pendingInsert, IActionSource actionSource) {
        MEStorage storage = resolveStorage(pendingInsert.target());
        if (storage == null) {
            GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} could not resolve storage for pending insert {}",
                    getBlockPos(), describeTarget(pendingInsert.target()));
            return false;
        }
        long inserted = insertIntoTarget(pendingInsert.target(), storage, pendingInsert.key(), pendingInsert.amount(),
                Actionable.MODULATE, actionSource);
        if (pendingInsert.key() instanceof AEFluidKey) {
            GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} modulated fluid insert {} / {} into {}",
                    getBlockPos(), inserted, pendingInsert.amount(), describeTarget(pendingInsert.target()));
        }
        return inserted == pendingInsert.amount();
    }

    private List<OrderedPush> getOrderedInputs(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        List<OrderedPush> ordered = new ArrayList<>();
        if (patternDetails instanceof AEProcessingPattern processingPattern) {
            appendSparseInputs(ordered, processingPattern.getSparseInputs());
            return ordered;
        }
        if (patternDetails instanceof IParallelPatternDetails parallelDetails) {
            IPatternDetails delegate = parallelDetails.getDelegate();
            if (delegate instanceof AEProcessingPattern processingPattern) {
                appendSparseInputs(ordered, scaleSparseInputs(
                        processingPattern.getSparseInputs(), parallelDetails.getParallel()));
                return ordered;
            }
        }
        if (patternDetails.supportsPushInputsToExternalInventory()) {
            patternDetails.pushInputsToExternalInventory(inputHolder, (key, amount) -> {
                if (amount > 0L) {
                    ordered.add(new OrderedPush(key, amount));
                }
            });
            return ordered;
        }
        for (KeyCounter keyCounter : inputHolder) {
            for (var entry : keyCounter) {
                if (entry.getLongValue() > 0L) {
                    ordered.add(new OrderedPush(entry.getKey(), entry.getLongValue()));
                }
            }
        }
        return ordered;
    }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            boolean illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()) && !id.equals(AEKeyType.fluids().getId()));
            if (illegal) {
                return false;
            }
        }
        return true;
    }

    private @Nullable PatternProviderTarget resolveTarget(TesseractDirectedTarget target, IActionSource actionSource) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.pos().dimension());
        if (targetLevel == null) {
            return null;
        }
        var blockEntity = targetLevel.getBlockEntity(target.pos().pos());
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }
        PatternProviderTarget directTarget = PatternProviderTarget.get(
                targetLevel, target.pos().pos(), blockEntity, target.face(), actionSource);
        if (directTarget != null) {
            return directTarget;
        }
        return PatternProviderTarget.get(
                targetLevel, target.pos().pos(), blockEntity, target.face().getOpposite(), actionSource);
    }

    private long insertIntoTarget(TesseractDirectedTarget target, MEStorage storage, AEKey key, long amount,
                                  Actionable actionable, IActionSource actionSource) {
        if (amount <= 0L) {
            return 0L;
        }
        long inserted = storage.insert(key, amount, actionable, actionSource);
        if (inserted > 0L || !(key instanceof AEFluidKey fluidKey)) {
            return inserted;
        }
        return insertFluidAcrossAllSides(target, fluidKey, amount, actionable, actionSource);
    }

    private @Nullable MEStorage resolveStorage(TesseractDirectedTarget target) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.pos().dimension());
        if (targetLevel == null) {
            return null;
        }
        var blockEntity = targetLevel.getBlockEntity(target.pos().pos());
        if (blockEntity == null || blockEntity.isRemoved()) {
            return null;
        }

        MEStorage directStorage = createStorage(blockEntity, target.face());
        if (directStorage != null) {
            return directStorage;
        }
        return createStorage(blockEntity, target.face().getOpposite());
    }

    private static @Nullable MEStorage createStorage(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                                                     net.minecraft.core.Direction side) {
        var level = blockEntity.getLevel();
        if (level == null) return null;
        var itemHandler = level.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), side);
        var fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), side);
        MEStorage itemStorage = itemHandler == null ? null : ExternalStorageFacade.of(itemHandler);
        MEStorage fluidStorage = fluidHandler == null ? null : ExternalStorageFacade.of(fluidHandler);
        if (itemStorage != null && fluidStorage != null) {
            return new CompositeStorage(Map.of(AEKeyType.items(), itemStorage, AEKeyType.fluids(), fluidStorage));
        }
        if (itemStorage != null) {
            return itemStorage;
        }
        return fluidStorage;
    }

    private long insertFluidAcrossAllSides(TesseractDirectedTarget target, AEFluidKey fluidKey, long amount,
                                           Actionable actionable,
                                           IActionSource actionSource) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return 0L;
        }
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.pos().dimension());
        if (targetLevel == null) {
            return 0L;
        }
        var blockEntity = targetLevel.getBlockEntity(target.pos().pos());
        if (blockEntity == null || blockEntity.isRemoved()) {
            return 0L;
        }

        logFluidTargetDiagnostics(target, blockEntity, fluidKey, amount);

        List<Direction> orderedSides = new ArrayList<>(8);
        orderedSides.add(target.face());
        orderedSides.add(target.face().getOpposite());
        for (Direction direction : Direction.values()) {
            if (!orderedSides.contains(direction)) {
                orderedSides.add(direction);
            }
        }

        for (Direction direction : orderedSides) {
            MEStorage storage = createStorage(blockEntity, direction);
            if (storage == null) {
                continue;
            }
            long inserted = storage.insert(fluidKey, amount, actionable, actionSource);
            if (inserted > 0L) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} inserted fluid {} / {} into {} via fallback side {}",
                        getBlockPos(), inserted, amount, describeTarget(target), direction);
                return inserted;
            }
        }
        MEStorage nullSideStorage = createStorage(blockEntity, null);
        if (nullSideStorage != null) {
            long inserted = nullSideStorage.insert(fluidKey, amount, actionable, actionSource);
            if (inserted > 0L) {
                GTNACORE.LOGGER.debug("[GTNA] Directed Tesseract {} inserted fluid {} / {} into {} via fallback null side",
                        getBlockPos(), inserted, amount, describeTarget(target));
                return inserted;
            }
        }
        return 0L;
    }

    private void logFluidTargetDiagnostics(TesseractDirectedTarget target,
                                           net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                                           AEFluidKey fluidKey, long amount) {
        String machineInfo = "none";
        if (blockEntity instanceof MetaMachine metaMachine) {
            machineInfo = metaMachine.getClass().getSimpleName() + " front=" + metaMachine.getFrontFacing();
        }
        GTNACORE.LOGGER.debug("[GTNA] Fluid diagnostics for {} blockEntity={} block={} machine={} amount={} fluid={}",
                describeTarget(target),
                blockEntity.getClass().getName(),
                blockEntity.getBlockState().getBlock(),
                machineInfo,
                amount,
                fluidKey);

        logFluidSideDiagnostics(blockEntity, target, fluidKey, amount, null, "null");
        for (Direction direction : Direction.values()) {
            logFluidSideDiagnostics(blockEntity, target, fluidKey, amount, direction, direction.getName());
        }
    }

    private void logFluidSideDiagnostics(net.minecraft.world.level.block.entity.BlockEntity blockEntity,
                                         TesseractDirectedTarget target, AEFluidKey fluidKey, long amount,
                                         @Nullable Direction direction, String label) {
        var level = blockEntity.getLevel();
        IFluidHandler handler = level == null ? null :
                level.getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), direction);
        if (handler == null) {
            GTNACORE.LOGGER.debug("[GTNA] Fluid diagnostics {} side={} handler=false", describeTarget(target), label);
            return;
        }

        int accepted = handler.fill(fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, amount)),
                IFluidHandler.FluidAction.SIMULATE);
        GTNACORE.LOGGER.debug("[GTNA] Fluid diagnostics {} side={} handler=true tanks={} accepted={} / {}",
                describeTarget(target),
                label,
                handler.getTanks(),
                accepted,
                amount);
    }

    private List<Component> getTargetDisplayText() {
        List<TesseractDirectedTarget> targets = getTargets();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gtna.machine.directed_tesseract.ui.provider"));
        lines.add(Component.translatable("gtna.machine.directed_tesseract.ui.order"));
        if (targets.isEmpty()) {
            lines.add(Component.translatable("gtna.machine.directed_tesseract.ui.empty"));
            return lines;
        }
        lines.add(Component.empty());
        lines.add(Component.translatable("gtna.machine.directed_tesseract.ui.bound_targets"));
        for (TesseractDirectedTarget target : targets) {
            var pos = target.pos().pos();
            lines.add(Component.translatable("gtna.machine.directed_tesseract.ui.target",
                    target.order(),
                    Component.translatable("gtna.machine.directed_tesseract.direction." + target.face().getName()),
                    pos.getX(), pos.getY(), pos.getZ(), target.pos().dimension().location(),
                    Component.translatable(inspectTargetCapabilities(target).translationKey())));
        }
        return lines;
    }

    private Component getRouteIssueText() {
        String[] parts = lastRouteIssue.split(ROUTE_ISSUE_SEPARATOR, 3);
        if (parts.length != 3) {
            return Component.translatable("gtna.machine.directed_tesseract.issue.unknown");
        }
        return Component.translatable("gtna.machine.directed_tesseract.issue.incompatible_route",
                parts[0], Component.translatable(parts[1]), parts[0], Component.translatable(parts[2]));
    }

    private void highlightTargets() {
        HIGHLIGHTS.add(ImmutableList.copyOf(getTargets()), 200);
    }

    private static void appendSparseInputs(List<OrderedPush> ordered, List<GenericStack> sparseInputs) {
        for (GenericStack sparseInput : sparseInputs) {
            if (sparseInput == null || sparseInput.amount() <= 0L) {
                continue;
            }
            ordered.add(new OrderedPush(sparseInput.what(), sparseInput.amount()));
        }
    }

    private static List<GenericStack> scaleSparseInputs(List<GenericStack> sparseInputs, long multiplier) {
        List<GenericStack> scaled = new ArrayList<>(sparseInputs.size());
        for (GenericStack sparseInput : sparseInputs) {
            if (sparseInput == null) {
                scaled.add(null);
                continue;
            }
            scaled.add(new GenericStack(sparseInput.what(), safeMultiply(sparseInput.amount(), multiplier)));
        }
        return scaled;
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private static String describeTarget(TesseractDirectedTarget target) {
        return formatTargetLine(target);
    }

    private static String formatTargetLine(TesseractDirectedTarget target) {
        ResourceKey<Level> dim = target.pos().dimension();
        var pos = target.pos().pos();
        return "#" + target.order() + " " + target.face().getName() + " @ " + pos.getX() + ", " + pos.getY() + ", " +
                pos.getZ() + " [" + dim.location() + "]";
    }

    private TargetCapabilitySummary inspectTargetCapabilities(TesseractDirectedTarget target) {
        if (!(getLevel() instanceof ServerLevel serverLevel)) {
            return TargetCapabilitySummary.UNKNOWN;
        }
        ServerLevel targetLevel = serverLevel.getServer().getLevel(target.pos().dimension());
        if (targetLevel == null) {
            return TargetCapabilitySummary.UNKNOWN;
        }
        var blockEntity = targetLevel.getBlockEntity(target.pos().pos());
        if (blockEntity == null || blockEntity.isRemoved()) {
            return TargetCapabilitySummary.UNKNOWN;
        }
        if (blockEntity instanceof MetaMachine metaMachine) {
            String machineName = metaMachine.getClass().getSimpleName();
            if (machineName.contains("FluidHatchPartMachine")) {
                return TargetCapabilitySummary.FLUID_HATCH;
            }
            if (machineName.contains("ItemBusPartMachine")) {
                return TargetCapabilitySummary.ITEM_BUS;
            }
        }

        boolean item = false;
        boolean fluid = false;
        for (Direction direction : Direction.values()) {
            item |= targetLevel.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), direction) != null;
            fluid |= targetLevel.getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), direction) != null;
        }
        item |= targetLevel.getCapability(Capabilities.ItemHandler.BLOCK, blockEntity.getBlockPos(), null) != null;
        fluid |= targetLevel.getCapability(Capabilities.FluidHandler.BLOCK, blockEntity.getBlockPos(), null) != null;

        if (item && fluid) {
            return TargetCapabilitySummary.BOTH;
        }
        if (fluid) {
            return TargetCapabilitySummary.FLUID;
        }
        if (item) {
            return TargetCapabilitySummary.ITEM;
        }
        return TargetCapabilitySummary.NONE;
    }

    private static boolean isTargetCompatible(AEKey key, TargetCapabilitySummary capabilitySummary) {
        if (key instanceof AEFluidKey) {
            return capabilitySummary.acceptsFluid();
        }
        return capabilitySummary.acceptsItem();
    }

    private static String inputKindKey(AEKey key) {
        return key instanceof AEFluidKey ? "gtna.machine.directed_tesseract.capability.fluid" :
                "gtna.machine.directed_tesseract.capability.item";
    }

    private record OrderedPush(AEKey key, long amount) {}

    private record PendingInsert(TesseractDirectedTarget target, AEKey key, long amount) {}

    private enum TargetCapabilitySummary {
        ITEM("gtna.machine.directed_tesseract.capability.item"),
        FLUID("gtna.machine.directed_tesseract.capability.fluid"),
        ITEM_BUS("gtna.machine.directed_tesseract.capability.item_bus"),
        FLUID_HATCH("gtna.machine.directed_tesseract.capability.fluid_hatch"),
        BOTH("gtna.machine.directed_tesseract.capability.both"),
        NONE("gtna.machine.directed_tesseract.capability.none"),
        UNKNOWN("gtna.machine.directed_tesseract.capability.unknown");

        private final String translationKey;

        TargetCapabilitySummary(String translationKey) {
            this.translationKey = translationKey;
        }

        public boolean acceptsItem() {
            return this == ITEM || this == ITEM_BUS || this == BOTH || this == UNKNOWN;
        }

        public boolean acceptsFluid() {
            return this == FLUID || this == FLUID_HATCH || this == BOTH || this == UNKNOWN;
        }

        public String translationKey() {
            return translationKey;
        }
    }
}
