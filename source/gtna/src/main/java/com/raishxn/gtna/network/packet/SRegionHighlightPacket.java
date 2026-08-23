package com.raishxn.gtna.network.packet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.raishxn.gtna.GTNACORE;
import com.raishxn.gtna.client.renderer.BlockHighlightHandler;

public class SRegionHighlightPacket implements CustomPacketPayload {

    public static final Type<SRegionHighlightPacket> TYPE = new Type<>(GTNACORE.id("region_highlight"));
    public static final StreamCodec<FriendlyByteBuf, SRegionHighlightPacket> CODEC =
            StreamCodec.ofMember(SRegionHighlightPacket::encode, SRegionHighlightPacket::new);

    private final BlockPos start;
    private final BlockPos end;
    private final ResourceKey<Level> dim;
    private final int color;
    private final long expiryTime;
    private final boolean clear;

    public SRegionHighlightPacket(BlockPos start, BlockPos end, ResourceKey<Level> dim, int color, long expiryTime,
                                  boolean clear) {
        this.start = start;
        this.end = end;
        this.dim = dim;
        this.color = color;
        this.expiryTime = expiryTime;
        this.clear = clear;
    }

    private SRegionHighlightPacket(FriendlyByteBuf buf) {
        this(
                BlockPos.of(buf.readVarLong()),
                BlockPos.of(buf.readVarLong()),
                buf.readResourceKey(Registries.DIMENSION),
                buf.readInt(),
                buf.readVarLong(),
                buf.readBoolean());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(start.asLong());
        buf.writeVarLong(end.asLong());
        buf.writeResourceKey(dim);
        buf.writeInt(color);
        buf.writeVarLong(expiryTime);
        buf.writeBoolean(clear);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (clear) {
                BlockHighlightHandler.stopRegionHighlight(start, end, dim);
            } else {
                BlockHighlightHandler.highlightRegion(start, end, dim, color, expiryTime);
            }
        });
    }

    @Override
    public Type<SRegionHighlightPacket> type() {
        return TYPE;
    }
}
