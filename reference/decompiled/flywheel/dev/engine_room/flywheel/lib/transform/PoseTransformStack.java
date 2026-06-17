package dev.engine_room.flywheel.lib.transform;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public final class PoseTransformStack implements TransformStack<PoseTransformStack> {
   private final PoseStack stack;

   @Internal
   public PoseTransformStack(PoseStack stack) {
      this.stack = stack;
   }

   public PoseTransformStack pushPose() {
      this.stack.pushPose();
      return this;
   }

   public PoseTransformStack popPose() {
      this.stack.popPose();
      return this;
   }

   public PoseTransformStack mulPose(Matrix4fc pose) {
      this.stack.last().pose().mul(pose);
      return this;
   }

   public PoseTransformStack mulNormal(Matrix3fc normal) {
      this.stack.last().normal().mul(normal);
      return this;
   }

   public PoseTransformStack rotateAround(Quaternionfc quaternion, float x, float y, float z) {
      Pose pose = this.stack.last();
      pose.pose().rotateAround(quaternion, x, y, z);
      pose.normal().rotate(quaternion);
      return this;
   }

   public PoseTransformStack translate(float x, float y, float z) {
      this.stack.translate(x, y, z);
      return this;
   }

   public PoseTransformStack rotate(Quaternionfc quaternion) {
      Pose pose = this.stack.last();
      pose.pose().rotate(quaternion);
      pose.normal().rotate(quaternion);
      return this;
   }

   public PoseTransformStack scale(float factorX, float factorY, float factorZ) {
      this.stack.scale(factorX, factorY, factorZ);
      return this;
   }

   public PoseStack unwrap() {
      return this.stack;
   }
}
