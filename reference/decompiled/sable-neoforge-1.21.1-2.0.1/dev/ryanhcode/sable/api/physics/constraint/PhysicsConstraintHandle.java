package dev.ryanhcode.sable.api.physics.constraint;

import org.joml.Vector3d;

public sealed interface PhysicsConstraintHandle permits FreeConstraintHandle, FixedConstraintHandle, RotaryConstraintHandle, GenericConstraintHandle {
   void getJointImpulses(Vector3d var1, Vector3d var2);

   void setContactsEnabled(boolean var1);

   void setMotor(ConstraintJointAxis var1, double var2, double var4, double var6, boolean var8, double var9);

   void remove();

   boolean isValid();
}
