package yuuki1293.pccard.impl;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import yuuki1293.pccard.PCCard;

import java.util.function.BooleanSupplier;

/**
 * The upgrade-card inventory attached to GT's ME pattern buffer.
 *
 * <p>Three backing slots are intentionally retained so worlds made with the 1.20.1 build can be loaded without
 * losing a card that was saved in one of the old hidden slots. Only slot zero is exposed by the 1.21.1 UI.</p>
 */
public final class PatternBufferCardInventory extends CustomItemStackHandler {

    private static final int SLOT_COUNT = 3;

    private final BooleanSupplier canChangeCard;
    private final Runnable onCardChanged;
    private boolean cardInstalled;
    private boolean loading;

    public PatternBufferCardInventory(BooleanSupplier canChangeCard, Runnable onCardChanged) {
        super(SLOT_COUNT);
        this.canChangeCard = canChangeCard;
        this.onCardChanged = onCardChanged;
        setFilter(stack -> stack.is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get()));
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        loading = true;
        try {
            var resized = tag.copy();
            resized.putInt("Size", SLOT_COUNT);
            super.deserializeNBT(provider, resized);
            migrateCardToVisibleSlot();
            cardInstalled = hasCard();
        } finally {
            loading = false;
        }
    }

    public boolean hasCard() {
        for (int slot = 0; slot < getSlots(); slot++) {
            if (getStackInSlot(slot).is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get())) {
                return true;
            }
        }
        return false;
    }

    public void synchronizeCardState() {
        migrateCardToVisibleSlot();
        cardInstalled = hasCard();
    }

    private void migrateCardToVisibleSlot() {
        if (!getStackInSlot(0).isEmpty()) {
            return;
        }

        for (int slot = 1; slot < getSlots(); slot++) {
            var stack = getStackInSlot(slot);
            if (stack.is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get())) {
                super.setStackInSlot(0, stack);
                super.setStackInSlot(slot, ItemStack.EMPTY);
                return;
            }
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return canChangeCard.getAsBoolean() && super.isItemValid(slot, stack) &&
            (getStackInSlot(slot).is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get()) || !hasCard());
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        var current = getStackInSlot(slot);
        var changesCard = !(current.isEmpty() && stack.isEmpty()) &&
            !ItemStack.isSameItemSameComponents(current, stack);
        if (changesCard && !canChangeCard.getAsBoolean()) {
            return;
        }
        if (!stack.isEmpty() && !current.is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get()) &&
            stack.is(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get()) && hasCard()) {
            return;
        }
        super.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!canChangeCard.getAsBoolean()) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!canChangeCard.getAsBoolean()) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public void onContentsChanged(int slot) {
        super.onContentsChanged(slot);
        if (loading) {
            return;
        }
        var cardInstalledNow = hasCard();
        if (cardInstalled != cardInstalledNow) {
            cardInstalled = cardInstalledNow;
            onCardChanged.run();
        }
    }
}
