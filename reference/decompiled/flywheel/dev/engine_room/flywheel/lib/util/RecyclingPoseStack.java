package dev.engine_room.flywheel.lib.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.lib.internal.FlwLibLink;
import java.util.ArrayDeque;
import java.util.Deque;

public class RecyclingPoseStack extends PoseStack {
   private final Deque<Pose> recycleBin = new ArrayDeque<>();

   public void pushPose() {
      if (this.recycleBin.isEmpty()) {
         super.pushPose();
      } else {
         Pose last = this.last();
         Pose recycle = this.recycleBin.removeLast();
         recycle.pose().set(last.pose());
         recycle.normal().set(last.normal());
         FlwLibLink.INSTANCE.getPoseStack(this).addLast(recycle);
      }
   }

   public void popPose() {
      this.recycleBin.addLast(FlwLibLink.INSTANCE.getPoseStack(this).removeLast());
   }
}
