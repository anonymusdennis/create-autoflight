package dev.ryanhcode.sable.companion;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import java.util.UUID;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public interface SubLevelAccess {
   @Contract(
      pure = true
   )
   Pose3dc logicalPose();

   @Contract(
      pure = true
   )
   Pose3dc lastPose();

   @Contract(
      pure = true
   )
   BoundingBox3dc boundingBox();

   @Contract(
      pure = true
   )
   UUID getUniqueId();

   @Contract(
      pure = true
   )
   @Nullable
   String getName();
}
