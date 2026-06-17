package dev.engine_room.flywheel.lib.instance;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.transform.Transform;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;

public class PosedInstance extends ColoredLitOverlayInstance implements Transform<PosedInstance> {
   public final Matrix4f pose = new Matrix4f();
   public final Matrix3f normal = new Matrix3f();

   public PosedInstance(InstanceType<? extends PosedInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public PosedInstance mulPose(Matrix4fc pose) {
      this.pose.mul(pose);
      return this;
   }

   public PosedInstance mulNormal(Matrix3fc normal) {
      this.normal.mul(normal);
      return this;
   }

   public PosedInstance rotateAround(Quaternionfc quaternion, float x, float y, float z) {
      this.pose.rotateAround(quaternion, x, y, z);
      this.normal.rotate(quaternion);
      return this;
   }

   public PosedInstance translate(float x, float y, float z) {
      this.pose.translate(x, y, z);
      return this;
   }

   public PosedInstance rotate(Quaternionfc quaternion) {
      this.pose.rotate(quaternion);
      this.normal.rotate(quaternion);
      return this;
   }

   public PosedInstance scale(float x, float y, float z) {
      this.pose.scale(x, y, z);
      if (x == y && y == z) {
         if (x < 0.0F) {
            this.normal.scale(-1.0F);
         }

         return this;
      } else {
         float invX = 1.0F / x;
         float invY = 1.0F / y;
         float invZ = 1.0F / z;
         float f = Mth.fastInvCubeRoot(Math.abs(invX * invY * invZ));
         this.normal.scale(f * invX, f * invY, f * invZ);
         return this;
      }
   }

   public PosedInstance setTransform(Matrix4fc pose, Matrix3fc normal) {
      this.pose.set(pose);
      this.normal.set(normal);
      return this;
   }

   public PosedInstance setTransform(Pose pose) {
      this.pose.set(pose.pose());
      this.normal.set(pose.normal());
      return this;
   }

   public PosedInstance setTransform(PoseStack stack) {
      return this.setTransform(stack.last());
   }

   public PosedInstance setIdentityTransform() {
      this.pose.identity();
      this.normal.identity();
      return this;
   }

   public PosedInstance setZeroTransform() {
      this.pose.zero();
      this.normal.zero();
      return this;
   }
}
