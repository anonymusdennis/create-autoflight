package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.Create;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class LogisticsNetworkSavedData extends SavedData {
   private Map<UUID, LogisticsNetwork> logisticsNetworks = new HashMap<>();

   public static Factory<LogisticsNetworkSavedData> factory() {
      return new Factory(LogisticsNetworkSavedData::new, LogisticsNetworkSavedData::load);
   }

   public CompoundTag save(CompoundTag nbt, Provider registries) {
      GlobalLogisticsManager logistics = Create.LOGISTICS;
      nbt.put("LogisticsNetworks", NBTHelper.writeCompoundList(logistics.logisticsNetworks.values(), network -> network.write(registries)));
      return nbt;
   }

   private static LogisticsNetworkSavedData load(CompoundTag nbt, Provider registries) {
      LogisticsNetworkSavedData sd = new LogisticsNetworkSavedData();
      sd.logisticsNetworks = new HashMap<>();
      NBTHelper.iterateCompoundList(nbt.getList("LogisticsNetworks", 10), c -> {
         LogisticsNetwork network = LogisticsNetwork.read(c, registries);
         sd.logisticsNetworks.put(network.id, network);
      });
      return sd;
   }

   public Map<UUID, LogisticsNetwork> getLogisticsNetworks() {
      return this.logisticsNetworks;
   }

   private LogisticsNetworkSavedData() {
   }

   public static LogisticsNetworkSavedData load(MinecraftServer server) {
      return (LogisticsNetworkSavedData)server.overworld().getDataStorage().computeIfAbsent(factory(), "create_logistics");
   }
}
