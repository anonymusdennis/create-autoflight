package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record EjectorElytraPacket(BlockPos pos) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, EjectorElytraPacket> STREAM_CODEC = BlockPos.STREAM_CODEC.map(EjectorElytraPacket::new, EjectorElytraPacket::pos);

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.EJECTOR_ELYTRA;
   }

   public void handle(ServerPlayer player) {
      Level world = player.level();
      if (world.isLoaded(this.pos)) {
         BlockEntity blockEntity = world.getBlockEntity(this.pos);
         if (blockEntity instanceof EjectorBlockEntity) {
            ((EjectorBlockEntity)blockEntity).deployElytra(player);
         }
      }
   }
}
