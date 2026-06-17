package dev.ryanhcode.sable.api.physics.object.box;

import dev.ryanhcode.sable.companion.math.Pose3d;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface BoxHandle {
   @OverrideOnly
   void readPose(Pose3d var1);

   void remove();

   void wakeUp();

   int getRuntimeId();
}
