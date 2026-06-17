package dev.ryanhcode.sable.api.physics.mass;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface MassData {
   double getMass();

   double getInverseMass();

   Matrix3dc getInertiaTensor();

   Matrix3dc getInverseInertiaTensor();

   @Nullable
   Vector3dc getCenterOfMass();

   default boolean isInvalid() {
      return this.getMass() <= 0.0;
   }

   default double getInverseNormalMass(Vector3dc position, Vector3dc direction) {
      Vector3d comLocalPos = position.sub(this.getCenterOfMass(), new Vector3d());
      Vector3d normalizedDirection = direction.normalize(new Vector3d());
      Vector3d cross = comLocalPos.cross(normalizedDirection, new Vector3d());
      return cross.dot(this.getInverseInertiaTensor().transform(cross, new Vector3d())) + this.getInverseMass();
   }
}
