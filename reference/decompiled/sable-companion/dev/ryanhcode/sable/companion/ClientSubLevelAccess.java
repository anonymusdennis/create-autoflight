package dev.ryanhcode.sable.companion;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import org.jetbrains.annotations.Contract;

public interface ClientSubLevelAccess extends SubLevelAccess {
   @Contract(
      pure = true
   )
   Pose3dc renderPose();

   @Contract(
      pure = true
   )
   Pose3dc renderPose(float var1);
}
