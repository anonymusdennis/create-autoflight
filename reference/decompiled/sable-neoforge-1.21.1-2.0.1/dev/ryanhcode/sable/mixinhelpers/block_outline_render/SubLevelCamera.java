package dev.ryanhcode.sable.mixinhelpers.block_outline_render;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.minecraft.client.Camera;
import net.minecraft.client.Camera.NearPlane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Internal
public class SubLevelCamera extends Camera {
   private Camera renderCamera;
   private final Quaterniond inverseOrientation = new Quaterniond();
   private final Quaternionf inverseOrientationf = new Quaternionf();
   private final Vector3f rotationYXZ = new Vector3f();
   private final MutableBlockPos blockPosition = new MutableBlockPos();
   private Vec3 pos = Vec3.ZERO;

   public void setCamera(Camera renderCamera) {
      this.renderCamera = renderCamera;
   }

   public void setPose(@Nullable Pose3dc pose) {
      if (pose != null) {
         Vec3 pos = pose.transformPositionInverse(this.renderCamera.getPosition());
         Quaternionf rotation = this.rotation();
         this.renderCamera.rotation().mul(this.inverseOrientationf.set(pose.orientation().invert(this.inverseOrientation)), rotation);
         this.blockPosition.set(pos.x, pos.y, pos.z);
         this.pos = pos;
         rotation.getEulerAnglesYXZ(this.rotationYXZ);
         this.getLookVector().set(0.0F, 0.0F, -1.0F).rotate(rotation);
         this.getUpVector().set(0.0F, 1.0F, 0.0F).rotate(rotation);
         this.getLeftVector().set(-1.0F, 0.0F, 0.0F).rotate(rotation);
      } else {
         this.pos = this.renderCamera.getPosition();
         this.blockPosition.set(this.pos.x, this.pos.y, this.pos.z);
         this.rotationYXZ.set(this.renderCamera.getXRot(), this.renderCamera.getYRot(), 0.0F);
         Quaternionf rotation = this.rotation();
         rotation.set(this.renderCamera.rotation());
         this.getLookVector().set(0.0F, 0.0F, -1.0F).rotate(rotation);
         this.getUpVector().set(0.0F, 1.0F, 0.0F).rotate(rotation);
         this.getLeftVector().set(-1.0F, 0.0F, 0.0F).rotate(rotation);
      }
   }

   public void clear() {
      this.renderCamera = null;
      this.pos = Vec3.ZERO;
   }

   @NotNull
   public Vec3 getPosition() {
      return this.pos;
   }

   @NotNull
   public BlockPos getBlockPosition() {
      return this.blockPosition;
   }

   public float getXRot() {
      return (float)((180.0 / Math.PI) * (double)(-this.rotationYXZ.x));
   }

   public float getYRot() {
      return (float)((180.0 / Math.PI) * (double)(-this.rotationYXZ.y) + 180.0);
   }

   @NotNull
   public Entity getEntity() {
      return this.renderCamera.getEntity();
   }

   public boolean isInitialized() {
      return this.renderCamera.isInitialized();
   }

   public boolean isDetached() {
      return this.renderCamera.isDetached();
   }

   @NotNull
   public NearPlane getNearPlane() {
      return this.renderCamera.getNearPlane();
   }

   @NotNull
   public FogType getFluidInCamera() {
      return this.renderCamera.getFluidInCamera();
   }

   public void reset() {
      this.renderCamera.reset();
   }

   public float getPartialTickTime() {
      return this.renderCamera.getPartialTickTime();
   }

   public Camera getRenderCamera() {
      return this.renderCamera;
   }
}
