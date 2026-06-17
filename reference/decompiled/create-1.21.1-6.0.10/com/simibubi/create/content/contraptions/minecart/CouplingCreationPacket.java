package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

public record CouplingCreationPacket(int id1, int id2) implements ServerboundPacketPayload {
   public static final StreamCodec<ByteBuf, CouplingCreationPacket> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, CouplingCreationPacket::id1, ByteBufCodecs.VAR_INT, CouplingCreationPacket::id2, CouplingCreationPacket::new
   );

   public CouplingCreationPacket(AbstractMinecart cart1, AbstractMinecart cart2) {
      this(cart1.getId(), cart2.getId());
   }

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.MINECART_COUPLING_CREATION;
   }

   public void handle(ServerPlayer player) {
      CouplingHandler.tryToCoupleCarts(player, player.level(), this.id1, this.id2);
   }
}
