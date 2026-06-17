package com.simibubi.create.compat.trainmap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;

public class TrainMapSyncClient {
   public static Map<UUID, TrainMapSync.TrainMapSyncEntry> currentData = new HashMap<>();
   public static double lastPacket;
   private static int ticks;

   public static void requestData() {
      ticks++;
      if (ticks % 5 == 0) {
         CatnipServices.NETWORK.sendToServer(TrainMapSyncRequestPacket.INSTANCE);
      }
   }

   public static void stopRequesting() {
      ticks = 0;
      currentData.clear();
   }

   public static void receive(TrainMapSyncPacket packet) {
      if (ticks != 0) {
         lastPacket = (double)AnimationTickHolder.getTicks();
         lastPacket = lastPacket + (double)AnimationTickHolder.getPartialTicks();
         Set<UUID> staleEntries = new HashSet<>(currentData.keySet());

         for (Pair<UUID, TrainMapSync.TrainMapSyncEntry> pair : packet.entries) {
            UUID id = (UUID)pair.getFirst();
            TrainMapSync.TrainMapSyncEntry entry = (TrainMapSync.TrainMapSyncEntry)pair.getSecond();
            staleEntries.remove(id);
            currentData.computeIfAbsent(id, $ -> entry).updateFrom(entry, packet.light);
         }

         for (UUID uuid : staleEntries) {
            currentData.remove(uuid);
         }
      }
   }
}
