package com.raishxn.gtna.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.config.ConfigHolder;

@EventBusSubscriber(modid = GTNACORE.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player == Minecraft.getInstance().player) {
            if (ConfigHolder.INSTANCE.client.disableFlyInertia && player.getAbilities().flying) {
                if (player.zza == 0 && player.xxa == 0) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(0.0, 1.0, 0.0));
                }
            }
        }
    }
}
