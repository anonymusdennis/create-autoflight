package dev.ryanhcode.sable.companion.math;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public sealed interface Pose3dc permits Pose3d {
   @Contract(
      pure = true
   )
   Vector3dc position();

   @Contract(
      pure = true
   )
   Quaterniondc orientation();

   @Contract(
      pure = true
   )
   Vector3dc rotationPoint();

   @Contract(
      pure = true
   )
   Vector3dc scale();

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d transformPosition(Vector3dc local, Vector3d dest) {
      return this.orientation().transform(local.sub(this.rotationPoint(), dest).mul(this.scale())).add(this.position());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   default Vec3 transformPosition(Vec3 local) {
      return JOMLConversion.toMojang(this.transformPosition(JOMLConversion.toJOML(local)));
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   default Vec3 transformPositionInverse(Vec3 global) {
      return JOMLConversion.toMojang(this.transformPositionInverse(JOMLConversion.toJOML(global)));
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d transformPositionInverse(Vector3dc global, Vector3d dest) {
      Vector3dc s = this.scale();
      return this.orientation().transformInverse(global.sub(this.position(), dest)).mul(1.0 / s.x(), 1.0 / s.y(), 1.0 / s.z()).add(this.rotationPoint());
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d transformNormal(Vector3dc local, Vector3d dest) {
      return this.orientation().transform(local.mul(this.scale(), dest));
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d transformNormalInverse(Vector3dc global, Vector3d dest) {
      Vector3dc s = this.scale();
      return this.orientation().transformInverse(global, dest).mul(1.0 / s.x(), 1.0 / s.y(), 1.0 / s.z());
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3d transformPosition(Vector3d local) {
      return this.transformPosition(local, local);
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3d transformPositionInverse(Vector3d global) {
      return this.transformPositionInverse(global, global);
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3d transformNormal(Vector3d local) {
      return this.transformNormal(local, local);
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3d transformNormalInverse(Vector3d global) {
      return this.transformNormalInverse(global, global);
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   default Vec3 transformNormal(Vec3 local) {
      return JOMLConversion.toMojang(this.transformNormal(JOMLConversion.toJOML(local)));
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   default Vec3 transformNormalInverse(Vec3 global) {
      return JOMLConversion.toMojang(this.transformNormalInverse(JOMLConversion.toJOML(global)));
   }

   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   default Pose3d lerp(Pose3dc pose, double frac, Pose3d dest) {
      this.position().lerp(pose.position(), frac, dest.position());
      this.orientation().nlerp(pose.orientation(), frac, dest.orientation());
      this.rotationPoint().lerp(pose.rotationPoint(), frac, dest.rotationPoint());
      this.scale().lerp(pose.scale(), frac, dest.scale());
      return dest;
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Matrix4d bakeIntoMatrix(Matrix4d dest) {
      Vector3dc rotationPoint = this.rotationPoint();
      return dest.identity()
         .translate(this.position())
         .rotate(this.orientation())
         .scale(this.scale())
         .translate(-rotationPoint.x(), -rotationPoint.y(), -rotationPoint.z());
   }

   @Contract(
      pure = true
   )
   default boolean withinTolerance(Pose3d pose3d, double distanceTolerance, double angularTolerance) {
      return this.position().distanceSquared(pose3d.position()) <= distanceTolerance * distanceTolerance
         && this.rotationPoint().distanceSquared(pose3d.rotationPoint()) <= distanceTolerance * distanceTolerance
         && this.orientation().div(pose3d.orientation(), new Quaterniond()).angle() <= angularTolerance;
   }
}
