package dev.engine_room.flywheel.lib.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

public interface Transform<Self extends Transform<Self>> extends Affine<Self> {
   Self mulPose(Matrix4fc var1);

   Self mulNormal(Matrix3fc var1);

   default Self transform(Matrix4fc pose, Matrix3fc normal) {
      return this.mulPose(pose).mulNormal(normal);
   }

   default Self transform(Pose pose) {
      return this.transform(pose.pose(), pose.normal());
   }

   default Self transform(PoseStack stack) {
      return this.transform(stack.last());
   }
}
