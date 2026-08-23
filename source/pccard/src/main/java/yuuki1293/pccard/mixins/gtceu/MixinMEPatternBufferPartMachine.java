package yuuki1293.pccard.mixins.gtceu;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.value.sync.IntSyncValue;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widgets.CycleButtonWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.ItemSlot;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.trait.notifiable.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.MEPatternBufferPartMachine;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import yuuki1293.pccard.PCCard;
import yuuki1293.pccard.TagUtils;
import yuuki1293.pccard.impl.PatternBufferBlockingMode;
import yuuki1293.pccard.impl.PatternBufferBlockingPolicy;
import yuuki1293.pccard.impl.PatternBufferCardInventory;
import yuuki1293.pccard.impl.PatternProviderLogicImpl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(value = MEPatternBufferPartMachine.class, remap = false)
public abstract class MixinMEPatternBufferPartMachine extends MEBusPartMachine {

    @Unique
    private static final int PCCARD$CIRCUIT_IDLE = -1;
    @Unique
    private static final int PCCARD$CIRCUIT_UNKNOWN = -2;

    @Shadow
    @Final
    protected MEPatternBufferPartMachine.InternalSlot[] internalInventory;

    @Unique
    @SaveField(nbtKey = "pccardCardInventory")
    private PatternBufferCardInventory pCCard$cardInventory;

    @Unique
    @SaveField(nbtKey = "pccardBlockingEnabled")
    private boolean pCCard$blockingEnabled;

    @Unique
    @SaveField(nbtKey = "pccardBlockingMode")
    private PatternBufferBlockingMode pCCard$blockingMode;

    @Unique
    @SaveField(nbtKey = "pccardActiveCircuit")
    private int pCCard$activeCircuit;

    @Unique
    private boolean pCCard$removing;
    @Unique
    private boolean pCCard$lastTransformationEnabled;

    protected MixinMEPatternBufferPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.IN, new NotifiableItemStackHandler(9, IO.IN, IO.NONE));
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void pCCard$initialize(BlockEntityCreationInfo info, CallbackInfo ci) {
        pCCard$cardInventory = new PatternBufferCardInventory(
            this::pCCard$canChangePatternBufferCard,
            this::pCCard$onPatternBufferCardChanged);
        pCCard$blockingMode = PatternBufferBlockingMode.NORMAL;
        pCCard$activeCircuit = PCCARD$CIRCUIT_IDLE;

        var previousListener = circuitSlot.storage.getOnContentsChanged();
        circuitSlot.storage.setOnContentsChanged(() -> {
            previousListener.run();
            pCCard$enforceCircuitLease();
        });
    }

    @ModifyArg(
        method = "onPatternChange",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"),
        index = 0)
    private ItemStack pCCard$updateChangedPattern(ItemStack stack) {
        return pCCard$transformPattern(stack);
    }

    @ModifyArg(
        method = "onLoad",
        at = @At(
            value = "INVOKE",
            target = "Lappeng/api/crafting/PatternDetailsHelper;decodePattern(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;)Lappeng/api/crafting/IPatternDetails;"),
        index = 0)
    private ItemStack pCCard$updateLoadedPattern(ItemStack stack) {
        return pCCard$transformPattern(stack);
    }

    @Inject(method = "onLoad", at = @At("TAIL"))
    private void pCCard$onLoad(CallbackInfo ci) {
        if (isRemote()) {
            return;
        }

        if (pCCard$blockingMode == null) {
            pCCard$blockingMode = PatternBufferBlockingMode.NORMAL;
            markAsChanged();
        } else if (!pCCard$isExpandedAEInstalled() &&
            pCCard$blockingMode != PatternBufferBlockingMode.NORMAL) {
            pCCard$blockingMode = PatternBufferBlockingMode.NORMAL;
            markAsChanged();
        }

        pCCard$cardInventory.synchronizeCardState();
        pCCard$lastTransformationEnabled = pCCard$canTransformPatterns();
        pCCard$reconcileCircuitLease();
        pCCard$enforceCircuitLease();
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void pCCard$onUpdate(CallbackInfo ci) {
        if (isRemote()) {
            return;
        }

        var transformationEnabled = pCCard$canTransformPatterns();
        if (transformationEnabled != pCCard$lastTransformationEnabled) {
            pCCard$lastTransformationEnabled = transformationEnabled;
            pCCard$refreshPatterns();
        }

        pCCard$reconcileCircuitLease();
        pCCard$enforceCircuitLease();
    }

    @Inject(method = "onSlotChanged", at = @At("TAIL"))
    private void pCCard$onBufferedContentsChanged(CallbackInfo ci) {
        if (!isRemote()) {
            pCCard$reconcileCircuitLease();
            pCCard$enforceCircuitLease();
        }
    }

    @ModifyArg(
        method = "getPanelBuilder",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/api/machine/mui/MachineUIPanelBuilder;leftConfigurators(Ljava/util/function/Consumer;)Lcom/gregtechceu/gtceu/api/machine/mui/MachineUIPanelBuilder;"),
        index = 0)
    private Consumer<Flow> pCCard$attachPatternBufferControls(Consumer<Flow> original) {
        return flow -> {
            original.accept(flow);
            flow.child(pCCard$createCardSlot());
            flow.child(pCCard$createBlockingModeButton());
        };
    }

    @Inject(
        method = "pushPattern",
        at = @At(
            value = "INVOKE",
            target = "Lcom/gregtechceu/gtceu/integration/ae2/machine/MEPatternBufferPartMachine$InternalSlot;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)V"),
        cancellable = true,
        require = 2,
        expect = 2)
    private void pCCard$preflightPush(IPatternDetails patternDetails, KeyCounter[] inputHolder,
                                      CallbackInfoReturnable<Boolean> cir) {
        var bufferedKeys = pCCard$getBufferedKeys();
        if (pCCard$blockingEnabled && pCCard$isBlocked(patternDetails, bufferedKeys)) {
            cir.setReturnValue(false);
            return;
        }

        var circuitNumber = PatternProviderLogicImpl.getCircuitNumber(patternDetails);
        if (circuitNumber.isEmpty()) {
            return;
        }

        if (!pCCard$canTransformPatterns() || !pCCard$hasIncomingPayload(inputHolder)) {
            cir.setReturnValue(false);
            return;
        }

        var wantedCircuit = circuitNumber.get();
        if (PatternBufferBlockingPolicy.circuitConflict(
            !bufferedKeys.isEmpty(), pCCard$activeCircuit, wantedCircuit)) {
            cir.setReturnValue(false);
            return;
        }

        if (bufferedKeys.isEmpty()) {
            pCCard$setActiveCircuit(wantedCircuit);
        }
        PatternProviderLogicImpl.setPCNumber(circuitSlot, wantedCircuit);
    }

    @Inject(method = "onMachineDestroyed", at = @At("HEAD"))
    private void pCCard$dropCard(CallbackInfo ci) {
        pCCard$removing = true;
        if (!isRemote() && pCCard$cardInventory != null) {
            pCCard$cardInventory.dropInventoryInWorld(getLevel(), getBlockPos());
        }
    }

    @Unique
    private ItemStack pCCard$transformPattern(ItemStack stack) {
        if (!pCCard$canTransformPatterns()) {
            return stack;
        }

        var transformed = stack.copy();
        var circuitNumber = TagUtils.extractCircuitNumber(transformed);
        if (circuitNumber >= 0) {
            transformed.set(PCCard.RECIPE_CIRCUIT.get(), circuitNumber);
        }
        return transformed;
    }

    @Unique
    private ItemSlot pCCard$createCardSlot() {
        return new ItemSlot()
            .size(18)
            .slot(SyncHandlers.itemSlot(pCCard$cardInventory, 0)
                .singletonSlotGroup()
                .accessibility(true, true)
                .filter(stack -> stack.is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get())))
            .background(GTGuiTextures.SLOT)
            .addTooltipLine(Text.lang("gui.pccard.pattern_buffer.card_slot"));
    }

    @Unique
    private CycleButtonWidget pCCard$createBlockingModeButton() {
        int stateCount = pCCard$isExpandedAEInstalled() ? PatternBufferBlockingMode.VALUES.length :
            PatternBufferBlockingMode.BASIC_VALUES.length;
        var modeValue = new IntSyncValue(
            () -> pCCard$getDisplayedBlockingMode().ordinal(),
            state -> pCCard$setDisplayedBlockingMode(PatternBufferBlockingMode.VALUES[
                Math.max(0, Math.min(stateCount - 1, state))]))
            .allowC2S();

        var button = new CycleButtonWidget()
            .size(18)
            .stateCount(stateCount)
            .value(modeValue)
            .background(GTGuiTextures.BUTTON);
        pCCard$configureModeState(button, PatternBufferBlockingMode.DISABLED);
        pCCard$configureModeState(button, PatternBufferBlockingMode.NORMAL);
        if (stateCount > PatternBufferBlockingMode.SMART.ordinal()) {
            pCCard$configureModeState(button, PatternBufferBlockingMode.SMART);
            pCCard$configureModeState(button, PatternBufferBlockingMode.FULL);
        }
        return button;
    }

    @Unique
    private static void pCCard$configureModeState(CycleButtonWidget button, PatternBufferBlockingMode mode) {
        int state = mode.ordinal();
        button.stateOverlay(state, Text.lang(mode.shortLabelKey()));
        button.tooltip(state, tooltip -> tooltip
            .addLine(Text.lang(mode.translationKey()))
            .addLine(Text.lang(mode.descriptionKey())));
    }

    @Unique
    private boolean pCCard$canChangePatternBufferCard() {
        return pCCard$removing || !pCCard$hasBufferedPayload();
    }

    @Unique
    private void pCCard$onPatternBufferCardChanged() {
        if (pCCard$removing || getLevel() == null || isRemote()) {
            return;
        }

        pCCard$lastTransformationEnabled = pCCard$canTransformPatterns();
        if (pCCard$activeCircuit != PCCARD$CIRCUIT_IDLE) {
            pCCard$releaseCircuitLease();
        }
        pCCard$refreshPatterns();
        markAsChanged();
    }

    @Unique
    private boolean pCCard$canTransformPatterns() {
        return pCCard$cardInventory != null && pCCard$cardInventory.hasCard() &&
            circuitSlot != null && circuitSlot.isEnabled() && circuitSlot.isControllerAllowsCircuits() &&
            ConfigHolder.INSTANCE.machines.ghostCircuit;
    }

    @Unique
    private void pCCard$refreshPatterns() {
        if (getLevel() == null || isRemote()) {
            return;
        }

        var patternInventory = ((MEPatternBufferPartMachine) (Object) this).getTerminalPatternInventory();
        for (int slot = 0; slot < patternInventory.size(); slot++) {
            patternInventory.setItemDirect(slot, patternInventory.getStackInSlot(slot).copy());
        }
    }

    @Unique
    private PatternBufferBlockingMode pCCard$getBlockingMode() {
        if (!pCCard$isExpandedAEInstalled()) {
            return PatternBufferBlockingMode.NORMAL;
        }
        return pCCard$blockingMode == null || pCCard$blockingMode == PatternBufferBlockingMode.DISABLED ?
            PatternBufferBlockingMode.NORMAL : pCCard$blockingMode;
    }

    @Unique
    private PatternBufferBlockingMode pCCard$getDisplayedBlockingMode() {
        return pCCard$blockingEnabled ? pCCard$getBlockingMode() : PatternBufferBlockingMode.DISABLED;
    }

    @Unique
    private void pCCard$setDisplayedBlockingMode(PatternBufferBlockingMode mode) {
        if (mode == null || (!pCCard$isExpandedAEInstalled() &&
            mode != PatternBufferBlockingMode.DISABLED && mode != PatternBufferBlockingMode.NORMAL)) {
            return;
        }

        var enabled = mode != PatternBufferBlockingMode.DISABLED;
        var changed = pCCard$blockingEnabled != enabled;
        pCCard$blockingEnabled = enabled;
        if (enabled && pCCard$blockingMode != mode) {
            pCCard$blockingMode = mode;
            changed = true;
        }
        if (changed && getLevel() != null && !isRemote()) {
            markAsChanged();
        }
    }

    @Unique
    private boolean pCCard$isBlocked(IPatternDetails incomingPattern, Set<AEKey> bufferedKeys) {
        var incomingInputs = new HashSet<AEKey>();
        pCCard$addPatternInputs(incomingPattern, incomingInputs);
        return switch (pCCard$getBlockingMode()) {
            case DISABLED -> false;
            case FULL -> PatternBufferBlockingPolicy.full(bufferedKeys);
            case NORMAL -> PatternBufferBlockingPolicy.normal(bufferedKeys, incomingInputs);
            case SMART -> PatternBufferBlockingPolicy.smart(bufferedKeys, incomingInputs);
        };
    }

    @Unique
    private static boolean pCCard$isExpandedAEInstalled() {
        return ModList.get().isLoaded("expandedae");
    }

    @Unique
    private static void pCCard$addPatternInputs(IPatternDetails pattern, Set<AEKey> inputs) {
        for (var input : pattern.getInputs()) {
            for (var candidate : input.getPossibleInputs()) {
                inputs.add(candidate.what().dropSecondary());
            }
        }
    }

    @Unique
    private Set<AEKey> pCCard$getBufferedKeys() {
        var keys = new HashSet<AEKey>();
        for (var internalSlot : internalInventory) {
            for (var stack : internalSlot.getItems()) {
                var key = AEItemKey.of(stack);
                if (key != null && !PatternProviderLogicImpl.isProgrammedCircuit(key)) {
                    keys.add(key.dropSecondary());
                }
            }
            for (var stack : internalSlot.getFluids()) {
                var key = AEFluidKey.of(stack);
                if (key != null) {
                    keys.add(key.dropSecondary());
                }
            }
        }
        return keys;
    }

    @Unique
    private boolean pCCard$hasBufferedPayload() {
        for (var internalSlot : internalInventory) {
            if (!internalSlot.isFluidEmpty()) {
                return true;
            }
            for (var stack : internalSlot.getItems()) {
                var key = AEItemKey.of(stack);
                if (key != null && !PatternProviderLogicImpl.isProgrammedCircuit(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private static boolean pCCard$hasIncomingPayload(KeyCounter[] inputHolder) {
        for (var inputs : inputHolder) {
            for (var input : inputs) {
                if (input.getLongValue() > 0 && !PatternProviderLogicImpl.isProgrammedCircuit(input.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Unique
    private void pCCard$reconcileCircuitLease() {
        var hasPayload = pCCard$hasBufferedPayload();
        if (!hasPayload && pCCard$activeCircuit != PCCARD$CIRCUIT_IDLE) {
            pCCard$releaseCircuitLease();
        } else if (hasPayload && pCCard$activeCircuit == PCCARD$CIRCUIT_IDLE && pCCard$canTransformPatterns()) {
            pCCard$setActiveCircuit(PCCARD$CIRCUIT_UNKNOWN);
        }
    }

    @Unique
    private void pCCard$releaseCircuitLease() {
        var hadLease = pCCard$activeCircuit != PCCARD$CIRCUIT_IDLE;
        pCCard$setActiveCircuit(PCCARD$CIRCUIT_IDLE);
        if (hadLease && circuitSlot != null && !circuitSlot.storage.getStackInSlot(0).isEmpty()) {
            circuitSlot.storage.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Unique
    private void pCCard$enforceCircuitLease() {
        if (getLevel() == null || isRemote() || pCCard$activeCircuit < 0 || !pCCard$hasBufferedPayload()) {
            return;
        }

        var current = circuitSlot.storage.getStackInSlot(0);
        if (current.isEmpty() || circuitSlot.getCurrentCircuit() != pCCard$activeCircuit) {
            PatternProviderLogicImpl.setPCNumber(circuitSlot, pCCard$activeCircuit);
        }
    }

    @Unique
    private void pCCard$setActiveCircuit(int circuit) {
        if (pCCard$activeCircuit == circuit) {
            return;
        }
        pCCard$activeCircuit = circuit;
        if (getLevel() != null && !isRemote()) {
            markAsChanged();
        }
    }
}
