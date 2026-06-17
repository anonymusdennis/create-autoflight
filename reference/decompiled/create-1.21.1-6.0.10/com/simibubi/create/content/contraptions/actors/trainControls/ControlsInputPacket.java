package com.simibubi.create.content.contraptions.actors.trainControls;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public record ControlsInputPacket(List<Integer> activatedButtons, boolean press, int contraptionEntityId, BlockPos controlsPos, boolean stopControlling)
   implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, ControlsInputPacket> STREAM_CODEC = StreamCodec.composite(
      CatnipStreamCodecBuilders.list(ByteBufCodecs.VAR_INT),
      ControlsInputPacket::activatedButtons,
      ByteBufCodecs.BOOL,
      ControlsInputPacket::press,
      ByteBufCodecs.INT,
      ControlsInputPacket::contraptionEntityId,
      BlockPos.STREAM_CODEC,
      ControlsInputPacket::controlsPos,
      ByteBufCodecs.BOOL,
      ControlsInputPacket::stopControlling,
      ControlsInputPacket::new
   );

   public ControlsInputPacket(Collection<Integer> activatedButtons, boolean press, int contraptionEntityId, BlockPos controlsPos, boolean stopControlling) {
      this(List.copyOf(activatedButtons), press, contraptionEntityId, controlsPos, stopControlling);
   }

   public void handle(ServerPlayer player) {
      Level world = player.getCommandSenderWorld();
      UUID uniqueID = player.getUUID();
      if (!player.isSpectator() || !this.press) {
         if (world.getEntity(this.contraptionEntityId) instanceof AbstractContraptionEntity ace) {
            if (this.stopControlling) {
               ace.stopControlling(this.controlsPos);
            } else {
               if (ace.canInteractWithBlock(player, this.controlsPos, 16.0)) {
                  ControlsServerHandler.receivePressed(world, ace, this.controlsPos, uniqueID, this.activatedButtons, this.press);
               }
            }
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONTROLS_INPUT;
   }
}
