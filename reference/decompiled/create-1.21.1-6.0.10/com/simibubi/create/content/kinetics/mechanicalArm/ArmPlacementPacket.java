package com.simibubi.create.content.kinetics.mechanicalArm;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ArmPlacementPacket(ListTag tag, BlockPos pos) implements ServerboundPacketPayload {
   public static final StreamCodec<FriendlyByteBuf, ArmPlacementPacket> STREAM_CODEC = StreamCodec.composite(
      CatnipStreamCodecs.COMPOUND_LIST_TAG, ArmPlacementPacket::tag, BlockPos.STREAM_CODEC, ArmPlacementPacket::pos, ArmPlacementPacket::new
   );

   public ArmPlacementPacket(Collection<ArmInteractionPoint> points, BlockPos pos) {
      this(new ListTag(), pos);

      for (ArmInteractionPoint point : points) {
         this.tag.add(point.serialize(pos));
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.PLACE_ARM;
   }

   public void handle(ServerPlayer player) {
      Level world = player.level();
      if (world.isLoaded(this.pos)) {
         if (world.getBlockEntity(this.pos) instanceof ArmBlockEntity arm) {
            arm.interactionPointTag = this.tag;
         }
      }
   }

   public static record ClientBoundRequest(BlockPos pos) implements ClientboundPacketPayload {
      public static final StreamCodec<ByteBuf, ArmPlacementPacket.ClientBoundRequest> STREAM_CODEC = BlockPos.STREAM_CODEC
         .map(ArmPlacementPacket.ClientBoundRequest::new, ArmPlacementPacket.ClientBoundRequest::pos);

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.S_PLACE_ARM;
      }

      @OnlyIn(Dist.CLIENT)
      public void handle(LocalPlayer player) {
         ArmInteractionPointHandler.flushSettings(this.pos);
      }
   }
}
