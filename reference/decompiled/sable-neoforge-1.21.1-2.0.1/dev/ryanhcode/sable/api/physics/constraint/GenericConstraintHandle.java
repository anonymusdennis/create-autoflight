package dev.ryanhcode.sable.api.physics.constraint;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public non-sealed interface GenericConstraintHandle extends PhysicsConstraintHandle {
   void setFrame1(Vector3dc var1, Quaterniondc var2);

   void setFrame2(Vector3dc var1, Quaterniondc var2);

   void setLimit(ConstraintJointAxis var1, double var2, double var4);

   void lockAxes(ConstraintJointAxis... var1);
}
