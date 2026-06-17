package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SableMathUtils {
   private static final Vector3d temp = new Vector3d();
   private static final Quaterniondc[] ALL_QUATS = new Quaterniondc[]{
      new Quaterniond(0.0, 0.0, 0.0, 1.0),
      new Quaterniond(1.0, 0.0, 0.0, 0.0),
      new Quaterniond(0.0, 1.0, 0.0, 0.0),
      new Quaterniond(0.0, 0.0, 1.0, 0.0),
      new Quaterniond(1.0, 0.0, 0.0, 1.0).normalize(),
      new Quaterniond(0.0, 1.0, 0.0, 1.0).normalize(),
      new Quaterniond(0.0, 0.0, 1.0, 1.0).normalize(),
      new Quaterniond(0.0, 1.0, 1.0, 0.0).normalize(),
      new Quaterniond(1.0, 0.0, 1.0, 0.0).normalize(),
      new Quaterniond(1.0, 1.0, 0.0, 0.0).normalize(),
      new Quaterniond(1.0, 1.0, 1.0, 1.0).normalize()
   };
   static final Quaterniond difference = new Quaterniond();

   public static Matrix3d setOuterProduct(Vector3dc u, Vector3dc v, Matrix3d target) {
      target.m00 = u.x() * v.x();
      target.m01 = u.y() * v.x();
      target.m02 = u.z() * v.x();
      target.m10 = u.x() * v.y();
      target.m11 = u.y() * v.y();
      target.m12 = u.z() * v.y();
      target.m20 = u.x() * v.z();
      target.m21 = u.y() * v.z();
      target.m22 = u.z() * v.z();
      return target;
   }

   public static Matrix3d setOuterProduct(Vector3dc u, Vector3dc v, double scale, Matrix3d target) {
      target.m00 = u.x() * v.x() * scale;
      target.m01 = u.y() * v.x() * scale;
      target.m02 = u.z() * v.x() * scale;
      target.m10 = u.x() * v.y() * scale;
      target.m11 = u.y() * v.y() * scale;
      target.m12 = u.z() * v.y() * scale;
      target.m20 = u.x() * v.z() * scale;
      target.m21 = u.y() * v.z() * scale;
      target.m22 = u.z() * v.z() * scale;
      return target;
   }

   public static Matrix3d addOuterProduct(Vector3dc u, Vector3dc v, Matrix3d target) {
      target.m00 = target.m00 + u.x() * v.x();
      target.m01 = target.m01 + u.y() * v.x();
      target.m02 = target.m02 + u.z() * v.x();
      target.m10 = target.m10 + u.x() * v.y();
      target.m11 = target.m11 + u.y() * v.y();
      target.m12 = target.m12 + u.z() * v.y();
      target.m20 = target.m20 + u.x() * v.z();
      target.m21 = target.m21 + u.y() * v.z();
      target.m22 = target.m22 + u.z() * v.z();
      return target;
   }

   public static Matrix3d fmaOuterProduct(Vector3dc u, Vector3dc v, double scale, Matrix3d target) {
      target.m00 = target.m00 + u.x() * v.x() * scale;
      target.m01 = target.m01 + u.y() * v.x() * scale;
      target.m02 = target.m02 + u.z() * v.x() * scale;
      target.m10 = target.m10 + u.x() * v.y() * scale;
      target.m11 = target.m11 + u.y() * v.y() * scale;
      target.m12 = target.m12 + u.z() * v.y() * scale;
      target.m20 = target.m20 + u.x() * v.z() * scale;
      target.m21 = target.m21 + u.y() * v.z() * scale;
      target.m22 = target.m22 + u.z() * v.z() * scale;
      return target;
   }

   public static Matrix3d fmaInertiaTensor(Vector3dc u, double scale, Matrix3d target) {
      target.m00 = target.m00 + (u.y() * u.y() + u.z() * u.z()) * scale;
      target.m01 = target.m01 - u.y() * u.x() * scale;
      target.m02 = target.m02 - u.z() * u.x() * scale;
      target.m10 = target.m10 - u.x() * u.y() * scale;
      target.m11 = target.m11 + (u.z() * u.z() + u.x() * u.x()) * scale;
      target.m12 = target.m12 - u.z() * u.y() * scale;
      target.m20 = target.m20 - u.x() * u.z() * scale;
      target.m21 = target.m21 - u.y() * u.z() * scale;
      target.m22 = target.m22 + (u.x() * u.x() + u.y() * u.y()) * scale;
      return target;
   }

   public static double multiplyInnerProduct(Vector3dc u, Matrix3dc A, Vector3dc v) {
      A.transform(v, temp);
      return temp.dot(u);
   }

   public static Vector3d getAngularVelocity(Quaterniondc lastOrientation, Quaterniondc orientation, Vector3d dest) {
      orientation.difference(lastOrientation, difference).conjugate();
      if (difference.w < 0.0) {
         difference.mul(-1.0);
      }

      Vector3d angularVelocity = dest.set(difference.x, difference.y, difference.z);
      if (angularVelocity.lengthSquared() <= 1.0E-15) {
         angularVelocity.mul(2.0 / difference.w);
      } else {
         angularVelocity.normalize().mul(2.0 * Math.safeAcos(difference.w));
      }

      return dest;
   }

   public static Quaterniond clampQuaternionToGrid(Quaterniondc q, Iterable<Quaterniondc> gridQuats, Quaterniond dest) {
      int signX = q.x() < 0.0 ? -1 : 1;
      int signY = q.y() < 0.0 ? -1 : 1;
      int signZ = q.z() < 0.0 ? -1 : 1;
      int signW = q.w() < 0.0 ? -1 : 1;
      dest.set(q);
      dest.x *= (double)(-signX);
      dest.y *= (double)(-signY);
      dest.z *= (double)(-signZ);
      dest.w *= (double)(-signW);
      Quaterniond temp = new Quaterniond();
      Quaterniond best = new Quaterniond();
      double distance = 10.0;

      for (Quaterniondc gq : gridQuats) {
         double currentDist = dest.add(gq, temp).lengthSquared();
         if (currentDist < distance) {
            distance = currentDist;
            best.set(gq);
         }
      }

      dest.set(best);
      dest.x *= (double)signX;
      dest.y *= (double)signY;
      dest.z *= (double)signZ;
      dest.w *= (double)signW;
      return dest;
   }

   public static void dampSubLevel(
      MassData massData,
      Vector3d frictionForce,
      Vector3d frictionTorque,
      Vector3dc localLinearVelocity,
      Vector3dc localAngularVelocity,
      double timeStep,
      ForceTotal forceTotal
   ) {
      Vector3d expectedVelocity = new Vector3d();
      expectedVelocity.set(frictionForce);
      expectedVelocity.mul(massData.getInverseMass());
      expectedVelocity.mul(timeStep);
      double forceScale = getClampingFactor(localLinearVelocity, expectedVelocity);
      expectedVelocity.set(frictionTorque);
      massData.getInverseInertiaTensor().transform(expectedVelocity);
      expectedVelocity.mul(timeStep);
      double torqueScale = getClampingFactor(localAngularVelocity, expectedVelocity);
      frictionForce.mul(forceScale);
      frictionTorque.mul(torqueScale);
      forceTotal.applyLinearAndAngularImpulse(frictionForce, frictionTorque);
   }

   private static double getClampingFactor(Vector3dc currentVelocity, Vector3dc expectedVelocityChange) {
      double k = -currentVelocity.dot(expectedVelocityChange);
      double v = currentVelocity.lengthSquared();
      if (k < 0.0) {
         return 0.0;
      } else if (10.0 * k < v) {
         return 1.0 - k / (2.0 * v);
      } else {
         return v < 1.0E-10 ? v / (k + 1.0E-10) : v * (1.0 - java.lang.Math.exp(-k / v)) / k;
      }
   }

   public static enum GridQuats implements ObjectIterable<Quaterniondc> {
      ALL(2047),
      X_AXIS(19),
      Y_AXIS(37),
      Z_AXIS(73),
      REAL(1137);

      private final ObjectList<Quaterniondc> currentQuats = new ObjectArrayList(SableMathUtils.ALL_QUATS.length);
      private final ObjectList<Quaterniondc> oppositeQuats = new ObjectArrayList(SableMathUtils.ALL_QUATS.length);

      private GridQuats(int bitPattern) {
         for (Quaterniondc q : SableMathUtils.ALL_QUATS) {
            ((bitPattern & 1) > 0 ? this.currentQuats : this.oppositeQuats).add(q);
            bitPattern >>= 1;
         }
      }

      public ObjectIterable<Quaterniondc> opposite() {
         return this.oppositeQuats::iterator;
      }

      @NotNull
      public ObjectIterator<Quaterniondc> iterator() {
         return this.currentQuats.iterator();
      }
   }
}
