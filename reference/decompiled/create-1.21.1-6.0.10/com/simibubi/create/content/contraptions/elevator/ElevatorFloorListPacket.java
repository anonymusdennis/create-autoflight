package com.simibubi.create.content.contraptions.elevator;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.IntAttached;
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

public record ElevatorFloorListPacket(int entityId, List<IntAttached<Couple<String>>> floors) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, ElevatorFloorListPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      ElevatorFloorListPacket::entityId,
      CatnipStreamCodecBuilders.list(IntAttached.streamCodec(Couple.streamCodec(ByteBufCodecs.STRING_UTF8))),
      ElevatorFloorListPacket::floors,
      ElevatorFloorListPacket::new
   );

   public ElevatorFloorListPacket(AbstractContraptionEntity entity, List<IntAttached<Couple<String>>> floors) {
      this(entity.getId(), floors);
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (player.clientLevel.getEntity(this.entityId) instanceof AbstractContraptionEntity ace) {
         if (ace.getContraption() instanceof ElevatorContraption ec) {
            ec.namesList = this.floors;
            ec.syncControlDisplays();
         }
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.UPDATE_ELEVATOR_FLOORS;
   }

   public static record RequestFloorList(int entityId) implements ServerboundPacketPayload {
      public static final StreamCodec<ByteBuf, ElevatorFloorListPacket.RequestFloorList> STREAM_CODEC = ByteBufCodecs.INT
         .map(ElevatorFloorListPacket.RequestFloorList::new, ElevatorFloorListPacket.RequestFloorList::entityId);

      public RequestFloorList(AbstractContraptionEntity entity) {
         this(entity.getId());
      }

      public void handle(ServerPlayer sender) {
         if (sender.level().getEntity(this.entityId) instanceof AbstractContraptionEntity ace) {
            if (ace.getContraption() instanceof ElevatorContraption ec) {
               CatnipServices.NETWORK.sendToClient(sender, new ElevatorFloorListPacket(ace, ec.namesList));
            }
         }
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.REQUEST_FLOOR_LIST;
      }
   }
}
