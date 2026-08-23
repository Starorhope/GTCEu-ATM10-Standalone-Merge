package com.raishxn.gtna.common.item;

import com.gregtechceu.gtceu.api.item.component.IInteractionItem;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError;
import com.gregtechceu.gtceu.common.item.behavior.TooltipBehavior;

import com.raishxn.gtna.network.GTNANetworkHandler;
import com.raishxn.gtna.network.packet.SStructureDetectHighlight;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class StructureDetectBehavior extends TooltipBehavior implements IInteractionItem {

    private static final ReentrantLock LOCK = new ReentrantLock();
    public static final StructureDetectBehavior INSTANCE = new StructureDetectBehavior(lines -> {
        lines.add(Component.translatable("structure_detect.tooltip.0"));
        lines.add(Component.translatable("structure_detect.tooltip.1").withStyle(ChatFormatting.GRAY));
    });

    public StructureDetectBehavior(@NotNull Consumer<List<Component>> tooltips) {
        super(tooltips);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (player != null) {
            Level level = context.getLevel();
            if (level.isClientSide) return InteractionResult.PASS;
            BlockPos blockPos = context.getClickedPos();
            if (MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller) {
                if (controller.isFormed()) {
                    player.sendSystemMessage(Component.translatable("message.gtnacore.structure_formed")
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    boolean isFlipped = !tag.isEmpty() && tag.getBoolean("isFlipped");
                    ((ServerLevel) level).getServer().execute(() -> {
                        var pattern = controller.getDefaultStructurePattern();
                        LOCK.lock();
                        try {
                            var result = check(controller, pattern, isFlipped);
                            for (var patternError : result) {
                                showError(player, patternError, isFlipped, level);
                            }
                        } finally {
                            LOCK.unlock();
                        }
                    });
                    return InteractionResult.SUCCESS;
                }
            } else if (player instanceof ServerPlayer serverPlayer) {
                boolean newFlipped = !tag.getBoolean("isFlipped");
                CustomData.update(DataComponents.CUSTOM_DATA, stack,
                        data -> data.putBoolean("isFlipped", newFlipped));
                serverPlayer.displayClientMessage(Component.translatable(
                        newFlipped ? "message.gtna.detection_mode_mirrored" : "message.gtna.detection_mode_normal"),
                        true);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private List<PatternError> check(MultiblockControllerMachine controller, IBlockPattern pattern,
                                     boolean isFlipped) {
        var errors = new ObjectArrayList<PatternError>();
        if (controller == null) {
            errors.add(new PatternStringError(Component.translatable("gtna.message.structure_detect.no_controller")));
            return errors;
        }
        var centerPos = controller.getBlockPos();
        var frontFacing = controller.getFrontFacing();
        var facings = controller.hasFrontFacing() ? new Direction[] { frontFacing } :
                new Direction[] { Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST };
        var upwardsFacing = controller.getUpwardsFacing();

        for (var direction : facings) {
            var patternState = new PatternState();
            patternState.setController(controller, centerPos);
            pattern.checkPatternAt(controller.getLevel(), patternState, centerPos, direction, upwardsFacing,
                    isFlipped);
            if (patternState.hasErrors()) {
                errors.addAll(patternState.getErrors());
            }
        }
        return errors;
    }

    private void showError(Player player, PatternError error, boolean flip, Level level) {
        var show = new ObjectArrayList<Component>();
        if (error instanceof PatternStringError pe) {
            player.sendSystemMessage(pe.getComponent());
            return;
        }
        var pos = error.getPos();
        if (pos == null) {
            if (error instanceof SinglePredicateError pe) {
                player.sendSystemMessage(Component.translatable(pe.debugName).withStyle(ChatFormatting.RED));
            } else {
                player.sendSystemMessage(Component.literal(error.type().id().toString()).withStyle(ChatFormatting.RED));
            }
            return;
        }
        var posComponent = Component.translatable("item.gtna.structure_detect.error.2", pos.getX(), pos.getY(),
                pos.getZ(), flip ?
                        Component.translatable("item.gtna.structure_detect.error.3").withStyle(ChatFormatting.GREEN) :
                        Component.translatable("item.gtna.structure_detect.error.4").withStyle(ChatFormatting.YELLOW));
        var candidates = error.getCandidates();

        if (error instanceof SinglePredicateError) {
            if (!candidates.isEmpty() && !candidates.get(0).isEmpty()) {
                var root = candidates.get(0).get(0).getItemStackForm().getHoverName();
                show.add(Component.translatable("item.gtna.structure_detect.error.1", posComponent));
                show.add(Component.literal(" - ").append(root));
            }
        } else {
            show.add(Component.translatable("item.gtna.structure_detect.error.0", posComponent));
            for (var candidate : candidates) {
                if (!candidate.isEmpty()) {
                    show.add(Component.literal(" - ").append(candidate.get(0).getItemStackForm().getHoverName()));
                }
            }
        }
        show.forEach(player::sendSystemMessage);
        if (player instanceof ServerPlayer serverPlayer) {
            GTNANetworkHandler.sendToPlayer(
                    new SStructureDetectHighlight(error.getPos(), level.dimension(),
                            System.currentTimeMillis() + 15000),
                    serverPlayer);
        }
    }
}
