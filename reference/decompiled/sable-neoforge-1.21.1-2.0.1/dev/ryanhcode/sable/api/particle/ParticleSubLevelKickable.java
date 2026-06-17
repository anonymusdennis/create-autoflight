package dev.ryanhcode.sable.api.particle;

import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import org.joml.Vector3dc;

public interface ParticleSubLevelKickable {
   default boolean sable$shouldCareAboutIntersectingSubLevels() {
      return true;
   }

   boolean sable$shouldKickFromTracking();

   boolean sable$shouldCollideWithTrackingSubLevel();

   default Vector3dc sable$getUpDirection() {
      return OrientedBoundingBox3d.UP;
   }
}
