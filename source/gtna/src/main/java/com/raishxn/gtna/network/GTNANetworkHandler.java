package com.raishxn.gtna.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.raishxn.gtna.network.packet.CLocateConnectionPacket;
import com.raishxn.gtna.network.packet.SRegionHighlightPacket;
import com.raishxn.gtna.network.packet.SStructureDetectHighlight;
import com.raishxn.gtna.network.packet.SStructureGhostPreviewPacket;

public final class GTNANetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    /** Compatibility alias retained for existing addon call sites. */
    public static final GTNANetworkHandler INSTANCE = new GTNANetworkHandler();

    private GTNANetworkHandler() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                SStructureDetectHighlight.TYPE,
                SStructureDetectHighlight.CODEC,
                SStructureDetectHighlight::handle);
        registrar.playToClient(
                SRegionHighlightPacket.TYPE,
                SRegionHighlightPacket.CODEC,
                SRegionHighlightPacket::handle);
        registrar.playToClient(
                SStructureGhostPreviewPacket.TYPE,
                SStructureGhostPreviewPacket.CODEC,
                SStructureGhostPreviewPacket::handle);
        registrar.playToServer(
                CLocateConnectionPacket.TYPE,
                CLocateConnectionPacket.CODEC,
                CLocateConnectionPacket::handle);
    }

    public void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    public void sendTo(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToPlayer(CustomPacketPayload payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
