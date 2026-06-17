package com.simibubi.create.foundation.networking;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.events.CommonEvents;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public enum LeftClickPacket implements ServerboundPacketPayload {
   INSTANCE;

   public static final StreamCodec<ByteBuf, LeftClickPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.LEFT_CLICK;
   }

   public void handle(ServerPlayer player) {
      CommonEvents.leftClickEmpty(player);
   }
}
