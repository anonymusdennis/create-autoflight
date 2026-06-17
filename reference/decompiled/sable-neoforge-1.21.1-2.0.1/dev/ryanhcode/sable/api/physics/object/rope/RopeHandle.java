package dev.ryanhcode.sable.api.physics.object.rope;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface RopeHandle {
   @OverrideOnly
   void readPose(List<Vector3d> var1);

   void remove();

   void setFirstSegmentLength(double var1);

   void removeFirstPoint();

   void addPoint(Vector3dc var1);

   void setAttachment(RopeHandle.AttachmentPoint var1, Vector3dc var2, ServerSubLevel var3);

   void wakeUp();

   public static enum AttachmentPoint {
      START,
      END;
   }
}
