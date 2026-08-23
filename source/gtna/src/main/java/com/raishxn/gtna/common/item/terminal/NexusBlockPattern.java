package com.raishxn.gtna.common.item.terminal;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.multiblock.PatternPredicate;
import com.gregtechceu.gtceu.api.multiblock.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternSlice;
import com.gregtechceu.gtceu.api.multiblock.predicates.BasePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import com.raishxn.gtna.common.item.terminal.ui.BlockSelectionConfigWidget;
import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Survival auto-builder for the Nexus Structure Terminal.
 *
 * <p>GT 8 replaced the old three-dimensional predicate array with slices and a character-to-predicate map. This
 * wrapper consumes that public model instead of reflecting into GT internals.</p>
 */
public final class NexusBlockPattern {

    private static final Direction[] FACINGS = { Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST,
            Direction.UP, Direction.DOWN };
    private static final Direction[] HORIZONTAL_FACINGS = { Direction.SOUTH, Direction.NORTH, Direction.WEST,
            Direction.EAST };

    private final BlockPattern pattern;
    private final PatternSlice[] slices;
    private final RelativeDirection[] directions;

    private NexusBlockPattern(BlockPattern pattern) {
        this.pattern = pattern;
        this.slices = pattern.getSlices();
        this.directions = pattern.getDirections();
    }

    @Nullable
    public static NexusBlockPattern fromBlockPattern(IBlockPattern pattern) {
        return pattern instanceof BlockPattern blockPattern ? new NexusBlockPattern(blockPattern) : null;
    }

    public void autoBuild(Player player, MultiblockControllerMachine controller,
                          NexusTerminalUIFactory.AutoBuildSetting setting, ItemStack terminalStack) {
        Level level = player.level();
        BlockPos controllerPos = controller.getBlockPos();
        Direction front = controller.getFrontFacing();
        Direction up = controller.getUpwardsFacing();
        boolean flipped = controller.isFlipped();

        Direction sliceDirection = directions[0].getRelativeFacing(front, up, flipped);
        Direction stringDirection = directions[1].getRelativeFacing(front, up, flipped);
        Direction charDirection = directions[2].getRelativeFacing(front, up, flipped);
        BlockPos.MutableBlockPos start = controllerPos.mutable();
        pattern.getOffset().apply(start, front, up, flipped);

        Object2IntOpenHashMap<BasePredicate> globalCounts = new Object2IntOpenHashMap<>();
        Map<BlockPos, Object> encountered = new HashMap<>();
        Set<BlockPos> placedPositions = new HashSet<>();
        encountered.put(controllerPos, controller);

        int sliceOffset = 0;
        for (PatternSlice slice : slices) {
            int repetitions = slice.getMinRepeats() == slice.getMaxRepeats() ? slice.getMinRepeats() :
                    Math.max(slice.getMinRepeats(), Math.min(slice.getMaxRepeats(), setting.getRepetitions()));
            for (int repetition = 0; repetition < repetitions; repetition++, sliceOffset++) {
                Object2IntOpenHashMap<BasePredicate> layerCounts = new Object2IntOpenHashMap<>();
                BlockPos.MutableBlockPos rowStart = start.mutable().move(sliceDirection, sliceOffset);
                char[][] shape = slice.getPattern();
                for (int row = 0; row < shape.length; row++) {
                    BlockPos.MutableBlockPos current = rowStart.mutable().move(stringDirection, row);
                    for (int column = 0; column < shape[row].length; column++, current.move(charDirection)) {
                        PatternPredicate predicate = pattern.getPredicates().get(shape[row][column]);
                        if (predicate == null || predicate == PatternPredicate.ANY || predicate.isController()) continue;
                        buildPosition(player, level, current.immutable(), predicate, globalCounts, layerCounts,
                                setting, terminalStack, encountered, placedPositions);
                    }
                }
            }
        }

        orientPlacedBlocks(level, controller, encountered, placedPositions);
        controller.getDefaultPatternState().getCache().clear();
        controller.checkAndFormStructure();
    }

    private void buildPosition(Player player, Level level, BlockPos pos, PatternPredicate predicate,
                               Object2IntOpenHashMap<BasePredicate> globalCounts,
                               Object2IntOpenHashMap<BasePredicate> layerCounts,
                               NexusTerminalUIFactory.AutoBuildSetting setting, ItemStack terminalStack,
                               Map<BlockPos, Object> encountered, Set<BlockPos> placedPositions) {
        BlockState previousState = level.getBlockState(pos);
        BasePredicate selected = selectPredicate(predicate, previousState, globalCounts, layerCounts,
                setting.isNoHatchMode());
        List<BlockInfo> infos = selected == null ? collectCandidates(predicate, setting.isNoHatchMode()) :
                selected.getCandidates();
        List<ItemStack> candidates = applySetting(infos, terminalStack);

        if (!previousState.isAir()) {
            incrementMatchingPredicate(predicate, previousState, globalCounts, layerCounts);
            if (!setting.isReplaceMode()) {
                encountered.put(pos, machineOrState(level, pos, previousState));
                return;
            }
            ItemStack existing = previousState.getBlock().asItem().getDefaultInstance();
            if (candidates.stream().anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, existing))) {
                encountered.put(pos, machineOrState(level, pos, previousState));
                return;
            }
        }

        FoundItem found = findItem(player, level, terminalStack, candidates, setting.isUseAE());
        if (found == null || !(found.stack().getItem() instanceof BlockItem blockItem)) return;

        ItemStack replacedStack = previousState.isAir() ? ItemStack.EMPTY :
                previousState.getBlock().asItem().getDefaultInstance();
        IItemHandler playerInventory = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        if (!replacedStack.isEmpty() && !player.isCreative() && playerInventory != null &&
                !ItemHandlerHelper.insertItemStacked(playerInventory, replacedStack.copy(), true).isEmpty()) return;

        if (!previousState.isAir()) level.destroyBlock(pos, false);

        ItemStack placementStack = found.stack().copy();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        InteractionResult result = blockItem.place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                placementStack, hit));
        if (!result.consumesAction()) {
            if (!previousState.isAir()) level.setBlock(pos, previousState, 3);
            return;
        }

        if (found.handler() != null && found.slot() >= 0) found.handler().extractItem(found.slot(), 1, false);
        if (!replacedStack.isEmpty() && !player.isCreative()) {
            ItemStack remainder = playerInventory == null ? replacedStack :
                    ItemHandlerHelper.insertItemStacked(playerInventory, replacedStack, false);
            if (!remainder.isEmpty()) player.drop(remainder, false);
        }
        if (selected != null) {
            globalCounts.addTo(selected, 1);
            layerCounts.addTo(selected, 1);
        }
        placedPositions.add(pos);
        encountered.put(pos, machineOrState(level, pos, level.getBlockState(pos)));
    }

    private static Object machineOrState(Level level, BlockPos pos, BlockState state) {
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        return machine == null ? state : machine;
    }

    @Nullable
    private BasePredicate selectPredicate(PatternPredicate predicate, BlockState existing,
                                          Object2IntOpenHashMap<BasePredicate> globalCounts,
                                          Object2IntOpenHashMap<BasePredicate> layerCounts, boolean noHatch) {
        List<BasePredicate> eligible = predicate.subPredicates.stream()
                .filter(base -> !base.getCandidates().isEmpty())
                .filter(base -> candidatesAllowed(base.getCandidates(), noHatch)).toList();
        if (!existing.isAir()) {
            BasePredicate matching = findMatchingPredicate(eligible, existing);
            if (matching != null) return matching;
        }
        for (BasePredicate base : eligible) {
            if (base.minSliceCount > 0 && layerCounts.getInt(base) < base.minSliceCount &&
                    belowLimits(base, globalCounts, layerCounts)) return base;
        }
        for (BasePredicate base : eligible) {
            if (base.minCount > 0 && globalCounts.getInt(base) < base.minCount &&
                    belowLimits(base, globalCounts, layerCounts)) return base;
        }
        for (BasePredicate base : eligible) {
            if (belowLimits(base, globalCounts, layerCounts)) return base;
        }
        return null;
    }

    private static boolean belowLimits(BasePredicate predicate, Object2IntOpenHashMap<BasePredicate> global,
                                       Object2IntOpenHashMap<BasePredicate> layer) {
        return (predicate.maxCount < 0 || global.getInt(predicate) < predicate.maxCount) &&
                (predicate.maxSliceCount < 0 || layer.getInt(predicate) < predicate.maxSliceCount);
    }

    private void incrementMatchingPredicate(PatternPredicate predicate, BlockState state,
                                            Object2IntOpenHashMap<BasePredicate> global,
                                            Object2IntOpenHashMap<BasePredicate> layer) {
        BasePredicate matching = findMatchingPredicate(predicate.subPredicates, state);
        if (matching != null) {
            global.addTo(matching, 1);
            layer.addTo(matching, 1);
        }
    }

    @Nullable
    private static BasePredicate findMatchingPredicate(List<BasePredicate> predicates, BlockState state) {
        for (BasePredicate predicate : predicates) {
            if (predicate.getCandidates().stream().anyMatch(info -> info.getBlockState().getBlock() ==
                    state.getBlock())) return predicate;
        }
        return null;
    }

    private List<BlockInfo> collectCandidates(PatternPredicate predicate, boolean noHatch) {
        List<BlockInfo> candidates = new ArrayList<>();
        for (BasePredicate base : predicate.subPredicates) {
            if (candidatesAllowed(base.getCandidates(), noHatch)) candidates.addAll(base.getCandidates());
        }
        return candidates;
    }

    private boolean candidatesAllowed(List<BlockInfo> candidates, boolean noHatch) {
        return !noHatch || candidates.stream().noneMatch(this::isHatchBlock);
    }

    private List<ItemStack> applySetting(List<BlockInfo> infos, ItemStack terminalStack) {
        if (infos.isEmpty()) return List.of();
        BlockSelectionConfigWidget.BlockCategory category = null;
        if (infos.stream().anyMatch(info -> info.getBlockState().getBlock() instanceof CoilBlock)) {
            category = BlockSelectionConfigWidget.BlockCategory.COILS;
        } else if (hasDescription(infos, "me_storage_core")) {
            category = BlockSelectionConfigWidget.BlockCategory.MATRIX_STORAGE_MODULE;
        } else if (hasDescription(infos, "crafting_storage_core")) {
            category = BlockSelectionConfigWidget.BlockCategory.MATRIX_CRAFTING_MODULE;
        } else if (hasAnyDescription(infos, "me_storage_access_hatch", "me_big_storage_access_hatch",
                "me_io_port_hatch")) {
            category = BlockSelectionConfigWidget.BlockCategory.ME_STORAGE_ACCESS;
        } else if (hasDescription(infos, "machine_casing")) {
            category = BlockSelectionConfigWidget.BlockCategory.MACHINE_CASING;
        } else if (infos.stream().anyMatch(info -> info.getBlockState().getBlock() instanceof
                com.raishxn.gtna.common.block.NexusCapacitorBlock)) {
            category = BlockSelectionConfigWidget.BlockCategory.WIRELESS_CAPACITOR;
        } else if (infos.stream().anyMatch(this::isHatchBlock)) {
            if (hasDescription(infos, "muffler")) category = BlockSelectionConfigWidget.BlockCategory.MUFFLER;
            else if (hasDescription(infos, "rotor")) category = BlockSelectionConfigWidget.BlockCategory.ROTOR_HOLDER;
        }
        if (category != null) {
            ItemStack selected = BlockSelectionConfigWidget.getSelectedBlock(terminalStack, category);
            if (selected != null && !selected.isEmpty()) return List.of(selected.copy());
        }
        return infos.stream().filter(info -> !info.getBlockState().isAir()).map(BlockInfo::getItemStackForm)
                .filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
    }

    private static boolean hasDescription(List<BlockInfo> infos, String text) {
        return infos.stream().anyMatch(info -> info.getItemStackForm().getDescriptionId().contains(text));
    }

    private static boolean hasAnyDescription(List<BlockInfo> infos, String... text) {
        return Arrays.stream(text).anyMatch(value -> hasDescription(infos, value));
    }

    private boolean isHatchBlock(BlockInfo info) {
        if (!(info.getBlockState().getBlock() instanceof MetaMachineBlock machineBlock)) return false;
        try {
            return machineBlock.getDefinition().getBlockEntityType().create(BlockPos.ZERO,
                    machineBlock.defaultBlockState()) instanceof MultiblockPartMachine;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Nullable
    private FoundItem findItem(Player player, Level level, ItemStack terminal, List<ItemStack> candidates,
                               boolean useAE) {
        if (candidates.isEmpty()) return null;
        if (useAE && com.raishxn.gtna.integration.ae2.NexusAE2Link.isAE2Available()) {
            ItemStack extracted = com.raishxn.gtna.integration.ae2.NexusAE2Link
                    .extractItem(terminal, level, player, candidates);
            if (extracted != null && !extracted.isEmpty()) return new FoundItem(extracted, null, -1);
        }
        if (player.isCreative()) {
            return candidates.stream().filter(stack -> stack.getItem() instanceof BlockItem).findFirst()
                    .map(stack -> new FoundItem(stack.copy(), null, -1)).orElse(null);
        }
        IItemHandler inventory = player.getCapability(Capabilities.ItemHandler.ENTITY, null);
        IntObjectPair<IItemHandler> match = findMatchingStack(candidates, inventory, 0);
        return match == null ? null : new FoundItem(match.second().getStackInSlot(match.firstInt()).copy(),
                match.second(), match.firstInt());
    }

    @Nullable
    private static IntObjectPair<IItemHandler> findMatchingStack(List<ItemStack> candidates,
                                                                  @Nullable IItemHandler handler, int depth) {
        if (handler == null || depth > 8) return null;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            IItemHandler nested = stack.getCapability(Capabilities.ItemHandler.ITEM, null);
            IntObjectPair<IItemHandler> nestedMatch = findMatchingStack(candidates, nested, depth + 1);
            if (nestedMatch != null) return nestedMatch;
            if (stack.getItem() instanceof BlockItem && candidates.stream()
                    .anyMatch(candidate -> ItemStack.isSameItemSameComponents(candidate, stack))) {
                return IntObjectPair.of(slot, handler);
            }
        }
        return null;
    }

    private void orientPlacedBlocks(Level level, MultiblockControllerMachine controller,
                                    Map<BlockPos, Object> encountered, Set<BlockPos> placed) {
        Direction preferred = controller.getFrontFacing();
        encountered.forEach((pos, object) -> {
            if (object instanceof MultiblockControllerMachine) return;
            if (object instanceof MetaMachine machine) {
                Direction facing = findFacing(pos, preferred, (p, direction) -> {
                    Object neighbor = encountered.get(p.relative(direction));
                    return (neighbor == null || neighbor instanceof BlockState state && state.isAir()) &&
                            machine.isFacingValid(direction);
                }, FACINGS);
                if (facing != null) machine.setFrontFacing(facing);
            } else if (object instanceof BlockState state && placed.contains(pos)) {
                resetFacing(pos, state, preferred, (p, direction) -> {
                    Object neighbor = encountered.get(p.relative(direction));
                    return neighbor == null || neighbor instanceof BlockState neighborState && neighborState.isAir();
                }, newState -> level.setBlock(pos, newState, 3));
            }
        });
    }

    private static void resetFacing(BlockPos pos, BlockState state, Direction preferred,
                                    BiPredicate<BlockPos, Direction> checker, Consumer<BlockState> consumer) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = findFacing(pos, preferred, checker,
                    ArrayUtils.addAll(new Direction[] { preferred }, FACINGS));
            if (facing != null) consumer.accept(state.setValue(BlockStateProperties.FACING, facing));
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction[] facings = preferred.getAxis().isHorizontal() ?
                    ArrayUtils.addAll(new Direction[] { preferred }, HORIZONTAL_FACINGS) : HORIZONTAL_FACINGS;
            Direction facing = findFacing(pos, preferred, checker, facings);
            if (facing != null) consumer.accept(state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing));
        }
    }

    @Nullable
    private static Direction findFacing(BlockPos pos, Direction preferred, BiPredicate<BlockPos, Direction> checker,
                                        Direction[] facings) {
        for (Direction facing : facings) if (checker.test(pos, facing)) return facing;
        return preferred;
    }

    private record FoundItem(ItemStack stack, @Nullable IItemHandler handler, int slot) {}
}
