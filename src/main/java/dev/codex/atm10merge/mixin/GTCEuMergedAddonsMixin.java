package dev.codex.atm10merge.mixin;

import com.gregtechceu.gtceu.GTCEu;
import com.raishxn.gtna.GTNACORE;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yuuki1293.pccard.PCCard;

/** Boots both embedded feature modules from the one real gtceu ModContainer. */
@Mixin(value = GTCEu.class, remap = false)
public abstract class GTCEuMergedAddonsMixin {

    @Inject(
        method = "<init>(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/javafmlmod/FMLModContainer;)V",
        at = @At("TAIL"),
        require = 1)
    private void codex$bootstrapEmbeddedAddons(IEventBus modBus, FMLModContainer container, CallbackInfo ci) {
        GTNACORE.bootstrapMerged(modBus, container);
        PCCard.bootstrapMerged(modBus, container);
    }
}
