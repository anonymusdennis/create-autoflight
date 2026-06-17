package dev.simulated_team.simulated.util;

import com.mojang.math.Axis;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterable;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SimMathUtils {
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

   public static Vec3 rotateQuat(Vec3 V, Quaterniond Q) {
      Quaterniond q = new Quaterniond((double)((float)V.x), (double)((float)V.y), (double)((float)V.z), 0.0);
      Quaterniond Q2 = new Quaterniond(Q);
      q.mul(Q2);
      Q2.conjugate();
      Q2.mul(q);
      return new Vec3(Q2.x(), Q2.y(), Q2.z());
   }

   public static Vec3 rotateQuat(Vec3 V, Quaternionf Q) {
      Quaternionf q = new Quaternionf((float)V.x, (float)V.y, (float)V.z, 0.0F);
      Quaternionf Q2 = new Quaternionf(Q);
      q.mul(Q2);
      Q2.conjugate();
      Q2.mul(q);
      return new Vec3((double)Q2.x(), (double)Q2.y(), (double)Q2.z());
   }

   public static Vec3 rotateQuatReverse(Vec3 V, Quaterniond Q) {
      Quaterniond q = new Quaterniond((double)((float)V.x), (double)((float)V.y), (double)((float)V.z), 0.0);
      Quaterniond Q2 = new Quaterniond(Q);
      Q2.conjugate();
      q.mul(Q2);
      Q2.conjugate();
      Q2.mul(q);
      return new Vec3(Q2.x(), Q2.y(), Q2.z());
   }

   public static Vec3 rotateQuatReverse(Vec3 V, Quaternionf Q) {
      Quaternionf q = new Quaternionf((float)V.x, (float)V.y, (float)V.z, 0.0F);
      Quaternionf Q2 = new Quaternionf(Q);
      Q2.conjugate();
      q.mul(Q2);
      Q2.conjugate();
      Q2.mul(q);
      return new Vec3((double)Q2.x(), (double)Q2.y(), (double)Q2.z());
   }

   public static Vec3 clampIntoCone(Vec3 v, Vec3 coneAxis, double coneAngle) {
      double vv = v.dot(v);
      double vn = v.dot(coneAxis);
      double nn = coneAxis.dot(coneAxis);
      double disc = nn * vv * 1.01 - vn * vn;
      double offsetDistance = (-vn + Math.sqrt(disc) / Math.tan(coneAngle)) / nn;
      return offsetDistance < 0.0 ^ coneAngle < 0.0 ? v : v.add(coneAxis.scale(offsetDistance)).normalize();
   }

   public static void clampIntoCone(Vector3d v, Vector3d coneAxis, double coneAngle) {
      double vv = v.dot(v);
      double vn = v.dot(coneAxis);
      double nn = coneAxis.dot(coneAxis);
      double disc = nn * vv * 1.01 - vn * vn;
      double offsetDistance = (-vn + Math.sqrt(disc) / Math.tan(coneAngle)) / nn;
      if (!(offsetDistance < 0.0 ^ coneAngle < 0.0)) {
         v.add(new Vector3d(coneAxis).mul(offsetDistance)).normalize();
      }
   }

   public static boolean isInCylinder(Vector3dc axisVector, Vector3d relativePosition, double cylinderLength, double cylinderRadius) {
      double distance = axisVector.dot(relativePosition);
      if (!(distance < 0.0) && !(distance > cylinderLength)) {
         Vector3d scaledAxis = axisVector.mul(distance, new Vector3d());
         relativePosition = relativePosition.sub(scaledAxis, scaledAxis);
         return relativePosition.lengthSquared() <= cylinderRadius * cylinderRadius;
      } else {
         return false;
      }
   }

   public static Quaternionf getBlockStateOrientation(Direction facing) {
      Quaternionf orientation;
      if (facing.getAxis().isHorizontal()) {
         orientation = Axis.YP.rotationDegrees(AngleHelper.horizontalAngle(facing.getOpposite()));
      } else {
         orientation = new Quaternionf();
      }

      orientation.rotateX((-90.0F - AngleHelper.verticalAngle(facing)) * (float) (Math.PI / 180.0));
      return orientation;
   }

   public static Quaternionf getQuaternionfFromVectorRotation(Vector3dc start, Vector3dc end) {
      Vector3d cross = new Vector3d();
      start.cross(end, cross);
      Quaternionf Q = new Quaternionf((float)cross.x(), (float)cross.y(), (float)cross.z(), 1.0F + (float)start.dot(end));
      Q.normalize();
      return Q;
   }

   public static Quaterniond clampQuaternionToGrid(Quaterniond q, Iterable<Quaterniondc> gridQuats) {
      return clampQuaternionToGrid(q, gridQuats, q);
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

   public static float smoothStep(float t) {
      return t * t * (3.0F - 2.0F * t);
   }

   public static double getClosestYaw(Quaterniond orientation) {
      double d = OrientedBoundingBox3d.UP.dot(new Vector3d(orientation.x(), orientation.y(), orientation.z()));
      return 2.0 * Math.atan2(-d, orientation.w());
   }

   public static enum GridQuats implements ObjectIterable<Quaterniondc> {
      ALL(2047),
      X_AXIS(19),
      Y_AXIS(37),
      Z_AXIS(73),
      REAL(1137);

      private final ObjectList<Quaterniondc> currentQuats = new ObjectArrayList(SimMathUtils.ALL_QUATS.length);
      private final ObjectList<Quaterniondc> oppositeQuats = new ObjectArrayList(SimMathUtils.ALL_QUATS.length);

      private GridQuats(int bitPattern) {
         for (Quaterniondc q : SimMathUtils.ALL_QUATS) {
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
