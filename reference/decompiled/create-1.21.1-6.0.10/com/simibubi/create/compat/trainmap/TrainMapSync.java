package com.simibubi.create.compat.trainmap;

import com.google.common.cache.Cache;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.ScheduleRuntime;
import com.simibubi.create.content.trains.signal.SignalBlock;
import com.simibubi.create.content.trains.signal.SignalBoundary;
import com.simibubi.create.content.trains.signal.SignalEdgeGroup;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.foundation.utility.TickBasedCache;
import io.netty.buffer.ByteBuf;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipLargerStreamCodecs;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public class TrainMapSync {
   public static final int lightPacketInterval = 5;
   public static final int fullPacketInterval = 10;
   public static int ticks;
   public static Cache<UUID, WeakReference<ServerPlayer>> requestingPlayers = new TickBasedCache<>(20, false);

   public static void requestReceived(ServerPlayer sender) {
      boolean sendImmediately = requestingPlayers.getIfPresent(sender.getUUID()) == null;
      requestingPlayers.put(sender.getUUID(), new WeakReference<>(sender));
      if (sendImmediately) {
         send(sender.server, false);
      }
   }

   public static void serverTick(ServerTickEvent event) {
      ticks++;
      if (ticks % 10 == 0) {
         send(event.getServer(), false);
      } else if (ticks % 5 == 0) {
         send(event.getServer(), true);
      }
   }

   public static void send(MinecraftServer minecraftServer, boolean light) {
      if (requestingPlayers.size() != 0L) {
         TrainMapSyncPacket packet = new TrainMapSyncPacket(light);

         for (Train train : Create.RAILWAYS.trains.values()) {
            packet.add(train.id, createEntry(minecraftServer, train));
         }

         for (WeakReference<ServerPlayer> weakReference : requestingPlayers.asMap().values()) {
            ServerPlayer player = weakReference.get();
            if (player != null) {
               CatnipServices.NETWORK.sendToClient(player, packet);
            }
         }
      }
   }

   private static TrainMapSync.TrainMapSyncEntry createEntry(MinecraftServer minecraftServer, Train train) {
      TrainMapSync.TrainMapSyncEntry entry = new TrainMapSync.TrainMapSyncEntry();
      boolean stopped = Math.abs(train.speed) < 0.05;
      entry.positions = new Float[train.carriages.size() * 6];
      entry.dimensions = new ArrayList<>();
      Arrays.fill(entry.positions, Float.valueOf(0.0F));
      List<Carriage> carriages = train.carriages;

      for (int i = 0; i < carriages.size(); i++) {
         Carriage carriage = carriages.get(i);
         Vec3 leadingPos;
         Vec3 trailingPos;
         if (train.graph == null) {
            Pair<ResourceKey<Level>, Carriage.DimensionalCarriageEntity> dimCarriage = carriage.anyAvailableDimensionalCarriage();
            if (dimCarriage == null || carriage.presentInMultipleDimensions()) {
               entry.dimensions.add(null);
               continue;
            }

            leadingPos = (Vec3)((Carriage.DimensionalCarriageEntity)dimCarriage.getSecond()).rotationAnchors.getFirst();
            trailingPos = (Vec3)((Carriage.DimensionalCarriageEntity)dimCarriage.getSecond()).rotationAnchors.getSecond();
            if (leadingPos == null || trailingPos == null) {
               entry.dimensions.add(null);
               continue;
            }

            entry.dimensions.add((ResourceKey<Level>)dimCarriage.getFirst());
         } else {
            TravellingPoint leading = carriage.getLeadingPoint();
            TravellingPoint trailing = carriage.getTrailingPoint();
            if (leading == null || trailing == null || leading.edge == null || trailing.edge == null) {
               entry.dimensions.add(null);
               continue;
            }

            ResourceKey<Level> leadingDim = leading.node1 != null && leading.edge != null && !leading.edge.isInterDimensional()
               ? leading.node1.getLocation().getDimension()
               : null;
            ResourceKey<Level> trailingDim = trailing.node1 != null && trailing.edge != null && !trailing.edge.isInterDimensional()
               ? trailing.node1.getLocation().getDimension()
               : null;
            ResourceKey<Level> carriageDim = leadingDim != null && leadingDim == trailingDim ? leadingDim : null;
            entry.dimensions.add(carriageDim);
            leadingPos = leading.getPosition(train.graph);
            trailingPos = trailing.getPosition(train.graph);
         }

         entry.positions[i * 6] = (float)leadingPos.x();
         entry.positions[i * 6 + 1] = (float)leadingPos.y();
         entry.positions[i * 6 + 2] = (float)leadingPos.z();
         entry.positions[i * 6 + 3] = (float)trailingPos.x();
         entry.positions[i * 6 + 4] = (float)trailingPos.y();
         entry.positions[i * 6 + 5] = (float)trailingPos.z();
      }

      entry.backwards = train.currentlyBackwards;
      if (train.owner != null) {
         ServerPlayer owner = minecraftServer.getPlayerList().getPlayer(train.owner);
         if (owner != null) {
            entry.ownerName = owner.getName().getString();
         }
      }

      if (train.derailed) {
         entry.state = TrainMapSync.TrainState.DERAILED;
         return entry;
      } else {
         ScheduleRuntime runtime = train.runtime;
         if (runtime.getSchedule() != null && stopped) {
            if (runtime.paused) {
               entry.state = TrainMapSync.TrainState.SCHEDULE_INTERRUPTED;
               return entry;
            }

            if (train.status.conductor) {
               entry.state = TrainMapSync.TrainState.CONDUCTOR_MISSING;
               return entry;
            }

            if (train.status.navigation) {
               entry.state = TrainMapSync.TrainState.NAVIGATION_FAILED;
               return entry;
            }
         }

         if ((runtime.getSchedule() == null || runtime.paused) && train.speed != 0.0) {
            entry.state = TrainMapSync.TrainState.RUNNING_MANUALLY;
         }

         GlobalStation currentStation = train.getCurrentStation();
         if (currentStation != null) {
            entry.targetStationName = currentStation.name;
            entry.targetStationDistance = 0;
         } else if (train.navigation.destination != null && !runtime.paused) {
            entry.targetStationName = train.navigation.destination.name;
            entry.targetStationDistance = Math.max(0, Mth.floor(train.navigation.distanceToDestination));
         }

         if (stopped && train.navigation.waitingForSignal != null) {
            UUID signalId = (UUID)train.navigation.waitingForSignal.getFirst();
            boolean side = (Boolean)train.navigation.waitingForSignal.getSecond();
            SignalBoundary signal = train.graph.getPoint(EdgePointType.SIGNAL, signalId);
            if (signal != null) {
               boolean chainSignal = signal.types.get(side) == SignalBlock.SignalType.CROSS_SIGNAL;
               entry.signalState = chainSignal ? TrainMapSync.SignalState.CHAIN_SIGNAL : TrainMapSync.SignalState.BLOCK_SIGNAL;
               if (signal.isForcedRed(side)) {
                  entry.signalState = TrainMapSync.SignalState.WAITING_FOR_REDSTONE;
               } else {
                  SignalEdgeGroup group = Create.RAILWAYS.signalEdgeGroups.get(signal.groups.get(side));
                  if (group != null) {
                     for (Train other : group.trains) {
                        if (other != train) {
                           entry.waitingForTrain = other.id;
                           break;
                        }
                     }
                  }
               }
            }
         }

         if (train.fuelTicks > 0 && !stopped) {
            entry.fueled = true;
         }

         return entry;
      }
   }

   public static enum SignalState {
      NOT_WAITING,
      WAITING_FOR_REDSTONE,
      BLOCK_SIGNAL,
      CHAIN_SIGNAL;

      public static final StreamCodec<ByteBuf, TrainMapSync.SignalState> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(TrainMapSync.SignalState.class);
   }

   public static class TrainMapSyncEntry {
      public static final StreamCodec<FriendlyByteBuf, TrainMapSync.TrainMapSyncEntry> STREAM_CODEC = CatnipLargerStreamCodecs.composite(
         CatnipStreamCodecBuilders.array(ByteBufCodecs.FLOAT, Float.class),
         packet -> packet.positions,
         CatnipStreamCodecBuilders.list(CatnipStreamCodecBuilders.nullable(ResourceKey.streamCodec(Registries.DIMENSION))),
         packet -> packet.dimensions,
         TrainMapSync.TrainState.STREAM_CODEC,
         packet -> packet.state,
         TrainMapSync.SignalState.STREAM_CODEC,
         packet -> packet.signalState,
         ByteBufCodecs.BOOL,
         packet -> packet.fueled,
         ByteBufCodecs.BOOL,
         packet -> packet.backwards,
         ByteBufCodecs.VAR_INT,
         packet -> packet.targetStationDistance,
         ByteBufCodecs.STRING_UTF8,
         packet -> packet.ownerName,
         ByteBufCodecs.STRING_UTF8,
         packet -> packet.targetStationName,
         CatnipStreamCodecBuilders.nullable(UUIDUtil.STREAM_CODEC),
         packet -> packet.waitingForTrain,
         TrainMapSync.TrainMapSyncEntry::new
      );
      public Float[] prevPositions;
      public List<ResourceKey<Level>> prevDims;
      public Float[] positions;
      public List<ResourceKey<Level>> dimensions;
      public TrainMapSync.TrainState state = TrainMapSync.TrainState.RUNNING;
      public TrainMapSync.SignalState signalState = TrainMapSync.SignalState.NOT_WAITING;
      public boolean fueled = false;
      public boolean backwards = false;
      public int targetStationDistance = 0;
      public String ownerName = "";
      public String targetStationName = "";
      public UUID waitingForTrain = null;

      public TrainMapSyncEntry() {
      }

      public TrainMapSyncEntry(
         Float[] positions,
         List<ResourceKey<Level>> dimensions,
         TrainMapSync.TrainState state,
         TrainMapSync.SignalState signalState,
         boolean fueled,
         boolean backwards,
         int targetStationDistance,
         String ownerName,
         String targetStationName,
         UUID waitingForTrain
      ) {
         this.positions = positions;
         this.dimensions = dimensions;
         this.state = state;
         this.signalState = signalState;
         this.fueled = fueled;
         this.backwards = backwards;
         this.targetStationDistance = targetStationDistance;
         this.ownerName = ownerName;
         this.targetStationName = targetStationName;
         this.waitingForTrain = waitingForTrain;
      }

      public void updateFrom(TrainMapSync.TrainMapSyncEntry other, boolean light) {
         this.prevPositions = this.positions;
         this.prevDims = this.dimensions;
         this.positions = other.positions;
         this.dimensions = other.dimensions;
         this.state = other.state;
         this.signalState = other.signalState;
         this.fueled = other.fueled;
         this.backwards = other.backwards;
         this.targetStationDistance = other.targetStationDistance;
         if (this.prevDims != null) {
            for (int i = 0; i < Math.min(this.prevDims.size(), this.dimensions.size()); i++) {
               if (this.prevDims.get(i) != this.dimensions.get(i)) {
                  System.arraycopy(this.positions, i * 6, this.prevPositions, i * 6, 6);
               }
            }
         }

         if (!light) {
            this.ownerName = other.ownerName;
            this.targetStationName = other.targetStationName;
            this.waitingForTrain = other.waitingForTrain;
         }
      }

      public Vec3 getPosition(int carriageIndex, boolean firstBogey, double time) {
         int startIndex = carriageIndex * 6 + (firstBogey ? 0 : 3);
         if (this.positions != null && this.positions.length > startIndex + 2) {
            Vec3 position = new Vec3(
               (double)this.positions[startIndex].floatValue(),
               (double)this.positions[startIndex + 1].floatValue(),
               (double)this.positions[startIndex + 2].floatValue()
            );
            if (this.prevPositions != null && this.prevPositions.length > startIndex + 2) {
               Vec3 prevPosition = new Vec3(
                  (double)this.prevPositions[startIndex].floatValue(),
                  (double)this.prevPositions[startIndex + 1].floatValue(),
                  (double)this.prevPositions[startIndex + 2].floatValue()
               );
               return prevPosition.lerp(position, time);
            } else {
               return position;
            }
         } else {
            return Vec3.ZERO;
         }
      }
   }

   public static enum TrainState {
      RUNNING,
      RUNNING_MANUALLY,
      DERAILED,
      SCHEDULE_INTERRUPTED,
      CONDUCTOR_MISSING,
      NAVIGATION_FAILED;

      public static final StreamCodec<ByteBuf, TrainMapSync.TrainState> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(TrainMapSync.TrainState.class);
   }
}
