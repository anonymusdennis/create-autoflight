package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public record ElevatorTargetFloorPacket(int entityId, int targetY) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ElevatorTargetFloorPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT, ElevatorTargetFloorPacket::entityId, ByteBufCodecs.INT, ElevatorTargetFloorPacket::targetY, ElevatorTargetFloorPacket::new
   );

   public ElevatorTargetFloorPacket(AbstractContraptionEntity entity, int targetY) {
      this(entity.getId(), targetY);
   }

   public void handle(ServerPlayer sender) {
      if (sender.serverLevel().getEntity(this.entityId) instanceof AbstractContraptionEntity ace) {
         if (ace.getContraption() instanceof ElevatorContraption ec) {
            if (!(ace.distanceToSqr(sender) > 2500.0)) {
               Level level = sender.level();
               ElevatorColumn elevatorColumn = ElevatorColumn.get(level, ec.getGlobalColumn());
               if (elevatorColumn.contacts.contains(this.targetY)) {
                  if (!ec.isTargetUnreachable(this.targetY)) {
                     BlockPos pos = elevatorColumn.contactAt(this.targetY);
                     BlockState blockState = level.getBlockState(pos);
                     if (blockState.getBlock() instanceof ElevatorContactBlock ecb) {
                        ecb.callToContactAndUpdate(elevatorColumn, blockState, level, pos, false);
                     }
                  }
               }
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.ELEVATOR_SET_FLOOR;
   }
}
