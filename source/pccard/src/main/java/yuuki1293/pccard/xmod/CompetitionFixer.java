package yuuki1293.pccard.xmod;

import com.google.common.base.Suppliers;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import yuuki1293.pccard.PCCard;

import java.util.function.Supplier;

public class CompetitionFixer {
    public static final String appFluxUpgradeField = "af_upgrades";
    public static final String appFluxTickerField = "af_ticker";

    public static Supplier<Boolean> existAppflux = Suppliers.memoize(CompetitionFixer::hasPatternProviderUpgrade);

    private static boolean hasPatternProviderUpgrade() {
        ModList modList = ModList.get();

        return modList.getMods().stream()
            .map(IModInfo::getModId)
            .anyMatch(id -> id.equals("appflux")); // detect Applied Flux
    }

    /**
     * Notify AppliedFlux's injected energy capability ticker after replacing its upgrade inventory.
     * The field is added to the provider by AppliedFlux/AdvancedAE mixins, so it cannot be shadowed
     * safely from PCC's optional compatibility mixin.
     */
    public static void updateAppFluxTicker(Class<?> providerClass, Object provider) {
        try {
            var tickerField = providerClass.getDeclaredField(appFluxTickerField);
            tickerField.setAccessible(true);
            var ticker = tickerField.get(provider);
            if (ticker == null) {
                PCCard.LOGGER.error("AppliedFlux ticker is not initialized on {}", providerClass.getName());
                return;
            }
            ticker.getClass().getMethod("updateSleep").invoke(ticker);
        } catch (ReflectiveOperationException | SecurityException e) {
            PCCard.LOGGER.error("Can't update AppliedFlux ticker on {}", providerClass.getName(), e);
        }
    }

    /**
     * Read the upgrade inventory injected by AppliedFlux. This avoids relying on which implementation
     * of the shared getUpgrades signature wins when several mixins target the same provider class.
     */
    public static IUpgradeInventory getAppFluxUpgrades(Class<?> providerClass, Object provider) {
        try {
            var upgradeField = providerClass.getDeclaredField(appFluxUpgradeField);
            upgradeField.setAccessible(true);
            var upgrades = upgradeField.get(provider);
            if (upgrades instanceof IUpgradeInventory inventory) {
                return inventory;
            }
            PCCard.LOGGER.error("AppliedFlux upgrade inventory is not initialized on {}", providerClass.getName());
        } catch (ReflectiveOperationException | SecurityException e) {
            PCCard.LOGGER.error("Can't read AppliedFlux upgrades on {}", providerClass.getName(), e);
        }
        return UpgradeInventories.empty();
    }
}
