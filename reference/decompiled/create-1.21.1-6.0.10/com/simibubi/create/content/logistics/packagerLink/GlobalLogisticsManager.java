package com.simibubi.create.content.logistics.packagerLink;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

public class GlobalLogisticsManager {
   public Map<UUID, LogisticsNetwork> logisticsNetworks = new HashMap<>();
   private LogisticsNetworkSavedData savedData;

   public void levelLoaded(LevelAccessor level) {
      MinecraftServer server = level.getServer();
      if (server != null && server.overworld() == level) {
         this.logisticsNetworks = new HashMap<>();
         this.savedData = null;
         this.loadLogisticsData(server);
      }
   }

   public boolean mayInteract(UUID networkId, Player player) {
      LogisticsNetwork network = this.logisticsNetworks.get(networkId);
      return network == null || network.owner == null || !network.locked || network.owner.equals(player.getUUID());
   }

   public boolean mayAdministrate(UUID networkId, Player player) {
      LogisticsNetwork network = this.logisticsNetworks.get(networkId);
      return network == null || network.owner == null || network.owner.equals(player.getUUID());
   }

   public boolean isLockable(UUID networkId) {
      LogisticsNetwork network = this.logisticsNetworks.get(networkId);
      return network != null;
   }

   public boolean isLocked(UUID networkId) {
      LogisticsNetwork network = this.logisticsNetworks.get(networkId);
      return network != null && network.locked;
   }

   public void linkAdded(UUID networkId, GlobalPos pos, UUID ownedBy) {
      LogisticsNetwork network = this.logisticsNetworks.computeIfAbsent(networkId, $ -> new LogisticsNetwork(networkId));
      network.totalLinks.add(pos);
      if (ownedBy != null && network.owner == null) {
         network.owner = ownedBy;
      }

      this.markDirty();
   }

   public void linkLoaded(UUID networkId, GlobalPos pos) {
      this.logisticsNetworks.computeIfAbsent(networkId, $ -> new LogisticsNetwork(networkId)).loadedLinks.add(pos);
   }

   public void linkRemoved(UUID networkId, GlobalPos pos) {
      LogisticsNetwork logisticsNetwork = this.logisticsNetworks.get(networkId);
      if (logisticsNetwork != null) {
         logisticsNetwork.totalLinks.remove(pos);
         logisticsNetwork.loadedLinks.remove(pos);
         if (logisticsNetwork.totalLinks.size() <= 0) {
            this.logisticsNetworks.remove(networkId);
         }

         this.markDirty();
      }
   }

   public void linkInvalidated(UUID networkId, GlobalPos pos) {
      LogisticsNetwork logisticsNetwork = this.logisticsNetworks.get(networkId);
      if (logisticsNetwork != null) {
         logisticsNetwork.loadedLinks.remove(pos);
      }
   }

   public int getUnloadedLinkCount(UUID networkId) {
      LogisticsNetwork logisticsNetwork = this.logisticsNetworks.get(networkId);
      return logisticsNetwork == null ? 0 : logisticsNetwork.totalLinks.size() - logisticsNetwork.loadedLinks.size();
   }

   @Nullable
   public RequestPromiseQueue getQueuedPromises(UUID networkId) {
      return !this.logisticsNetworks.containsKey(networkId) ? null : this.logisticsNetworks.get(networkId).panelPromises;
   }

   public boolean hasQueuedPromises(UUID networkId) {
      return this.logisticsNetworks.containsKey(networkId) && !this.logisticsNetworks.get(networkId).panelPromises.isEmpty();
   }

   private void loadLogisticsData(MinecraftServer server) {
      if (this.savedData == null) {
         this.savedData = LogisticsNetworkSavedData.load(server);
         this.logisticsNetworks = this.savedData.getLogisticsNetworks();
      }
   }

   public void tick(Level level) {
      if (level.dimension() == Level.OVERWORLD) {
         this.logisticsNetworks.forEach((id, network) -> network.panelPromises.tick());
      }
   }

   public void markDirty() {
      if (this.savedData != null) {
         this.savedData.setDirty();
      }
   }
}
