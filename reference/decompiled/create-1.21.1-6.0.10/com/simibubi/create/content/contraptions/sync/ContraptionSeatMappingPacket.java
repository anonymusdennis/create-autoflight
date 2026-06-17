package com.simibubi.create.content.contraptions.sync;

import com.simibubi.create.AllPackets;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ContraptionSeatMappingPacket(int entityId, Map<UUID, Integer> mapping, int dismountedId) implements ClientboundPacketPayload {
   public static final StreamCodec<ByteBuf, ContraptionSeatMappingPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      ContraptionSeatMappingPacket::entityId,
      ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.INT),
      ContraptionSeatMappingPacket::mapping,
      ByteBufCodecs.INT,
      ContraptionSeatMappingPacket::dismountedId,
      ContraptionSeatMappingPacket::new
   );

   public ContraptionSeatMappingPacket(int entityId, Map<UUID, Integer> mapping, int dismountedId) {
      mapping = Map.copyOf(mapping);
      this.entityId = entityId;
      this.mapping = mapping;
      this.dismountedId = dismountedId;
   }

   public ContraptionSeatMappingPacket(int entityID, Map<UUID, Integer> mapping) {
      this(entityID, mapping, -1);
   }

   @OnlyIn(Dist.CLIENT)
   public void handle(LocalPlayer player) {
      if (player.clientLevel.getEntity(this.entityId) instanceof AbstractContraptionEntity contraptionEntity) {
         if (this.dismountedId == player.getId()) {
            Vec3 transformedVector = contraptionEntity.getPassengerPosition(player, 1.0F);
            if (transformedVector != null) {
               player.getPersistentData().put("ContraptionDismountLocation", VecHelper.writeNBT(transformedVector));
            }
         }

         contraptionEntity.getContraption().setSeatMapping(this.mapping);
      }
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CONTRAPTION_SEAT_MAPPING;
   }
}
