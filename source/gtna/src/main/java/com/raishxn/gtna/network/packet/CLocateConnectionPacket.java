package com.raishxn.gtna.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.network.GTNANetworkHandler;

/** Client-to-server request for a locate highlight. */
public class CLocateConnectionPacket implements CustomPacketPayload {

    public static final Type<CLocateConnectionPacket> TYPE = new Type<>(GTNACORE.id("locate_connection"));
    public static final StreamCodec<FriendlyByteBuf, CLocateConnectionPacket> CODEC =
            StreamCodec.ofMember(CLocateConnectionPacket::encode, CLocateConnectionPacket::new);

    private final int x;
    private final int y;
    private final int z;
    private final String dimension;

    public CLocateConnectionPacket(int x, int y, int z, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    private CLocateConnectionPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readUtf(256));
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(x);
        buf.writeVarInt(y);
        buf.writeVarInt(z);
        buf.writeUtf(dimension, 256);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation dimensionId = ResourceLocation.tryParse(dimension);
            if (dimensionId == null) {
                return;
            }

            BlockPos pos = new BlockPos(x, y, z);
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
            long expiryTime = System.currentTimeMillis() + 15_000L;
            GTNANetworkHandler.sendToPlayer(new SStructureDetectHighlight(pos, dimKey, expiryTime), player);
        });
    }

    @Override
    public Type<CLocateConnectionPacket> type() {
        return TYPE;
    }
}
