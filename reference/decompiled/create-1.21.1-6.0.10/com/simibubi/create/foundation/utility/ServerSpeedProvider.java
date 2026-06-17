package com.simibubi.create.foundation.utility;

import com.simibubi.create.AllPackets;
import com.simibubi.create.infrastructure.config.AllConfigs;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.createmod.catnip.net.base.BasePacketPayload.PacketTypeProvider;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class ServerSpeedProvider {
   private static final LerpedFloat modifier = LerpedFloat.linear();
   private static int clientTimer = 0;
   private static int serverTimer = 0;
   private static boolean initialized = false;

   public static void serverTick() {
      serverTimer++;
      if (serverTimer > getSyncInterval()) {
         CatnipServices.NETWORK.sendToAllClients(ServerSpeedProvider.Packet.INSTANCE);
         serverTimer = 0;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void clientTick() {
      if (!Minecraft.getInstance().hasSingleplayerServer() || !Minecraft.getInstance().isPaused()) {
         modifier.tickChaser();
         clientTimer++;
      }
   }

   public static Integer getSyncInterval() {
      return (Integer)AllConfigs.server().tickrateSyncTimer.get();
   }

   public static float get() {
      return modifier.getValue();
   }

   public static enum Packet implements ClientboundPacketPayload {
      INSTANCE;

      public static final StreamCodec<ByteBuf, ServerSpeedProvider.Packet> STREAM_CODEC = StreamCodec.unit(INSTANCE);

      @OnlyIn(Dist.CLIENT)
      public void handle(LocalPlayer player) {
         if (!ServerSpeedProvider.initialized) {
            ServerSpeedProvider.initialized = true;
            ServerSpeedProvider.clientTimer = 0;
         } else {
            float target = (float)ServerSpeedProvider.getSyncInterval().intValue() / (float)Math.max(ServerSpeedProvider.clientTimer, 1);
            ServerSpeedProvider.modifier.chase((double)Math.min(target, 1.0F), 0.25, Chaser.EXP);
            ServerSpeedProvider.clientTimer = -1;
         }
      }

      public PacketTypeProvider getTypeProvider() {
         return AllPackets.SERVER_SPEED;
      }
   }
}
