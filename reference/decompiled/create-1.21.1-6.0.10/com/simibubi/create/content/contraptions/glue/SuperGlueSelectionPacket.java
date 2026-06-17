package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import io.netty.buffer.ByteBuf;
import java.util.Set;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public record SuperGlueSelectionPacket(BlockPos from, BlockPos to) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, SuperGlueSelectionPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC, SuperGlueSelectionPacket::from, BlockPos.STREAM_CODEC, SuperGlueSelectionPacket::to, SuperGlueSelectionPacket::new
   );

   public void handle(ServerPlayer player) {
      if (player.canInteractWithBlock(this.to, 2.0)) {
         if (this.to.closerThan(this.from, 25.0)) {
            Set<BlockPos> group = SuperGlueSelectionHelper.searchGlueGroup(player.level(), this.from, this.to, false);
            if (group != null) {
               if (group.contains(this.to)) {
                  if (SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, true)) {
                     AABB bb = SuperGlueEntity.span(this.from, this.to);
                     SuperGlueSelectionHelper.collectGlueFromInventory(player, 1, false);
                     SuperGlueEntity entity = new SuperGlueEntity(player.level(), bb);
                     player.level().addFreshEntity(entity);
                     entity.spawnParticles();
                     AllAdvancements.SUPER_GLUE.awardTo(player);
                  }
               }
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.GLUE_IN_AREA;
   }
}
