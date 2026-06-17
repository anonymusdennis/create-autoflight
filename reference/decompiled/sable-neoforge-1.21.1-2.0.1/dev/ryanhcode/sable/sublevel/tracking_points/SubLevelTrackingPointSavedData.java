package dev.ryanhcode.sable.sublevel.tracking_points;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import dev.ryanhcode.sable.util.SableNBTUtils;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelTrackingPointSavedData extends SavedData implements SubLevelObserver {
   public static final String FILE_ID = "sable_tracking_points";
   private final ServerLevel level;
   private final Map<UUID, TrackingPoint> trackingPoints = new Object2ObjectOpenHashMap();

   private SubLevelTrackingPointSavedData(ServerLevel level) {
      this.level = level;
   }

   public static SubLevelTrackingPointSavedData getOrLoad(ServerLevel level) {
      return (SubLevelTrackingPointSavedData)level.getDataStorage()
         .computeIfAbsent(new Factory(() -> new SubLevelTrackingPointSavedData(level), (tag, provider) -> load(level, tag), null), "sable_tracking_points");
   }

   private static SubLevelTrackingPointSavedData load(ServerLevel level, CompoundTag tag) {
      SubLevelTrackingPointSavedData data = new SubLevelTrackingPointSavedData(level);
      CompoundTag trackingPointsTag = tag.getCompound("tracking_points");

      for (String key : trackingPointsTag.getAllKeys()) {
         UUID uuid = UUID.fromString(key);
         CompoundTag pointTag = trackingPointsTag.getCompound(key);
         boolean inSubLevel = pointTag.getBoolean("InSubLevel");
         GlobalSavedSubLevelPointer pointer = pointTag.contains("SubLevelPointer")
            ? (GlobalSavedSubLevelPointer)GlobalSavedSubLevelPointer.CODEC.parse(NbtOps.INSTANCE, pointTag.getCompound("SubLevelPointer")).getOrThrow()
            : null;
         Vector3d point = SableNBTUtils.readVector3d(pointTag.getCompound("Point"));
         Vector3d globalPlaceholder = null;
         if (pointTag.contains("GlobalPlaceholder")) {
            globalPlaceholder = SableNBTUtils.readVector3d(pointTag.getCompound("GlobalPlaceholder"));
         }

         UUID subLevelID = null;
         if (pointTag.contains("SubLevelID")) {
            subLevelID = pointTag.getUUID("SubLevelID");
         }

         TrackingPoint trackingPoint = new TrackingPoint(inSubLevel, subLevelID, pointer, point, globalPlaceholder);
         data.trackingPoints.put(uuid, trackingPoint);
      }

      return data;
   }

   @NotNull
   public CompoundTag save(@NotNull CompoundTag compoundTag, @NotNull Provider provider) {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);

      assert container != null : "Sub-level container is null";

      CompoundTag loginPointsTag = new CompoundTag();

      for (Entry<UUID, TrackingPoint> entry : this.trackingPoints.entrySet()) {
         CompoundTag pointTag = new CompoundTag();
         TrackingPoint trackingPoint = entry.getValue();
         pointTag.putBoolean("InSubLevel", trackingPoint.inSubLevel());
         if (trackingPoint.lastSavedSubLevelPointer() != null) {
            pointTag.put(
               "SubLevelPointer", (Tag)GlobalSavedSubLevelPointer.CODEC.encodeStart(NbtOps.INSTANCE, trackingPoint.lastSavedSubLevelPointer()).getOrThrow()
            );
         }

         pointTag.put("Point", SableNBTUtils.writeVector3d(trackingPoint.point()));
         if (trackingPoint.globalPlaceholderPosition() != null) {
            pointTag.put("GlobalPlaceholder", SableNBTUtils.writeVector3d(trackingPoint.globalPlaceholderPosition()));
         }

         if (trackingPoint.subLevelID() != null) {
            pointTag.putUUID("SubLevelID", trackingPoint.subLevelID());
         }

         loginPointsTag.put(entry.getKey().toString(), pointTag);
      }

      compoundTag.put("tracking_points", loginPointsTag);
      return compoundTag;
   }

   @Nullable
   public UUID generateTrackingPoint(ServerPlayer player) {
      ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getTrackingSubLevel(player);
      return this.generateTrackingPoint(player, subLevel);
   }

   @Nullable
   public UUID generateTrackingPoint(ServerPlayer player, ServerSubLevel subLevel) {
      if (subLevel == null) {
         return null;
      } else {
         GlobalSavedSubLevelPointer pointer = subLevel.getLastSerializationPointer();
         Vector3d globalPlaceholderPosition = pointer == null ? JOMLConversion.toJOML(player.position()) : null;
         TrackingPoint trackingPoint = new TrackingPoint(
            true,
            subLevel.getUniqueId(),
            pointer,
            subLevel.logicalPose().transformPositionInverse(JOMLConversion.toJOML(player.position())),
            globalPlaceholderPosition
         );
         UUID uuid = player.getUUID();
         this.trackingPoints.put(uuid, trackingPoint);
         this.setDirty(true);
         return uuid;
      }
   }

   @Nullable
   public UUID generateTrackingPoint(Vec3 pos, ServerSubLevel subLevel) {
      if (subLevel == null) {
         return null;
      } else {
         Pose3d pose = subLevel.logicalPose();
         GlobalSavedSubLevelPointer pointer = subLevel.getLastSerializationPointer();
         Vector3d globalPlaceholderPosition = pointer == null ? pose.transformPosition(JOMLConversion.toJOML(pos)) : null;
         TrackingPoint trackingPoint = new TrackingPoint(true, subLevel.getUniqueId(), pointer, JOMLConversion.toJOML(pos), globalPlaceholderPosition);
         UUID uuid = UUID.randomUUID();
         this.trackingPoints.put(uuid, trackingPoint);
         this.setDirty(true);
         return uuid;
      }
   }

   public SubLevelTrackingPointSavedData.TakenLoginPoint take(UUID uuid, boolean remove) {
      TrackingPoint point = remove ? this.trackingPoints.remove(uuid) : this.trackingPoints.get(uuid);
      if (remove) {
         this.setDirty(true);
      }

      if (point == null) {
         return null;
      } else if (point.inSubLevel()) {
         SubLevel existingSubLevel = Sable.HELPER.getContaining(this.level, point.point());
         if (existingSubLevel != null) {
            return new SubLevelTrackingPointSavedData.TakenLoginPoint(
               existingSubLevel.logicalPose().transformPosition(new Vector3d(point.point())), existingSubLevel.getUniqueId(), new Vector3d(point.point())
            );
         } else {
            ServerSubLevelContainer container = ServerSubLevelContainer.getContainer(this.level);
            SubLevelHoldingChunkMap holdingMap = container.getHoldingChunkMap();
            if (point.subLevelID() != null) {
               HoldingSubLevel holdingSubLevel = holdingMap.getHoldingSubLevel(point.subLevelID());
               if (holdingSubLevel != null) {
                  SubLevelData data = holdingSubLevel.data();
                  return new SubLevelTrackingPointSavedData.TakenLoginPoint(
                     data.pose().transformPosition(new Vector3d(point.point())), data.uuid(), new Vector3d(point.point())
                  );
               }
            }

            GlobalSavedSubLevelPointer pointer = point.lastSavedSubLevelPointer();
            if (pointer != null) {
               Sable.LOGGER.info("Player logged in with tracking point in non-loaded sub-level. Attempting to load.");
               SubLevelData data = holdingMap.getStorage().attemptLoadSubLevel(pointer.chunkPos(), pointer.local());
               if (data == null) {
                  Sable.LOGGER.warn("Failed to load sub-level at pointer {} for tracking point", point.lastSavedSubLevelPointer());
                  return null;
               } else {
                  return new SubLevelTrackingPointSavedData.TakenLoginPoint(
                     data.pose().transformPosition(new Vector3d(point.point())), data.uuid(), new Vector3d(point.point())
                  );
               }
            } else {
               Sable.LOGGER
                  .warn("Player logged in with tracking point in non-loaded sub-level without a pointer toward one. Placing them at their global placeholder.");
               Vector3d placeholder = point.globalPlaceholderPosition();
               if (placeholder != null) {
                  return new SubLevelTrackingPointSavedData.TakenLoginPoint(placeholder, null, null);
               } else {
                  Sable.LOGGER
                     .error(
                        "Player logged in with tracking point in non-loaded sub-level without a pointer toward one, and without a placeholder. Something has gone wrong."
                     );
                  return null;
               }
            }
         }
      } else {
         return new SubLevelTrackingPointSavedData.TakenLoginPoint(point.point(), null, null);
      }
   }

   public Iterable<Entry<UUID, TrackingPoint>> getAllTrackingPoints() {
      return new ObjectArrayList(this.trackingPoints.entrySet());
   }

   public Iterable<Pair<UUID, TrackingPoint>> getAllTrackingPoints(BoundingBox3ic bounds) {
      ObjectArrayList<Pair<UUID, TrackingPoint>> keys = new ObjectArrayList();

      for (Entry<UUID, TrackingPoint> entry : this.trackingPoints.entrySet()) {
         Vector3d point = entry.getValue().point();
         if (bounds.contains(point)) {
            keys.add(Pair.of(entry.getKey(), entry.getValue()));
         }
      }

      return keys;
   }

   public void setTrackingPoint(UUID key, TrackingPoint point) {
      this.trackingPoints.put(key, point);
      this.setDirty(true);
   }

   public void removeTrackingPoint(UUID key) {
      this.trackingPoints.remove(key);
      this.setDirty(true);
   }

   @Nullable
   public TrackingPoint getTrackingPoint(UUID uuid) {
      return this.trackingPoints.get(uuid);
   }

   public static record TakenLoginPoint(Vector3dc position, @Nullable UUID subLevelId, @Nullable Vector3d localAnchor) {
   }
}
