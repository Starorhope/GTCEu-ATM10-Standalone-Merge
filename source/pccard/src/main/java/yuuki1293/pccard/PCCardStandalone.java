package yuuki1293.pccard;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point used only by the standalone PCCard artifact.
 *
 * <p>The single-logical-mod GregTech bundle omits this class and starts the feature through
 * {@link PCCard#bootstrapMerged(IEventBus, ModContainer)} instead.</p>
 */
@Mod(PCCard.MODID)
public final class PCCardStandalone {

    public PCCardStandalone(IEventBus modEventBus, ModContainer modContainer) {
        new PCCard(modEventBus, modContainer);
    }
}
