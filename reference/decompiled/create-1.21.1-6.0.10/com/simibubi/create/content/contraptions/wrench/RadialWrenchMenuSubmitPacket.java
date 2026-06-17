package com.simibubi.create.content.contraptions.wrench;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record RadialWrenchMenuSubmitPacket(BlockPos blockPos, BlockState newState) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, RadialWrenchMenuSubmitPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      RadialWrenchMenuSubmitPacket::blockPos,
      CatnipStreamCodecs.BLOCK_STATE,
      RadialWrenchMenuSubmitPacket::newState,
      RadialWrenchMenuSubmitPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.RADIAL_WRENCH_MENU_SUBMIT;
   }

   public void handle(ServerPlayer player) {
      Level level = player.level();
      if (level.getBlockState(this.blockPos).is(this.newState.getBlock())) {
         BlockState updatedState = Block.updateFromNeighbourShapes(this.newState, level, this.blockPos);
         KineticBlockEntity.switchToBlockState(level, this.blockPos, updatedState);
         IWrenchable.playRotateSound(level, this.blockPos);
      }
   }
}
