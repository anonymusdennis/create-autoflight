package com.simibubi.create.content.contraptions;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ContraptionColliderLockPacket(int contraption, double offset, int sender) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, ContraptionColliderLockPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ContraptionColliderLockPacket::contraption,
      ByteBufCodecs.DOUBLE,
      ContraptionColliderLockPacket::offset,
      ByteBufCodecs.VAR_INT,
      ContraptionColliderLockPacket::sender,
      ContraptionColliderLockPacket::new
   );

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      ContraptionCollider.lockPacketReceived(this.contraption, this.sender, this.offset);
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONTRAPTION_COLLIDER_LOCK;
   }

   public static record ContraptionColliderLockPacketRequest(int contraption, double offset) implements ServerboundPacketPayload {
      public static final StreamCodec<ByteBuf, ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest::contraption,
         ByteBufCodecs.DOUBLE,
         ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest::offset,
         ContraptionColliderLockPacket.ContraptionColliderLockPacketRequest::new
      );

      public void handle(ServerPlayer player) {
         CatnipServices.NETWORK.sendToClientsTrackingEntity(player, new ContraptionColliderLockPacket(this.contraption, this.offset, player.getId()));
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.CONTRAPTION_COLLIDER_LOCK_REQUEST;
      }
   }
}
