package com.raishxn.gtna.common.item.terminal;

import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.raishxn.gtna.common.item.terminal.ui.NexusTerminalUIFactory;
import com.raishxn.gtna.integration.ae2.NexusAE2Link;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.BooleanSyncValue;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.CycleButtonWidget;
import brachy.modularui.widgets.ToggleButton;

import java.util.List;

public class NexusTerminalBehavior implements IItemUIHolder, IAddInformation {

    public static final NexusTerminalBehavior INSTANCE = new NexusTerminalBehavior();

    protected NexusTerminalBehavior() {}

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> data, PanelSyncManager syncManager, UISettings settings) {
        ItemStack held = data.getUsedItemStack();
        NexusTerminalUIFactory.AutoBuildSetting setting = NexusTerminalUIFactory.AutoBuildSetting.getSetting(held);

        IntSyncValue repetitions = new IntSyncValue(setting::getRepetitions, value -> {
            setting.setRepetitions(Math.max(0, Math.min(64, value)));
            setting.save(held);
            data.setUsedItemStack(held);
        }).allowC2S();
        BooleanSyncValue replace = boolValue(setting::isReplaceMode, setting::setReplaceMode, setting, held, data);
        BooleanSyncValue noHatch = boolValue(setting::isNoHatchMode, setting::setNoHatchMode, setting, held, data);
        BooleanSyncValue useAE = boolValue(setting::isUseAE, setting::setUseAE, setting, held, data);
        BooleanSyncValue mirror = boolValue(setting::isMirrorBuild, setting::setMirrorBuild, setting, held, data);

        return new ModularPanel<>("nexus_structure_terminal")
                .size(176, 116)
                .background(GTGuiTextures.BACKGROUND)
                .child(Text.of(Component.translatable("item.gtna.nexus_structure_terminal"))
                        .asWidget().left(8).top(7))
                .child(Text.dynamic(() -> Component.translatable("gtna.terminal.nexus.repetitions",
                        repetitions.getIntValue())).asWidget().left(32).top(27))
                .child(new CycleButtonWidget().value(repetitions).stateCount(65).left(8).top(23).size(18))
                .child(toggle(replace, "gtna.terminal.nexus.replace_mode", 47))
                .child(toggle(noHatch, "gtna.terminal.nexus.no_hatch", 65))
                .child(toggle(useAE, "gtna.terminal.nexus.use_ae", 83))
                .child(toggle(mirror, "gtna.terminal.nexus.mirror", 101));
    }

    private static BooleanSyncValue boolValue(java.util.function.BooleanSupplier getter,
                                              it.unimi.dsi.fastutil.booleans.BooleanConsumer setter,
                                              NexusTerminalUIFactory.AutoBuildSetting setting, ItemStack stack,
                                              PlayerInventoryGuiData<?> data) {
        return new BooleanSyncValue(getter, value -> {
            setter.accept(value);
            setting.save(stack);
            data.setUsedItemStack(stack);
        }).allowC2S();
    }

    private static ToggleButton toggle(BooleanSyncValue value, String translationKey, int top) {
        return new ToggleButton().value(value).left(8).top(top).size(18)
                .child(false, Text.of(Component.translatable(translationKey)).asWidget().left(24).top(4))
                .child(true, Text.of(Component.translatable(translationKey)).asWidget().left(24).top(4));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        ItemStack terminalStack = player.getItemInHand(context.getHand());

        // ── AE2 Wireless Access Point linking (any click on WAP) ──────────────
        if (NexusAE2Link.isAE2Available()) {
            BlockEntity be = level.getBlockEntity(blockPos);
            if (be != null && NexusAE2Link.isWirelessAccessPoint(be)) {
                if (!level.isClientSide()) {
                    NexusAE2Link.linkToAccessPoint(terminalStack, level, blockPos);
                    player.displayClientMessage(
                            Component.translatable("gtna.terminal.nexus.ae2.linked"),
                            true);
                    level.playSound(null, blockPos,
                            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS,
                            1.0f, 1.5f);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
        }

        // ── Shift+Click on controller: auto-build / replace ───────────────────
        if (player.isShiftKeyDown()) {
            if (MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller) {
                if (!controller.isFormed()) {
                    if (!level.isClientSide()) {
                        NexusAutoBuilder.autoBuild(player, controller, terminalStack);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                } else if (controller instanceof WorkableMultiblockMachine workableMultiblockMachine &&
                        NexusTerminalUIFactory.AutoBuildSetting.getSetting(terminalStack).isReplaceMode()) {
                            if (!level.isClientSide()) {
                                NexusAutoBuilder.autoBuild(player, controller, terminalStack);
                                workableMultiblockMachine.onPartUnload();
                            }
                            return InteractionResult.sidedSuccess(level.isClientSide);
                        }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        // ── Usage instructions ────────────────────────────────────────────────
        tooltipComponents.add(Component.translatable("item.gtna.nexus_structure_terminal.tooltip.use")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("item.gtna.nexus_structure_terminal.tooltip.shift_use")
                .withStyle(ChatFormatting.GRAY));

        // ── Replace mode indicator ────────────────────────────────────────────
        NexusTerminalUIFactory.AutoBuildSetting settings = NexusTerminalUIFactory.AutoBuildSetting.getSetting(stack);
        if (settings.isReplaceMode()) {
            tooltipComponents.add(Component.translatable(
                    "item.gtna.nexus_structure_terminal.tooltip.replace_mode_active")
                    .withStyle(ChatFormatting.GOLD));
        }

        // ── AE2 Network status ────────────────────────────────────────────────
        if (NexusAE2Link.isAE2Available()) {
            tooltipComponents.add(Component.literal("")); // spacer

            if (NexusAE2Link.isLinked(stack)) {
                // Show linked status
                GlobalPos linkedPos = NexusAE2Link.getLinkedPosition(stack);
                if (linkedPos != null) {
                    tooltipComponents.add(Component.translatable(
                            "gtna.terminal.nexus.ae2.tooltip.linked",
                            linkedPos.pos().getX(),
                            linkedPos.pos().getY(),
                            linkedPos.pos().getZ())
                            .withStyle(ChatFormatting.GREEN));
                }

                // Range check (client-side only)
                Level level = context.level();
                if (level != null && level.isClientSide) {
                    try {
                        appendAE2RangeTooltip(stack, level, tooltipComponents);
                    } catch (Exception ignored) {
                        // Safety catch for client-only code
                    }
                }
            } else {
                tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.not_linked")
                        .withStyle(ChatFormatting.RED));
                tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.how_to_link")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }

    /**
     * Append range status tooltip. Separated to safely reference client-side player.
     */
    private void appendAE2RangeTooltip(ItemStack stack, Level level, List<Component> tooltipComponents) {
        Player localPlayer = net.minecraft.client.Minecraft.getInstance().player;
        if (localPlayer == null) return;

        if (NexusAE2Link.isInRange(stack, level, localPlayer)) {
            tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.in_range")
                    .withStyle(ChatFormatting.GREEN));
        } else {
            tooltipComponents.add(Component.translatable("gtna.terminal.nexus.ae2.tooltip.out_of_range")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
