package dev.ryanhcode.sable.sublevel.tracking_points;

import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelTrackingPointObserver implements SubLevelObserver {
   private final ServerLevel serverLevel;

   public SubLevelTrackingPointObserver(ServerLevel serverLevel) {
      this.serverLevel = serverLevel;
   }

   @Override
   public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
      if (reason == SubLevelRemovalReason.REMOVED) {
         SubLevelTrackingPointSavedData data = this.getTrackingPointData();

         for (UUID uuid : getTrackingPoints((ServerSubLevel)subLevel, data)) {
            TrackingPoint trackingPoint = data.getTrackingPoint(uuid);
            if (trackingPoint != null) {
               Vector3dc point = subLevel.logicalPose().transformPosition(trackingPoint.point());
               data.setTrackingPoint(uuid, new TrackingPoint(false, null, null, new Vector3d(point), null));
            }
         }
      }
   }

   @NotNull
   private static List<UUID> getTrackingPoints(ServerSubLevel subLevel, SubLevelTrackingPointSavedData data) {
      List<UUID> toProject = new ObjectArrayList();

      for (Entry<UUID, TrackingPoint> entry : data.getAllTrackingPoints()) {
         TrackingPoint trackingPoint = entry.getValue();
         GlobalSavedSubLevelPointer pointer = trackingPoint.lastSavedSubLevelPointer();
         if (trackingPoint.inSubLevel() && pointer != null && pointer.equals(subLevel.getLastSerializationPointer())) {
            toProject.add(entry.getKey());
         }
      }

      return toProject;
   }

   private SubLevelTrackingPointSavedData getTrackingPointData() {
      return SubLevelTrackingPointSavedData.getOrLoad(this.serverLevel);
   }
}
