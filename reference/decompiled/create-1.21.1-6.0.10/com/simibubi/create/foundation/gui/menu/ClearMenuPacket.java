package com.simibubi.create.foundation.gui.menu;

import com.simibubi.create.AllPackets;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

public enum ClearMenuPacket implements ServerboundPacketPayload {
   INSTANCE;

   public static final StreamCodec<ByteBuf, ClearMenuPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

   public PacketTypeProvider getTypeProvider() {
      return AllPackets.CLEAR_CONTAINER;
   }

   public void handle(ServerPlayer player) {
      if (player != null) {
         if (player.containerMenu instanceof IClearableMenu) {
            ((IClearableMenu)player.containerMenu).clearContents();
         }
      }
   }
}
