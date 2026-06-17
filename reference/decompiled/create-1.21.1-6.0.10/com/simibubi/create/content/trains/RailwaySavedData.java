package com.simibubi.create.content.trains;

import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;

public class RailwaySavedData extends SavedData {
   private Map<UUID, TrackGraph> trackNetworks = new HashMap<>();
   private Map<UUID, SignalEdgeGroup> signalEdgeGroups = new HashMap<>();
   private Map<UUID, Train> trains = new HashMap<>();

   public static Factory<RailwaySavedData> factory() {
      return new Factory(RailwaySavedData::new, RailwaySavedData::load);
   }

   public CompoundTag save(CompoundTag nbt, Provider registries) {
      GlobalRailwayManager railways = Create.RAILWAYS;
      DimensionPalette dimensions = new DimensionPalette();
      nbt.put("RailGraphs", NBTHelper.writeCompoundList(railways.trackNetworks.values(), tg -> tg.write(registries, dimensions)));
      nbt.put(
         "SignalBlocks",
         NBTHelper.writeCompoundList(
            railways.signalEdgeGroups.values(), seg -> seg.fallbackGroup && !railways.trackNetworks.containsKey(seg.id) ? null : seg.write()
         )
      );
      nbt.put("Trains", NBTHelper.writeCompoundList(railways.trains.values(), t -> t.write(dimensions, registries)));
      dimensions.write(nbt);
      return nbt;
   }

   private static RailwaySavedData load(CompoundTag nbt, Provider registries) {
      RailwaySavedData sd = new RailwaySavedData();
      sd.trackNetworks = new HashMap<>();
      sd.signalEdgeGroups = new HashMap<>();
      sd.trains = new HashMap<>();
      DimensionPalette dimensions = DimensionPalette.read(nbt);
      NBTHelper.iterateCompoundList(nbt.getList("RailGraphs", 10), c -> {
         TrackGraph graphx = TrackGraph.read(c, registries, dimensions);
         sd.trackNetworks.put(graphx.id, graphx);
      });
      NBTHelper.iterateCompoundList(nbt.getList("SignalBlocks", 10), c -> {
         SignalEdgeGroup groupx = SignalEdgeGroup.read(c);
         sd.signalEdgeGroups.put(groupx.id, groupx);
      });
      NBTHelper.iterateCompoundList(nbt.getList("Trains", 10), c -> {
         Train train = Train.read(c, registries, sd.trackNetworks, dimensions);
         sd.trains.put(train.id, train);
      });

      for (TrackGraph graph : sd.trackNetworks.values()) {
         for (SignalBoundary signal : graph.getPoints(EdgePointType.SIGNAL)) {
            UUID groupId = (UUID)signal.groups.getFirst();
            UUID otherGroupId = (UUID)signal.groups.getSecond();
            if (groupId != null && otherGroupId != null) {
               SignalEdgeGroup group = sd.signalEdgeGroups.get(groupId);
               SignalEdgeGroup otherGroup = sd.signalEdgeGroups.get(otherGroupId);
               if (group != null && otherGroup != null) {
                  group.putAdjacent(otherGroupId);
                  otherGroup.putAdjacent(groupId);
               }
            }
         }
      }

      return sd;
   }

   public Map<UUID, TrackGraph> getTrackNetworks() {
      return this.trackNetworks;
   }

   public Map<UUID, Train> getTrains() {
      return this.trains;
   }

   public Map<UUID, SignalEdgeGroup> getSignalBlocks() {
      return this.signalEdgeGroups;
   }

   private RailwaySavedData() {
   }

   public static RailwaySavedData load(MinecraftServer server) {
      return (RailwaySavedData)server.overworld().getDataStorage().computeIfAbsent(factory(), "create_tracks");
   }
}
