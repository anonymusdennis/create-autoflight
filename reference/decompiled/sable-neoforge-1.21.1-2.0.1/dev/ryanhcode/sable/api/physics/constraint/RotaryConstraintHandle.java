package dev.ryanhcode.sable.api.physics.constraint;

public non-sealed interface RotaryConstraintHandle extends PhysicsConstraintHandle {
   ConstraintJointAxis DEFAULT_AXIS = ConstraintJointAxis.ANGULAR_X;

   @Deprecated(
      forRemoval = true
   )
   default void setServoCoefficients(double angle, double stiffness, double damping) {
      this.setMotor(DEFAULT_AXIS, angle, stiffness, damping, false, 0.0);
   }
}
