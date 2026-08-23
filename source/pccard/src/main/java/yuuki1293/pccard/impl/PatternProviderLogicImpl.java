package yuuki1293.pccard.impl;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEKey;
import appeng.parts.storagebus.StorageBusPart;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.machine.trait.ProgrammableCircuitSlotTrait;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import yuuki1293.pccard.ConfigCommon;
import yuuki1293.pccard.PCCard;
import yuuki1293.pccard.wrapper.IPatternProviderLogicMixin;
import yuuki1293.pccard.TagUtils;
import yuuki1293.pccard.wrapper.IAEPattern;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PatternProviderLogicImpl {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static ItemStack updatePatterns(IPatternProviderLogicMixin self, ItemStack stack) {
        if (self.pCCard$hasPCCard()) {
            var newStack = stack.copy();
            var number = TagUtils.extractCircuitNumber(newStack);
            if (number >= 0) {
                newStack.set(PCCard.RECIPE_CIRCUIT, number);
            }

            return newStack;
        }

        return stack;
    }

    public static boolean isProgrammedCircuit(AEKey key) {
        return key != null && key.getId().equals(GTItems.PROGRAMMED_CIRCUIT.getId());
    }

    public static Optional<Integer> getCircuitNumber(IPatternDetails patternDetails) {
        return Optional.ofNullable(patternDetails.getDefinition().get(PCCard.RECIPE_CIRCUIT.get()));
    }

    public static void setPCNumber(ProgrammableCircuitSlotTrait circuitSlot, int number) {
        if (circuitSlot != null) {
            circuitSlot.setCurrentCircuit(number);
        }
    }

    public static void setPCNumber(IPatternProviderLogicMixin self, IPatternDetails patternDetails) {
        try {
            if (!self.pCCard$hasPCCard()) return;

            // Read the component first: Nexus' parallel pattern wrapper delegates its definition,
            // but intentionally does not implement PCC's IAEPattern extension.
            var circuitNumber = getCircuitNumber(patternDetails);
            if (circuitNumber.isEmpty() && patternDetails instanceof IAEPattern aePattern) {
                // Compatibility for crafting jobs restored from PCC's legacy NBT path.
                circuitNumber = Optional.of(aePattern.pCCard$getNumber());
            }
            if (circuitNumber.isEmpty()) return;

            var be = self.pCCard$getBlockEntity();
            var level = be.getLevel();
            if (level == null) return;

            var blockPoses = self.pCCard$getSendPos();
            int number = circuitNumber.get();
            for (var blockPos : blockPoses) {
                var gtMachine = MetaMachine.getMachine(level, blockPos);
                if (gtMachine == null) continue;
                gtMachine.getTraitOptional(ProgrammableCircuitSlotTrait.TYPE)
                        .ifPresent(trait -> setPCNumber(trait, number));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to set PC number", e);
        }
    }

    /**
     * get BlockPos which ingredient are sent. include subnet.
     * @param self caller
     * @param parent Class of mixin target
     * @return all send posses
     */
    public static List<BlockPos> getSendPos(Level level, IPatternProviderLogicMixin self, Class<?> parent) {
        var root = getSendPosDirect(self, parent);
        if (root == null) {
            return List.of();
        }
        var results = new ArrayList<BlockPos>();
        var visited = new HashSet<PosSide>();
        var queue = new ArrayDeque<SearchNode>();
        queue.add(new SearchNode(new PosSide(root.getA(), root.getB()), 0));

        int maximumDepth = ConfigCommon.getSearchDepth();
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            if (!visited.add(current.target())) {
                continue;
            }

            if (current.depth() >= maximumDepth) {
                results.add(current.target().pos());
                continue;
            }

            var children = getSendPosSubnet(
                level,
                current.target().pos(),
                current.target().direction().getOpposite());
            if (children.isEmpty()) {
                results.add(current.target().pos());
                continue;
            }

            for (var child : children) {
                queue.addLast(new SearchNode(new PosSide(child.getA(), child.getB()), current.depth() + 1));
            }
        }

        return results.isEmpty() ? List.of(root.getA()) : List.copyOf(results);
    }

    /**
     * support MAE2 pattern p2p
     */
    public static Tuple<BlockPos, Direction> getSendPosDirect(IPatternProviderLogicMixin self, Class<?> parent) {
        try {
            var dir = self.pCCard$getSendDirection();
            if (dir == null) {
                LOGGER.warn("Skipping programmed-circuit update because {} has no direction for this push",
                    parent.getName());
                return null;
            }

            // stone.mae2.mixins.PatternProviderLogicMixin.pushPattern
            if (Arrays.stream(parent.getDeclaredFields()).anyMatch(f -> f.getName().equals("sendPos"))) {
                var posFiled = parent.getDeclaredField("sendPos");
                posFiled.setAccessible(true);
                var pos = posFiled.get(self);

                if (pos != null)
                    return new Tuple<>((BlockPos) posFiled.get(self), dir);
            }

            var be = self.pCCard$getBlockEntity();

            return new Tuple<>(be.getBlockPos().relative(dir), dir);
        } catch (Exception e) {
            LOGGER.error("Error while getting sendPos", e);
            return null;
        }
    }

    /**
     * get BlockPos which ingredient are sent in subnet.
     * @param level level
     * @param pos interface pos
     * @param side interface side
     * @return storage bus dest
     */
    public static List<Tuple<BlockPos, Direction>> getSendPosSubnet(Level level, BlockPos pos, Direction side) {
        var host = getActionHost(level, pos, side);
        var grid = getGrid(host);
        var parts = getStorageBusParts(grid);
        return getBlockPoses(parts);
    }

    /**
     * get action host from blockEntity or part
     */
    private static IActionHost getActionHost(Level level, BlockPos pos, Direction side) {
        var be = level.getBlockEntity(pos);

        if (be instanceof IActionHost host) return host;

        if (be instanceof IPartHost partHost) {
            var part = partHost.getPart(side);
            if (part instanceof IActionHost host) return host;
        }

        return null;
    }

    /**
     * get Grid
     */
    private static IGrid getGrid(IActionHost host) {
        if (host == null) return null;

        var node = host.getActionableNode();
        if (node != null) {
            return node.getGrid();
        }
        return null;
    }

    /**
     * get all StorageBusPart in grid
     */
    private static Set<StorageBusPart> getStorageBusParts(IGrid grid) {
        if (grid == null) {
            return Set.of();
        }

        return grid.getMachines(StorageBusPart.class);
    }

    /**
     * get BlockPos es from storageBusPart list
     */
    private static List<Tuple<BlockPos, Direction>> getBlockPoses(Iterable<StorageBusPart> parts) {
        var poses = new ArrayList<Tuple<BlockPos, Direction>>();

        for (var part : parts) {
            var pos = part.getBlockEntity().getBlockPos();
            var side = part.getSide();
            var machinePos = pos.relative(side);
            poses.add(new Tuple<>(machinePos, side.getOpposite()));
        }

        return poses;
    }

    private record PosSide(BlockPos pos, Direction direction) {}

    private record SearchNode(PosSide target, int depth) {}
}
