package com.simibubi.create.content.contraptions.sync;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;

public record ContraptionInteractionPacket(InteractionHand hand, int target, BlockPos localPos, Direction face) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ContraptionInteractionPacket> STREAM_CODEC = StreamCodec.composite(
      CatnipStreamCodecBuilders.nullable(CatnipStreamCodecs.HAND),
      ContraptionInteractionPacket::hand,
      ByteBufCodecs.INT,
      ContraptionInteractionPacket::target,
      BlockPos.STREAM_CODEC,
      ContraptionInteractionPacket::localPos,
      Direction.STREAM_CODEC,
      ContraptionInteractionPacket::face,
      ContraptionInteractionPacket::new
   );

   public ContraptionInteractionPacket(AbstractContraptionEntity target, InteractionHand hand, BlockPos localPos, Direction side) {
      this(hand, target.getId(), localPos, side);
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONTRAPTION_INTERACT;
   }

   public void handle(ServerPlayer sender) {
      if (sender != null) {
         Entity entityByID = sender.level().getEntity(this.target);
         if (entityByID instanceof AbstractContraptionEntity contraptionEntity) {
            AABB bb = contraptionEntity.getBoundingBox();
            double boundsExtra = Math.max(bb.getXsize(), bb.getYsize());
            double d = sender.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 10.0 + boundsExtra;
            if (!sender.hasLineOfSight(entityByID)) {
               d -= 3.0;
            }

            d *= d;
            if (!(sender.distanceToSqr(entityByID) > d)) {
               if (contraptionEntity.handlePlayerInteraction(sender, this.localPos, this.face, this.hand)) {
                  sender.swing(this.hand, true);
               }
            }
         }
      }
   }
}
