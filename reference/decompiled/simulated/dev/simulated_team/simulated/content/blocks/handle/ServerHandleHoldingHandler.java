package dev.simulated_team.simulated.content.blocks.handle;

import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.packets.handle.ClientboundPlayersHoldingHandlePacket;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.UUID;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.world.entity.player.Player;

public class ServerHandleHoldingHandler {
   public static Object2IntMap<UUID> holdingPlayers = new Object2IntOpenHashMap();
   public static int ticks;

   public static void startHolding(Player player) {
      int count = holdingPlayers.size();
      if (holdingPlayers.put(player.getUUID(), 20) <= 0) {
         SimStats.INTERACT_WITH_HANDLE.awardTo(player);
      }

      if (holdingPlayers.size() != count) {
         sync();
      }
   }

   public static void stopHolding(Player player) {
      if (holdingPlayers.removeInt(player.getUUID()) != 0) {
         sync();
      }
   }

   public static void tick() {
      ticks++;
      int before = holdingPlayers.size();
      ObjectIterator<Entry<UUID>> iterator = holdingPlayers.object2IntEntrySet().iterator();

      while (iterator.hasNext()) {
         java.util.Map.Entry<UUID, Integer> entry = (java.util.Map.Entry<UUID, Integer>)iterator.next();
         int newTTL = entry.getValue() - 1;
         if (newTTL <= 0) {
            iterator.remove();
         } else {
            entry.setValue(newTTL);
         }
      }

      int after = holdingPlayers.size();
      if (ticks % 10 == 0 || before != after) {
         sync();
      }
   }

   public static void sync() {
      CatnipServices.NETWORK.sendToAllClients(new ClientboundPlayersHoldingHandlePacket(holdingPlayers.keySet()));
   }
}
