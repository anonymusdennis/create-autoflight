package com.simibubi.create.content.equipment.symmetryWand;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record SymmetryEffectPacket(BlockPos mirror, List<BlockPos> positions) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, SymmetryEffectPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      SymmetryEffectPacket::mirror,
      CatnipStreamCodecBuilders.list(BlockPos.STREAM_CODEC),
      SymmetryEffectPacket::positions,
      SymmetryEffectPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.SYMMETRY_EFFECT;
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (!(player.position().distanceTo(Vec3.atLowerCornerOf(this.mirror)) > 100.0)) {
         for (BlockPos to : this.positions) {
            SymmetryHandler.drawEffect(this.mirror, to);
         }
      }
   }
}
