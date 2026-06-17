package com.simibubi.create.content.trains.track;

import com.simibubi.create.AllPackets;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class CurvedTrackDestroyPacket extends BlockEntityConfigurationPacket<TrackBlockEntity> {
   public static final StreamCodec<ByteBuf, CurvedTrackDestroyPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      BlockPos.STREAM_CODEC,
      packet -> packet.targetPos,
      BlockPos.STREAM_CODEC,
      packet -> packet.soundSource,
      ByteBufCodecs.BOOL,
      packet -> packet.wrench,
      CurvedTrackDestroyPacket::new
   );
   private final BlockPos targetPos;
   private final BlockPos soundSource;
   private final boolean wrench;

   public CurvedTrackDestroyPacket(BlockPos pos, BlockPos targetPos, BlockPos soundSource, boolean wrench) {
      super(pos);
      this.targetPos = targetPos;
      this.soundSource = soundSource;
      this.wrench = wrench;
   }

   protected void applySettings(ServerPlayer player, TrackBlockEntity be) {
      int verifyDistance = (Integer)AllConfigs.server().trains.maxTrackPlacementLength.get() * 4;
      if (!player.canInteractWithBlock(be.getBlockPos(), (double)verifyDistance)) {
         Create.LOGGER.warn("{} too far away from destroyed Curve track", player.getScoreboardName());
      } else {
         Level level = be.getLevel();
         BezierConnection bezierConnection = be.getConnections().get(this.targetPos);
         be.removeConnection(this.targetPos);
         if (level.getBlockEntity(this.targetPos) instanceof TrackBlockEntity other) {
            other.removeConnection(this.pos);
         }

         BlockState blockState = be.getBlockState();
         TrackPropagator.onRailRemoved(level, this.pos, blockState);
         if (this.wrench) {
            AllSoundEvents.WRENCH_REMOVE.playOnServer(player.level(), this.soundSource, 1.0F, level.random.nextFloat() * 0.5F + 0.5F);
            if (!player.isCreative() && bezierConnection != null) {
               bezierConnection.addItemsToPlayer(player);
            }
         } else if (!player.isCreative() && bezierConnection != null) {
            bezierConnection.spawnItems(level);
         }

         bezierConnection.spawnDestroyParticles(level);
         SoundType soundtype = blockState.getSoundType(level, this.pos, player);
         if (soundtype != null) {
            level.playSound(
               null, this.soundSource, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F
            );
         }
      }
   }

   @Override
   protected int maxRange() {
      return (Integer)AllConfigs.server().trains.maxTrackPlacementLength.get() + 16;
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.DESTROY_CURVED_TRACK;
   }
}
