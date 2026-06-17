package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record EjectorPlacementPacket(int h, int v, BlockPos pos, Direction facing) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, EjectorPlacementPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      EjectorPlacementPacket::h,
      ByteBufCodecs.INT,
      EjectorPlacementPacket::v,
      BlockPos.STREAM_CODEC,
      EjectorPlacementPacket::pos,
      Direction.STREAM_CODEC,
      EjectorPlacementPacket::facing,
      EjectorPlacementPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.PLACE_EJECTOR;
   }

   public void handle(ServerPlayer player) {
      Level world = player.level();
      if (world.isLoaded(this.pos)) {
         BlockEntity blockEntity = world.getBlockEntity(this.pos);
         BlockState state = world.getBlockState(this.pos);
         if (blockEntity instanceof EjectorBlockEntity) {
            ((EjectorBlockEntity)blockEntity).setTarget(this.h, this.v);
         }

         if (AllBlocks.WEIGHTED_EJECTOR.has(state)) {
            world.setBlockAndUpdate(this.pos, (BlockState)state.setValue(EjectorBlock.HORIZONTAL_FACING, this.facing));
         }
      }
   }

   public static record ClientBoundRequest(BlockPos pos) implements ClientboundPacketPayload {
      public static final StreamCodec<ByteBuf, EjectorPlacementPacket.ClientBoundRequest> STREAM_CODEC = BlockPos.STREAM_CODEC
         .map(EjectorPlacementPacket.ClientBoundRequest::new, EjectorPlacementPacket.ClientBoundRequest::pos);

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.S_PLACE_EJECTOR;
      }

      @OnlyIn(Dist.CLIENT)
      public void handle(LocalPlayer player) {
         EjectorTargetHandler.flushSettings(this.pos);
      }
   }
}
