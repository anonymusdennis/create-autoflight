package net.createmod.catnip.platform.services;

import net.createmod.catnip.net.base.CatnipPacketRegistry;
import net.createmod.catnip.net.packets.ClientboundSimpleActionPacket;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

public interface NetworkHelper {
   @Internal
   void registerPackets(CatnipPacketRegistry var1);

   @OnlyIn(Dist.CLIENT)
   void sendToServer(CustomPacketPayload var1);

   void sendToClient(ServerPlayer var1, CustomPacketPayload var2);

   default void sendToClients(Iterable<ServerPlayer> players, CustomPacketPayload payload) {
      for (ServerPlayer player : players) {
         this.sendToClient(player, payload);
      }
   }

   void sendToAllClients(CustomPacketPayload var1);

   void sendToClientsTrackingAndSelf(Entity var1, CustomPacketPayload var2);

   void sendToClientsTrackingEntity(Entity var1, CustomPacketPayload var2);

   void sendToClientsTrackingChunk(ServerLevel var1, ChunkPos var2, CustomPacketPayload var3);

   void sendToClientsAround(ServerLevel var1, Vec3 var2, double var3, CustomPacketPayload var5);

   default void sendToClientsAround(ServerLevel serverLevel, Vec3i pos, double radius, CustomPacketPayload payload) {
      this.sendToClientsAround(serverLevel, new Vec3((double)pos.getX(), (double)pos.getY(), (double)pos.getZ()), radius, payload);
   }

   default void simpleActionToClient(ServerPlayer player, String action, String value) {
      this.sendToClient(player, new ClientboundSimpleActionPacket(action, value));
   }
}
