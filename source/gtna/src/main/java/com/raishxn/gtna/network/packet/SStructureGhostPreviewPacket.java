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

import java.util.ArrayList;
import java.util.List;

public class SStructureGhostPreviewPacket implements CustomPacketPayload {

    public static final Type<SStructureGhostPreviewPacket> TYPE = new Type<>(GTNACORE.id("structure_ghost_preview"));
    public static final StreamCodec<FriendlyByteBuf, SStructureGhostPreviewPacket> CODEC =
            StreamCodec.ofMember(SStructureGhostPreviewPacket::encode, SStructureGhostPreviewPacket::new);

    private final ResourceKey<Level> dim;
    private final List<BlockPos> positions;
    private final int color;
    private final long expiryTime;
    private final boolean clear;

    public SStructureGhostPreviewPacket(ResourceKey<Level> dim, List<BlockPos> positions, int color, long expiryTime,
                                        boolean clear) {
        this.dim = dim;
        this.positions = List.copyOf(positions);
        this.color = color;
        this.expiryTime = expiryTime;
        this.clear = clear;
    }

    private SStructureGhostPreviewPacket(FriendlyByteBuf buf) {
        this.dim = buf.readResourceKey(Registries.DIMENSION);
        int size = buf.readVarInt();
        if (size < 0 || size > 65_536) {
            throw new IllegalArgumentException("Invalid ghost preview position count: " + size);
        }
        List<BlockPos> decodedPositions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            decodedPositions.add(BlockPos.of(buf.readVarLong()));
        }
        this.positions = List.copyOf(decodedPositions);
        this.color = buf.readInt();
        this.expiryTime = buf.readVarLong();
        this.clear = buf.readBoolean();
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeResourceKey(dim);
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeVarLong(pos.asLong());
        }
        buf.writeInt(color);
        buf.writeVarLong(expiryTime);
        buf.writeBoolean(clear);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (clear) {
                BlockHighlightHandler.clearStructureGhost(dim);
            } else {
                BlockHighlightHandler.highlightStructureGhost(dim, positions, color, expiryTime);
            }
        });
    }

    @Override
    public Type<SStructureGhostPreviewPacket> type() {
        return TYPE;
    }
}
