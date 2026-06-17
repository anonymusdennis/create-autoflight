package dev.engine_room.flywheel.lib.instance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.transform.Affine;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public class TransformedInstance extends ColoredLitOverlayInstance implements Affine<TransformedInstance> {
   public final Matrix4f pose = new Matrix4f();

   public TransformedInstance(InstanceType<? extends TransformedInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public TransformedInstance translate(float x, float y, float z) {
      this.pose.translate(x, y, z);
      return this;
   }

   public TransformedInstance rotate(Quaternionfc quaternion) {
      this.pose.rotate(quaternion);
      return this;
   }

   public TransformedInstance scale(float x, float y, float z) {
      this.pose.scale(x, y, z);
      return this;
   }

   public TransformedInstance mul(Matrix4fc other) {
      this.pose.mul(other);
      return this;
   }

   public TransformedInstance mul(Pose other) {
      return this.mul(other.pose());
   }

   public TransformedInstance mul(PoseStack stack) {
      return this.mul(stack.last());
   }

   public TransformedInstance setTransform(Matrix4fc pose) {
      this.pose.set(pose);
      return this;
   }

   public TransformedInstance setTransform(Pose pose) {
      this.pose.set(pose.pose());
      return this;
   }

   public TransformedInstance setTransform(PoseStack stack) {
      return this.setTransform(stack.last());
   }

   public TransformedInstance setIdentityTransform() {
      this.pose.identity();
      return this;
   }

   public TransformedInstance setZeroTransform() {
      this.pose.zero();
      return this;
   }

   public TransformedInstance rotateAround(Quaternionfc quaternion, float x, float y, float z) {
      this.pose.rotateAround(quaternion, x, y, z);
      return this;
   }

   public TransformedInstance rotateCentered(float radians, float axisX, float axisY, float axisZ) {
      this.pose.translate(0.5F, 0.5F, 0.5F).rotate(radians, axisX, axisY, axisZ).translate(-0.5F, -0.5F, -0.5F);
      return this;
   }

   public TransformedInstance rotateXCentered(float radians) {
      this.pose.translate(0.5F, 0.5F, 0.5F).rotateX(radians).translate(-0.5F, -0.5F, -0.5F);
      return this;
   }

   public TransformedInstance rotateYCentered(float radians) {
      this.pose.translate(0.5F, 0.5F, 0.5F).rotateY(radians).translate(-0.5F, -0.5F, -0.5F);
      return this;
   }

   public TransformedInstance rotateZCentered(float radians) {
      this.pose.translate(0.5F, 0.5F, 0.5F).rotateZ(radians).translate(-0.5F, -0.5F, -0.5F);
      return this;
   }

   public TransformedInstance rotate(float radians, float axisX, float axisY, float axisZ) {
      this.pose.rotate(radians, axisX, axisY, axisZ);
      return this;
   }

   public TransformedInstance rotate(AxisAngle4f axisAngle) {
      this.pose.rotate(axisAngle);
      return this;
   }

   public TransformedInstance rotateX(float radians) {
      this.pose.rotateX(radians);
      return this;
   }

   public TransformedInstance rotateY(float radians) {
      this.pose.rotateY(radians);
      return this;
   }

   public TransformedInstance rotateZ(float radians) {
      this.pose.rotateZ(radians);
      return this;
   }
}
