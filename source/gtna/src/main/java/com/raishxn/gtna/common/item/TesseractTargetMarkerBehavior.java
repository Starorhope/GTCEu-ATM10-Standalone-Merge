package com.raishxn.gtna.common.item;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.item.component.IAddInformation;
import com.gregtechceu.gtceu.api.item.component.IInteractionItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.common.machine.tesseract.ITesseractMarkerInteractable;
import com.raishxn.gtna.common.machine.tesseract.TesseractDirectedTarget;
import com.raishxn.gtna.utils.GTNATooltips;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EventBusSubscriber(modid = GTNACORE.MOD_ID)
public class TesseractTargetMarkerBehavior implements IInteractionItem, IAddInformation {

    public static final TesseractTargetMarkerBehavior INSTANCE = new TesseractTargetMarkerBehavior();
    private static final String TARGETS_KEY = "gtnaTesseractTargets";

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        if (!isTesseractTargetMarker(itemStack)) {
            return IInteractionItem.super.onItemUseFirst(itemStack, context);
        }
        Player player = context.getPlayer();
        if (player == null || context.getLevel().isClientSide) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return removeTarget(itemStack, context.getLevel().dimension(), context.getClickedPos(),
                    context.getClickedFace()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        addTarget(itemStack, context.getLevel().dimension(), context.getClickedPos(), context.getClickedFace());
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack stack, Level level, Player player, InteractionHand usedHand) {
        if (player.isShiftKeyDown() && !level.isClientSide) {
            double range = player.blockInteractionRange();
            if (player.pick(range, 0.0F, false).getType() == HitResult.Type.MISS) {
                clearTargets(stack);
                return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
            }
        }
        return IInteractionItem.super.use(stack, level, player, usedHand);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack stack = event.getEntity().getMainHandItem();
        if (!isTesseractTargetMarker(stack) || !event.getEntity().isShiftKeyDown()) {
            return;
        }
        event.setCanceled(true);
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (MetaMachine.getMachine(event.getLevel(), event.getPos()) instanceof ITesseractMarkerInteractable interactable &&
                interactable.onMarkerInteract(event.getEntity(), getAllTargets(stack))) {
            event.setCanceled(true);
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag tooltipFlag) {
        tooltip.add(GTNATooltips.desc("item.gtna.tesseract_target_marker.tooltip.1"));
        tooltip.add(GTNATooltips.structure("item.gtna.tesseract_target_marker.tooltip.2"));
        tooltip.add(GTNATooltips.info("item.gtna.tesseract_target_marker.tooltip.3", getAllTargets(itemStack).size()));
    }

    public static boolean isTesseractTargetMarker(ItemStack stack) {
        if (stack.getItem() instanceof ComponentItem item) {
            return item.getComponents().stream().anyMatch(component -> component instanceof TesseractTargetMarkerBehavior);
        }
        return false;
    }

    public static List<TesseractDirectedTarget> getAllTargets(ItemStack stack) {
        List<PatternFaceUnindexed> storedTargets = getTargetsFromNbt(stack);
        List<TesseractDirectedTarget> result = new ArrayList<>(storedTargets.size());
        for (int i = 0; i < storedTargets.size(); i++) {
            PatternFaceUnindexed target = storedTargets.get(i);
            result.add(new TesseractDirectedTarget(target.pos(), target.face(), i + 1));
        }
        return result;
    }

    private static List<PatternFaceUnindexed> getTargetsFromNbt(ItemStack stack) {
        List<PatternFaceUnindexed> result = new ArrayList<>();
        ListTag list = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getList(TARGETS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(entry.getString("dim")));
            BlockPos pos = BlockPos.of(entry.getLong("pos"));
            Direction face = Direction.from3DDataValue(entry.getInt("face"));
            result.add(new PatternFaceUnindexed(GlobalPos.of(dimension, pos), face));
        }
        return result;
    }

    private static void saveTargets(ItemStack stack, List<PatternFaceUnindexed> targets) {
        ListTag list = new ListTag();
        for (PatternFaceUnindexed target : targets) {
            CompoundTag entry = new CompoundTag();
            entry.putString("dim", target.pos().dimension().location().toString());
            entry.putLong("pos", target.pos().pos().asLong());
            entry.putInt("face", target.face().get3DDataValue());
            list.add(entry);
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put(TARGETS_KEY, list));
    }

    private static void addTarget(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos, Direction face) {
        PatternFaceUnindexed newTarget = new PatternFaceUnindexed(GlobalPos.of(dimension, pos), face);
        List<PatternFaceUnindexed> targets = getTargetsFromNbt(stack);
        if (!targets.contains(newTarget)) {
            targets.add(newTarget);
            saveTargets(stack, targets);
        }
    }

    private static boolean removeTarget(ItemStack stack, ResourceKey<Level> dimension, BlockPos pos, Direction face) {
        PatternFaceUnindexed target = new PatternFaceUnindexed(GlobalPos.of(dimension, pos), face);
        List<PatternFaceUnindexed> targets = getTargetsFromNbt(stack);
        boolean removed = targets.remove(target);
        if (removed) {
            saveTargets(stack, targets);
        }
        return removed;
    }

    private static void clearTargets(ItemStack stack) {
        saveTargets(stack, new ArrayList<>());
    }

    private record PatternFaceUnindexed(GlobalPos pos, Direction face) {

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof PatternFaceUnindexed other)) {
                return false;
            }
            return Objects.equals(pos, other.pos) && face == other.face;
        }

        @Override
        public int hashCode() {
            return Objects.hash(pos, face);
        }
    }
}
