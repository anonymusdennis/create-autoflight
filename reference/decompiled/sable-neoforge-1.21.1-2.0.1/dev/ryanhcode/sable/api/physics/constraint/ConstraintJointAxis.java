package dev.ryanhcode.sable.api.physics.constraint;

public enum ConstraintJointAxis {
   LINEAR_X,
   LINEAR_Y,
   LINEAR_Z,
   ANGULAR_X,
   ANGULAR_Y,
   ANGULAR_Z;

   public static final ConstraintJointAxis[] ALL = values();
   public static final ConstraintJointAxis[] LINEAR = new ConstraintJointAxis[]{LINEAR_X, LINEAR_Y, LINEAR_Z};
   public static final ConstraintJointAxis[] ANGULAR = new ConstraintJointAxis[]{ANGULAR_X, ANGULAR_Y, ANGULAR_Z};
}
