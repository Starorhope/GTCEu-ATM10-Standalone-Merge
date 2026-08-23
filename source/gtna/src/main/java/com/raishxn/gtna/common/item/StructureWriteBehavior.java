package com.raishxn.gtna.common.item;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.api.mui.IItemUIHolder;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import com.google.common.base.Joiner;
import com.mojang.blaze3d.platform.InputConstants;
import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.api.pattern.DebugBlockPattern;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PlayerInventoryGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.TextWidget;

public class StructureWriteBehavior implements IItemUIHolder {

    private static final String DATA_KEY = "structure_writer";

    public static final StructureWriteBehavior INSTANCE = new StructureWriteBehavior();

    protected StructureWriteBehavior() {}

    @Override
    public ModularPanel<?> buildUI(PlayerInventoryGuiData<?> data, PanelSyncManager syncManager, UISettings settings) {
        syncManager.registerServerSyncedAction("structure_writer_export", packet -> exportLog(data));
        syncManager.registerServerSyncedAction("structure_writer_rotate_x", packet -> changeDir(data, Direction.Axis.X));
        syncManager.registerServerSyncedAction("structure_writer_rotate_y", packet -> changeDir(data, Direction.Axis.Y));

        return new ModularPanel<>("structure_writer")
                .size(176, 120)
                .background(GTGuiTextures.BACKGROUND)
                .child(new TextWidget<>(() -> sizeText(data.getUsedItemStack()))
                        .left(10).top(11).maxWidth(156))
                .child(new TextWidget<>(() -> directionText(data.getUsedItemStack()))
                        .left(10).top(26).maxWidth(156))
                .child(actionButton("structure_writer.action.rotate_x", 9, 68, 77, 20,
                        syncManager, "structure_writer_rotate_x"))
                .child(actionButton("structure_writer.action.rotate_y", 90, 68, 77, 20,
                        syncManager, "structure_writer_rotate_y"))
                .child(actionButton("structure_writer.action.export_log", 9, 91, 158, 20,
                        syncManager, "structure_writer_export"));
    }

    private static ButtonWidget<?> actionButton(String translationKey, int left, int top, int width, int height,
                                                 PanelSyncManager syncManager, String action) {
        return new ButtonWidget<>()
                .left(left).top(top).size(width, height)
                .background(GTGuiTextures.BUTTON)
                .onMousePressed((context, button) -> {
                    if (button != InputConstants.MOUSE_BUTTON_LEFT) return false;
                    syncManager.callSyncedAction(action);
                    return true;
                })
                .child(Text.of(Component.translatable(translationKey)).asWidget().posRel(Alignment.Center));
    }

    private static Component sizeText(ItemStack stack) {
        BlockPos[] positions = getPos(stack);
        int x = 0;
        int y = 0;
        int z = 0;
        if (positions != null) {
            x = 1 + positions[1].getX() - positions[0].getX();
            y = 1 + positions[1].getY() - positions[0].getY();
            z = 1 + positions[1].getZ() - positions[0].getZ();
        }
        return Component.translatable("structure_writer.structural_scale", x, y, z);
    }

    private static Component directionText(ItemStack stack) {
        RelativeDirection[] directions = DebugBlockPattern.getDir(getDir(stack));
        return Component.translatable("structure_writer.export_order",
                directionName(directions[0]), directionName(directions[1]), directionName(directions[2]));
    }

    private static Component directionName(RelativeDirection direction) {
        String translationKey = switch (direction) {
            case UP -> "structure_writer.direction.up";
            case DOWN -> "structure_writer.direction.down";
            case LEFT -> "structure_writer.direction.left";
            case RIGHT -> "structure_writer.direction.right";
            case FRONT -> "structure_writer.direction.front";
            case BACK -> "structure_writer.direction.back";
        };
        return Component.translatable(translationKey);
    }

    private static void exportLog(PlayerInventoryGuiData<?> data) {
        BlockPos[] positions = getPos(data.getUsedItemStack());
        if (positions == null || data.getPlayer().level().isClientSide()) return;

        DebugBlockPattern blockPattern = new DebugBlockPattern(
                data.getPlayer().level(),
                positions[0].getX(), positions[0].getY(), positions[0].getZ(),
                positions[1].getX(), positions[1].getY(), positions[1].getZ());
        RelativeDirection[] directions = DebugBlockPattern.getDir(getDir(data.getUsedItemStack()));
        blockPattern.changeDir(directions[0], directions[1], directions[2]);

        StringBuilder builder = new StringBuilder(".pattern(definition -> MultiblockPatternBuilder.start()\n");
        for (String[] aisle : blockPattern.pattern) {
            builder.append(".aisle(\"")
                    .append(Joiner.on("\", \"").join(aisle))
                    .append("\")\n");
        }
        builder.append(".where(\"~\", Predicates.controller(Predicates.blocks(definition.get())))\n");
        blockPattern.legend.forEach((block, symbol) -> {
            if (symbol.equals(' ')) return;
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            builder.append(".where(\"").append(symbol)
                    .append("\", Predicates.blocks(Registries.getBlock(\"")
                    .append(blockId).append("\")))\n");
        });
        GTNACORE.LOGGER.info("Generated multiblock pattern:\n{}", builder);
    }

    private static void changeDir(PlayerInventoryGuiData<?> data, Direction.Axis axis) {
        ItemStack stack = data.getUsedItemStack();
        if (getPos(stack) == null || data.getPlayer().level().isClientSide()) return;
        setDir(stack, getDir(stack).getClockWise(axis));
        data.setUsedItemStack(stack);
    }

    public static boolean isItemStructureWriter(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() instanceof ComponentItem item && item.getComponents().contains(INSTANCE);
    }

    public static Direction getDir(ItemStack stack) {
        CompoundTag tag = getData(stack);
        Direction direction = tag.contains("dir") ? Direction.byName(tag.getString("dir")) : null;
        return direction == null ? Direction.WEST : direction;
    }

    public static void setDir(ItemStack stack, Direction direction) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = root.getCompound(DATA_KEY);
            tag.putString("dir", direction.getName());
            root.put(DATA_KEY, tag);
        });
    }

    public static BlockPos[] getPos(ItemStack stack) {
        CompoundTag tag = getData(stack);
        if (!tag.contains("minX")) return null;
        return new BlockPos[] {
                new BlockPos(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ")),
                new BlockPos(tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"))
        };
    }

    public static void addPos(ItemStack stack, BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = root.getCompound(DATA_KEY);
            putMin(tag, "minX", pos.getX());
            putMax(tag, "maxX", pos.getX());
            putMin(tag, "minY", pos.getY());
            putMax(tag, "maxY", pos.getY());
            putMin(tag, "minZ", pos.getZ());
            putMax(tag, "maxZ", pos.getZ());
            root.put(DATA_KEY, tag);
        });
    }

    public static void removePos(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, root -> {
            CompoundTag tag = root.getCompound(DATA_KEY);
            tag.remove("minX");
            tag.remove("maxX");
            tag.remove("minY");
            tag.remove("maxY");
            tag.remove("minZ");
            tag.remove("maxZ");
            root.put(DATA_KEY, tag);
        });
    }

    private static CompoundTag getData(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(DATA_KEY);
    }

    private static void putMin(CompoundTag tag, String key, int value) {
        if (!tag.contains(key) || tag.getInt(key) > value) tag.putInt(key, value);
    }

    private static void putMax(CompoundTag tag, String key, int value) {
        if (!tag.contains(key) || tag.getInt(key) < value) tag.putInt(key, value);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = player.getItemInHand(context.getHand());
        if (player.isShiftKeyDown()) {
            removePos(stack);
        } else {
            addPos(stack, context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level level, Player player,
                                                   InteractionHand usedHand) {
        if (player.isShiftKeyDown()) {
            removePos(stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return IItemUIHolder.super.use(stack, level, player, usedHand);
    }
}
