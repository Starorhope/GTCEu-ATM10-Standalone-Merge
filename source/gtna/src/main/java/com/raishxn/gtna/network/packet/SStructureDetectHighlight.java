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

/** Server-to-client block highlight. */
public class SStructureDetectHighlight implements CustomPacketPayload {

    public static final Type<SStructureDetectHighlight> TYPE = new Type<>(GTNACORE.id("structure_detect_highlight"));
    public static final StreamCodec<FriendlyByteBuf, SStructureDetectHighlight> CODEC =
            StreamCodec.ofMember(SStructureDetectHighlight::encode, SStructureDetectHighlight::new);

    private final BlockPos pos;
    private final ResourceKey<Level> dim;
    private final long time;

    public SStructureDetectHighlight(BlockPos pos, ResourceKey<Level> dim, long time) {
        this.pos = pos;
        this.dim = dim;
        this.time = time;
    }

    private SStructureDetectHighlight(FriendlyByteBuf buf) {
        this(
                BlockPos.of(buf.readVarLong()),
                buf.readResourceKey(Registries.DIMENSION),
                buf.readVarLong());
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(pos.asLong());
        buf.writeResourceKey(dim);
        buf.writeVarLong(time);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> BlockHighlightHandler.highlight(pos, dim, time));
    }

    @Override
    public Type<SStructureDetectHighlight> type() {
        return TYPE;
    }
}
