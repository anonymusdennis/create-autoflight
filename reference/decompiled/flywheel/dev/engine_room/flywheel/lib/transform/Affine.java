package dev.engine_room.flywheel.lib.transform;

import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;

public interface Affine<Self extends Affine<Self>> extends Translate<Self>, Rotate<Self>, Scale<Self> {
   default Self rotateAround(Quaternionfc quaternion, float x, float y, float z) {
      return this.translate(x, y, z).rotate(quaternion).translateBack(x, y, z);
   }

   default Self rotateAround(Quaternionfc quaternion, Vector3fc vec) {
      return this.rotateAround(quaternion, vec.x(), vec.y(), vec.z());
   }

   default Self rotateCentered(Quaternionfc q) {
      return this.rotateAround(q, 0.5F, 0.5F, 0.5F);
   }

   default Self rotateCentered(float radians, float axisX, float axisY, float axisZ) {
      return radians == 0.0F ? this.self() : this.rotateCentered(new Quaternionf().setAngleAxis(radians, axisX, axisY, axisZ));
   }

   default Self rotateCentered(float radians, Axis axis) {
      return radians == 0.0F ? this.self() : this.rotateCentered(axis.rotation(radians));
   }

   default Self rotateCentered(float radians, Vector3fc axis) {
      return this.rotateCentered(radians, axis.x(), axis.y(), axis.z());
   }

   default Self rotateCentered(float radians, net.minecraft.core.Direction.Axis axis) {
      return this.rotateCentered(radians, Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE));
   }

   default Self rotateCentered(float radians, Direction axis) {
      return this.rotateCentered(radians, (float)axis.getStepX(), (float)axis.getStepY(), (float)axis.getStepZ());
   }

   default Self rotateCenteredDegrees(float degrees, float axisX, float axisY, float axisZ) {
      return this.rotateCentered((float) (Math.PI / 180.0) * degrees, axisX, axisY, axisZ);
   }

   default Self rotateCenteredDegrees(float degrees, Axis axis) {
      return this.rotateCentered((float) (Math.PI / 180.0) * degrees, axis);
   }

   default Self rotateCenteredDegrees(float degrees, Vector3fc axis) {
      return this.rotateCentered((float) (Math.PI / 180.0) * degrees, axis);
   }

   default Self rotateCenteredDegrees(float degrees, Direction axis) {
      return this.rotateCentered((float) (Math.PI / 180.0) * degrees, axis);
   }

   default Self rotateCenteredDegrees(float degrees, net.minecraft.core.Direction.Axis axis) {
      return this.rotateCentered((float) (Math.PI / 180.0) * degrees, axis);
   }

   default Self rotateXCentered(float radians) {
      return this.rotateCentered(radians, Axis.XP);
   }

   default Self rotateYCentered(float radians) {
      return this.rotateCentered(radians, Axis.YP);
   }

   default Self rotateZCentered(float radians) {
      return this.rotateCentered(radians, Axis.ZP);
   }

   default Self rotateXCenteredDegrees(float degrees) {
      return this.rotateXCentered((float) (Math.PI / 180.0) * degrees);
   }

   default Self rotateYCenteredDegrees(float degrees) {
      return this.rotateYCentered((float) (Math.PI / 180.0) * degrees);
   }

   default Self rotateZCenteredDegrees(float degrees) {
      return this.rotateZCentered((float) (Math.PI / 180.0) * degrees);
   }
}
