package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.lib.transform.Rotate;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public class OrientedInstance extends ColoredLitOverlayInstance implements Rotate<OrientedInstance> {
   public float posX;
   public float posY;
   public float posZ;
   public float pivotX = 0.5F;
   public float pivotY = 0.5F;
   public float pivotZ = 0.5F;
   public final Quaternionf rotation = new Quaternionf();

   public OrientedInstance(InstanceType<? extends OrientedInstance> type, InstanceHandle handle) {
      super(type, handle);
   }

   public OrientedInstance position(float x, float y, float z) {
      this.posX = x;
      this.posY = y;
      this.posZ = z;
      return this;
   }

   public OrientedInstance position(Vector3fc pos) {
      return this.position(pos.x(), pos.y(), pos.z());
   }

   public OrientedInstance position(Vec3i pos) {
      return this.position((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
   }

   public OrientedInstance position(Vec3 pos) {
      return this.position((float)pos.x(), (float)pos.y(), (float)pos.z());
   }

   public OrientedInstance zeroPosition() {
      return this.position(0.0F, 0.0F, 0.0F);
   }

   public OrientedInstance translatePosition(float x, float y, float z) {
      this.posX += x;
      this.posY += y;
      this.posZ += z;
      return this;
   }

   public OrientedInstance pivot(float x, float y, float z) {
      this.pivotX = x;
      this.pivotY = y;
      this.pivotZ = z;
      return this;
   }

   public OrientedInstance pivot(Vector3fc pos) {
      return this.pivot(pos.x(), pos.y(), pos.z());
   }

   public OrientedInstance pivot(Vec3i pos) {
      return this.pivot((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
   }

   public OrientedInstance pivot(Vec3 pos) {
      return this.pivot((float)pos.x(), (float)pos.y(), (float)pos.z());
   }

   public OrientedInstance centerPivot() {
      return this.pivot(0.5F, 0.5F, 0.5F);
   }

   public OrientedInstance translatePivot(float x, float y, float z) {
      this.pivotX += x;
      this.pivotY += y;
      this.pivotZ += z;
      return this;
   }

   public OrientedInstance rotation(Quaternionfc q) {
      this.rotation.set(q);
      return this;
   }

   public OrientedInstance rotation(float x, float y, float z, float w) {
      this.rotation.set(x, y, z, w);
      return this;
   }

   public OrientedInstance identityRotation() {
      this.rotation.identity();
      return this;
   }

   public OrientedInstance rotate(Quaternionfc quaternion) {
      this.rotation.mul(quaternion);
      return this;
   }
}
