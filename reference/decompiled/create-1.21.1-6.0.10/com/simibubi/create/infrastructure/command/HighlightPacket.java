package com.simibubi.create.infrastructure.command;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSpecialTextures;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record HighlightPacket(BlockPos pos) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, HighlightPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(HighlightPacket::new, p -> p.pos);

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (player.clientLevel.isLoaded(this.pos)) {
         Outliner.getInstance()
            .showAABB("highlightCommand", Shapes.block().bounds().move(this.pos), 200)
            .lineWidth(0.03125F)
            .colored(15658734)
            .withFaceTexture(AllSpecialTextures.SELECTION);
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.BLOCK_HIGHLIGHT;
   }
}
