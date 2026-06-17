package com.simibubi.create.content.trains.entity;

import com.simibubi.create.AllPackets;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.ContraptionRelocationPacket;
import com.simibubi.create.content.trains.track.BezierTrackPointLocation;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public record TrainRelocationPacket(UUID trainId, BlockPos pos, Vec3 lookAngle, int entityId, boolean direction, BezierTrackPointLocation hoveredBezier)
   implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, TrainRelocationPacket> STREAM_CODEC = StreamCodec.composite(
      UUIDUtil.STREAM_CODEC,
      TrainRelocationPacket::trainId,
      BlockPos.STREAM_CODEC,
      TrainRelocationPacket::pos,
      CatnipStreamCodecs.VEC3,
      TrainRelocationPacket::lookAngle,
      ByteBufCodecs.INT,
      TrainRelocationPacket::entityId,
      ByteBufCodecs.BOOL,
      TrainRelocationPacket::direction,
      CatnipStreamCodecBuilders.nullable(BezierTrackPointLocation.STREAM_CODEC),
      TrainRelocationPacket::hoveredBezier,
      TrainRelocationPacket::new
   );

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.RELOCATE_TRAIN;
   }

   public void handle(ServerPlayer sender) {
      Train train = Create.RAILWAYS.trains.get(this.trainId);
      Entity entity = sender.level().getEntity(this.entityId);
      String messagePrefix = sender.getName().getString() + " could not relocate Train ";
      if (train != null && entity instanceof CarriageContraptionEntity cce) {
         if (train.id.equals(cce.trainId)) {
            int verifyDistance = (Integer)AllConfigs.server().trains.maxTrackPlacementLength.get() * 2;
            if (!sender.canInteractWithBlock(this.pos, (double)verifyDistance)) {
               Create.LOGGER.warn(messagePrefix + train.name.getString() + ": player too far from clicked pos");
            } else if (!sender.canInteractWithEntity(cce, (double)verifyDistance)) {
               Create.LOGGER.warn(messagePrefix + train.name.getString() + ": player too far from carriage entity");
            } else if (TrainRelocator.relocate(train, sender.level(), this.pos, this.hoveredBezier, this.direction, this.lookAngle, false)) {
               sender.displayClientMessage(CreateLang.translateDirect("train.relocate.success").withStyle(ChatFormatting.GREEN), true);
               train.carriages.forEach(c -> c.forEachPresentEntity(e -> {
                     e.nonDamageTicks = 10;
                     CatnipServices.NETWORK.sendToClientsTrackingEntity(e, new ContraptionRelocationPacket(e.getId()));
                  }));
            } else {
               Create.LOGGER.warn(messagePrefix + train.name.getString() + ": relocation failed server-side");
            }
         }
      } else {
         Create.LOGGER.warn(messagePrefix + train.id.toString().substring(0, 5) + ": not present on server");
      }
   }
}
