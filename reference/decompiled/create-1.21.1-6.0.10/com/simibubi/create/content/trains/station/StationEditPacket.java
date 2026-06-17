package com.simibubi.create.content.trains.station;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.decoration.slidingDoor.DoorControl;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class StationEditPacket extends BlockEntityConfigurationPacket<StationBlockEntity> {
   public static final StreamCodec<ByteBuf, StationEditPacket> STREAM_CODEC = StreamCodec.composite(
      BlockPos.STREAM_CODEC,
      packet -> packet.pos,
      ByteBufCodecs.BOOL,
      packet -> packet.dropSchedule,
      ByteBufCodecs.BOOL,
      packet -> packet.assemblyMode,
      CatnipStreamCodecBuilders.nullable(ByteBufCodecs.BOOL),
      packet -> packet.tryAssemble,
      CatnipStreamCodecBuilders.nullable(DoorControl.STREAM_CODEC),
      packet -> packet.doorControl,
      CatnipStreamCodecBuilders.nullable(ByteBufCodecs.stringUtf8(256)),
      packet -> packet.name,
      StationEditPacket::new
   );
   private final boolean dropSchedule;
   private final boolean assemblyMode;
   private final Boolean tryAssemble;
   private final DoorControl doorControl;
   private final String name;

   public static StationEditPacket dropSchedule(BlockPos pos) {
      return new StationEditPacket(pos, true, false, false, null, null);
   }

   public static StationEditPacket tryAssemble(BlockPos pos) {
      return new StationEditPacket(pos, false, false, true, null, null);
   }

   public static StationEditPacket tryDisassemble(BlockPos pos) {
      return new StationEditPacket(pos, false, false, false, null, null);
   }

   public static StationEditPacket configure(BlockPos pos, boolean assemble, String name, DoorControl doorControl) {
      return new StationEditPacket(pos, false, assemble, null, doorControl, name);
   }

   private StationEditPacket(BlockPos pos, boolean dropSchedule, boolean assemblyMode, Boolean tryAssemble, DoorControl doorControl, String name) {
      super(pos);
      this.dropSchedule = dropSchedule;
      this.assemblyMode = assemblyMode;
      this.tryAssemble = tryAssemble;
      this.doorControl = doorControl;
      this.name = name;
   }

   protected void applySettings(ServerPlayer player, StationBlockEntity be) {
      Level level = be.getLevel();
      BlockPos blockPos = be.getBlockPos();
      BlockState blockState = level.getBlockState(blockPos);
      GlobalStation station = be.getStation();
      if (this.dropSchedule) {
         if (station != null) {
            be.dropSchedule(player, station.getPresentTrain());
         }
      } else {
         if (this.doorControl != null) {
            be.doorControls.set(this.doorControl);
         }

         if (this.name != null && !this.name.isBlank()) {
            be.updateName(this.name);
         }

         if (blockState.getBlock() instanceof StationBlock) {
            Boolean isAssemblyMode = (Boolean)blockState.getValue(StationBlock.ASSEMBLING);
            boolean assemblyComplete = false;
            if (this.tryAssemble != null) {
               if (!isAssemblyMode) {
                  return;
               }

               if (!this.tryAssemble) {
                  if (be.tryDisassembleTrain(player) && be.tryEnterAssemblyMode()) {
                     be.refreshAssemblyInfo();
                  }
               } else {
                  be.assemble(player.getUUID());
                  assemblyComplete = station != null && station.getPresentTrain() != null;
               }

               if (!assemblyComplete) {
                  return;
               }
            }

            if (this.assemblyMode) {
               be.enterAssemblyMode(player);
            } else {
               be.exitAssemblyMode();
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONFIGURE_STATION;
   }
}
