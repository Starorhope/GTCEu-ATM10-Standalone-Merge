package yuuki1293.pccard.api;

import appeng.api.upgrades.Upgrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import yuuki1293.pccard.PCCard;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Public hook for add-ons whose pattern providers use a custom host item. */
public final class PatternProviderRegistration {
    private static final Set<Item> REGISTERED_ITEMS =
        Collections.newSetFromMap(new IdentityHashMap<>());

    private PatternProviderRegistration() {}

    public static void register(ItemLike provider) {
        if (provider != null) {
            register(provider, provider.asItem().getDescriptionId());
        }
    }

    public static synchronized void register(ItemLike provider, String groupTranslationKey) {
        if (provider == null) {
            return;
        }

        var item = provider.asItem();
        if (item == null || !REGISTERED_ITEMS.add(item)) {
            return;
        }

        Upgrades.add(PCCard.PROGRAMMED_CIRCUIT_CARD_ITEM.get(), item, 1, groupTranslationKey);
    }
}
