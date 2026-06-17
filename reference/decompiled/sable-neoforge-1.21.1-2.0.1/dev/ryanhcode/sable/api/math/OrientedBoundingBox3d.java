package dev.ryanhcode.sable.api.math;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class OrientedBoundingBox3d {
   public static final Vector3dc RIGHT = new Vector3d(1.0, 0.0, 0.0);
   public static final Vector3dc UP = new Vector3d(0.0, 1.0, 0.0);
   public static final Vector3dc FORWARD = new Vector3d(0.0, 0.0, 1.0);
   private final Vector3d position = new Vector3d();
   private final Vector3d dimensions = new Vector3d();
   private final Quaterniond orientation = new Quaterniond();
   private final LevelReusedVectors sink;

   public OrientedBoundingBox3d(@NotNull LevelReusedVectors sink) {
      this.sink = sink;
   }

   public OrientedBoundingBox3d(@NotNull Vector3dc position, @NotNull Vector3dc dimensions, @NotNull Quaterniondc orientation, @NotNull LevelReusedVectors sink) {
      this.position.set(position);
      this.dimensions.set(dimensions);
      this.orientation.set(orientation);
      this.sink = sink;
   }

   public OrientedBoundingBox3d(
      double x, double y, double z, double sizeX, double sizeY, double sizeZ, @NotNull Quaterniondc orientation, @NotNull LevelReusedVectors sink
   ) {
      this.position.set(x, y, z);
      this.dimensions.set(sizeX, sizeY, sizeZ);
      this.orientation.set(orientation);
      this.sink = sink;
   }

   public void set(Vector3dc position, Vector3dc dimensions, Quaterniondc orientation) {
      this.position.set(position);
      this.dimensions.set(dimensions);
      this.orientation.set(orientation);
   }

   public OrientedBoundingBox3d setPosition(Vector3dc position) {
      this.position.set(position);
      return this;
   }

   public OrientedBoundingBox3d setDimensions(Vector3dc dimensions) {
      this.dimensions.set(dimensions);
      return this;
   }

   public OrientedBoundingBox3d setOrientation(Quaterniondc orientation) {
      this.orientation.set(orientation);
      return this;
   }

   public Quaterniond getOrientation() {
      return this.orientation;
   }

   public Vector3d getPosition() {
      return this.position;
   }

   public Vector3d getDimensions() {
      return this.dimensions;
   }

   @NotNull
   public Vector3d[] vertices(Vector3d[] result) {
      this.dimensions.mul(0.5, this.sink.tempmin);
      this.dimensions.mul(-0.5, this.sink.tempmax);
      this.orientation.transform(this.sink.tempmin, result[0]).add(this.position);
      this.orientation.transform(this.sink.tempVert1.set(this.sink.tempmax.x, this.sink.tempmin.y, this.sink.tempmin.z), result[1]).add(this.position);
      this.orientation.transform(this.sink.tempVert4.set(this.sink.tempmin.x, this.sink.tempmin.y, this.sink.tempmax.z), result[4]).add(this.position);
      this.orientation.transform(this.sink.tempVert5.set(this.sink.tempmax.x, this.sink.tempmin.y, this.sink.tempmax.z), result[5]).add(this.position);
      this.orientation.transform(this.sink.tempVert3.set(this.sink.tempmax.x, this.sink.tempmax.y, this.sink.tempmin.z), result[3]).add(this.position);
      this.orientation.transform(this.sink.tempVert2.set(this.sink.tempmin.x, this.sink.tempmax.y, this.sink.tempmin.z), result[2]).add(this.position);
      this.orientation.transform(this.sink.tempVert6.set(this.sink.tempmin.x, this.sink.tempmax.y, this.sink.tempmax.z), result[6]).add(this.position);
      this.orientation.transform(this.sink.tempmax, result[7]).add(this.position);
      return result;
   }

   public Vector3d rotate(@NotNull Vector3d vec) {
      return this.orientation.transform(vec);
   }

   private static boolean doesOverlap(@NotNull Vector2d a, @NotNull Vector2d b) {
      return a.x <= b.y && a.y >= b.x;
   }

   public static double getOverlap(@NotNull Vector2d a, @NotNull Vector2d b) {
      return !doesOverlap(a, b) ? 0.0 : Math.min(a.y, b.y) - Math.max(a.x, b.x);
   }

   @NotNull
   public static Vector3d sat(@NotNull OrientedBoundingBox3d obbA, @NotNull OrientedBoundingBox3d obbB) {
      return sat(obbA, obbB, new Vector3d());
   }

   @NotNull
   public static Vector3d sat(@NotNull OrientedBoundingBox3d obbA, @NotNull OrientedBoundingBox3d obbB, @NotNull Vector3d dest) {
      Objects.requireNonNull(obbA, "obbA");
      Objects.requireNonNull(obbB, "obbB");
      Objects.requireNonNull(dest, "dest");
      LevelReusedVectors context = obbA.sink;
      Vector3d[] verticesA = obbA.vertices(context.a);
      Vector3d[] verticesB = obbB.vertices(context.b);
      Vector3d checker = obbA.position.sub(obbB.position, obbA.sink.checker).normalize();
      Vector3d aRight = obbA.rotate(context.obbARight.set(RIGHT));
      Vector3d aUp = obbA.rotate(context.obbAUp.set(UP));
      Vector3d aForward = obbA.rotate(context.obbAForward.set(FORWARD));
      Vector3d bRight = obbB.rotate(context.obbBRight.set(RIGHT));
      Vector3d bUp = obbB.rotate(context.obbBUp.set(UP));
      Vector3d bForward = obbB.rotate(context.obbBForward.set(FORWARD));
      Vector3d mtv = dest.set(Double.MAX_VALUE);
      genChecks(aRight, aUp, aForward, bRight, bUp, bForward, context.checks);
      double minOverlap = Double.MAX_VALUE;

      for (Vector3d check : context.checks) {
         if (!(check.lengthSquared() <= 0.0)) {
            check.normalize();
            checkSeparation(verticesA, check, context.proj1);
            checkSeparation(verticesB, check, context.proj2);
            if (check.dot(checker) > 0.0) {
               check.mul(-1.0);
            }

            double overlap = getOverlap(context.proj1, context.proj2);
            if (overlap == 0.0) {
               return dest.zero();
            }

            if (overlap < minOverlap) {
               minOverlap = overlap;
               mtv.set(check.mul(overlap));
            }
         }
      }

      boolean facingOpposite = obbA.position.sub(obbB.position, context.oppo).dot(mtv) < 0.0;
      if (facingOpposite) {
         mtv.mul(-1.0);
      }

      return mtv;
   }

   public static Vector3d[] genChecks(Vector3d aRight, Vector3d aUp, Vector3d aForward, Vector3d bRight, Vector3d bUp, Vector3d bForward, Vector3d[] checks) {
      checks[0].set(aRight);
      checks[1].set(aUp);
      checks[2].set(aForward);
      checks[3].set(bRight);
      checks[4].set(bUp);
      checks[5].set(bForward);
      aRight.cross(bRight, checks[6]);
      aRight.cross(bUp, checks[7]);
      aRight.cross(bForward, checks[8]);
      aUp.cross(bRight, checks[9]);
      aUp.cross(bUp, checks[10]);
      aUp.cross(bForward, checks[11]);
      aForward.cross(bRight, checks[12]);
      aForward.cross(bUp, checks[13]);
      aForward.cross(bForward, checks[14]);
      return checks;
   }

   public static Vector3dc satToleranced(OrientedBoundingBox3d entityOBB, OrientedBoundingBox3d obbB, double tolerance) {
      Objects.requireNonNull(entityOBB, "entityOBB");
      Objects.requireNonNull(obbB, "obbB");
      LevelReusedVectors context = entityOBB.sink;
      Vector3d[] verticesA = entityOBB.vertices(context.a);
      Vector3d[] verticesB = obbB.vertices(context.b);
      Vector3d checker = entityOBB.position.sub(obbB.position, new Vector3d()).normalize();
      Vector3d aRight = entityOBB.rotate(context.obbARight.set(RIGHT));
      Vector3d aUp = entityOBB.rotate(context.obbAUp.set(UP));
      Vector3d aForward = entityOBB.rotate(context.obbAForward.set(FORWARD));
      Vector3d bRight = obbB.rotate(context.obbBRight.set(RIGHT));
      Vector3d bUp = obbB.rotate(context.obbBUp.set(UP));
      Vector3d bForward = obbB.rotate(context.obbBForward.set(FORWARD));
      Vector3d mtv = new Vector3d(Double.MAX_VALUE);
      genChecks(aRight, aUp, aForward, bRight, bUp, bForward, context.checks);
      double minOverlap = Double.MAX_VALUE;
      int i = 0;

      for (Vector3d check : context.checks) {
         if (!(check.lengthSquared() <= 0.0)) {
            check.normalize();
            checkSeparation(verticesA, check, context.proj1);
            checkSeparation(verticesB, check, context.proj2);
            if (check.dot(checker) > 0.0) {
               check.mul(-1.0);
            }

            double overlap = getOverlap(context.proj1, context.proj2);
            if (overlap == 0.0) {
               return context.zero;
            }

            if (overlap - (i == 14 ? 0.1 : 0.0) < minOverlap) {
               minOverlap = overlap;
               mtv = check.mul(overlap);
            }

            i++;
         }
      }

      boolean facingOpposite = entityOBB.position.sub(obbB.position, context.oppo).dot(mtv) < 0.0;
      if (facingOpposite) {
         mtv.mul(-1.0);
      }

      return mtv;
   }

   @NotNull
   public static Vector2d checkSeparation(@NotNull Vector3d[] self, @NotNull Vector3d axis, Vector2d result) {
      if (axis.lengthSquared() <= 0.0) {
         return result.set(0.0, 0.0);
      } else {
         double min = Double.MAX_VALUE;
         double max = -Double.MAX_VALUE;

         for (Vector3d vec : self) {
            double dot = vec.dot(axis);
            min = Math.min(dot, min);
            max = Math.max(dot, max);
         }

         return result.set(min, max);
      }
   }
}
