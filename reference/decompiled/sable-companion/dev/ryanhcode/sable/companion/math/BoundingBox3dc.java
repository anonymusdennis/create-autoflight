package dev.ryanhcode.sable.companion.math;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public sealed interface BoundingBox3dc permits BoundingBox3d {
   default boolean intersects(BoundingBox3dc other) {
      return this.intersects(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
   }

   default boolean intersects(AABB other) {
      return this.intersects(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
   }

   default boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      return this.maxX() >= minX && this.maxY() >= minY && this.maxZ() >= minZ && this.minX() <= maxX && this.minY() <= maxY && this.minZ() <= maxZ;
   }

   default boolean contains(Vector3dc point) {
      return this.contains(point.x(), point.y(), point.z());
   }

   default boolean contains(double x, double y, double z) {
      return x >= this.minX() && x <= this.maxX() && y >= this.minY() && y <= this.maxY() && z >= this.minZ() && z <= this.maxZ();
   }

   double minX();

   double minY();

   double minZ();

   double maxX();

   double maxY();

   double maxZ();

   default BoundingBox3d expandTo(Vector3dc point, BoundingBox3d dest) {
      return this.expandTo(point.x(), point.y(), point.z(), dest);
   }

   default BoundingBox3d expandTo(double x, double y, double z, BoundingBox3d dest) {
      dest.setUnchecked(this);
      dest.maxX = Math.max(dest.maxX, x);
      dest.maxY = Math.max(dest.maxY, y);
      dest.maxZ = Math.max(dest.maxZ, z);
      dest.minX = Math.min(dest.minX, x);
      dest.minY = Math.min(dest.minY, y);
      dest.minZ = Math.min(dest.minZ, z);
      return dest;
   }

   default BoundingBox3d expandTo(BoundingBox3dc other, BoundingBox3d dest) {
      dest.setUnchecked(this);
      dest.maxX = Math.max(dest.maxX, other.maxX());
      dest.maxY = Math.max(dest.maxY, other.maxY());
      dest.maxZ = Math.max(dest.maxZ, other.maxZ());
      dest.minX = Math.min(dest.minX, other.minX());
      dest.minY = Math.min(dest.minY, other.minY());
      dest.minZ = Math.min(dest.minZ, other.minZ());
      return dest;
   }

   default BoundingBox3d expand(double amount, BoundingBox3d dest) {
      return dest.setUnchecked(
         this.minX() - amount, this.minY() - amount, this.minZ() - amount, this.maxX() + amount, this.maxY() + amount, this.maxZ() + amount
      );
   }

   default BoundingBox3d expand(double amountX, double amountY, double amountZ, BoundingBox3d dest) {
      return dest.setUnchecked(
         this.minX() - amountX, this.minY() - amountY, this.minZ() - amountZ, this.maxX() + amountX, this.maxY() + amountY, this.maxZ() + amountZ
      );
   }

   default BoundingBox3d move(double amountX, double amountY, double amountZ, BoundingBox3d dest) {
      return dest.setUnchecked(
         this.minX() + amountX, this.minY() + amountY, this.minZ() + amountZ, this.maxX() + amountX, this.maxY() + amountY, this.maxZ() + amountZ
      );
   }

   default BoundingBox3d intersect(BoundingBox3dc box, BoundingBox3d dest) {
      return dest.setUnchecked(
         Math.max(this.minX(), box.minX()),
         Math.max(this.minY(), box.minY()),
         Math.max(this.minZ(), box.minZ()),
         Math.min(this.maxX(), box.maxX()),
         Math.min(this.maxY(), box.maxY()),
         Math.min(this.maxZ(), box.maxZ())
      );
   }

   default BoundingBox3d transform(Pose3dc pose, BoundingBox3d dest) {
      return this.transform(pose.bakeIntoMatrix(new Matrix4d()), dest);
   }

   default BoundingBox3d transform(Pose3dc pose, Matrix4d bakedMatrix, BoundingBox3d dest) {
      return this.transform(pose.bakeIntoMatrix(bakedMatrix), dest);
   }

   default BoundingBox3d transform(Matrix4dc mpose, BoundingBox3d dest) {
      double minX = this.minX();
      double minY = this.minY();
      double minZ = this.minZ();
      double maxX = this.maxX();
      double maxY = this.maxY();
      double maxZ = this.maxZ();
      dest.setUnchecked(
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      );
      Vector3d corner = new Vector3d();

      for (int i = 0; i <= 7; i++) {
         corner.set((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ);
         dest.expandTo(mpose.transformPosition(corner), dest);
      }

      return dest;
   }

   default BoundingBox3d transformInverse(Pose3dc pose, BoundingBox3d dest) {
      return this.transformInverse(pose.bakeIntoMatrix(new Matrix4d()).invertAffine(), dest);
   }

   default BoundingBox3d transformInverse(Pose3dc pose, Matrix4d bakedMatrix, BoundingBox3d dest) {
      return this.transformInverse(pose.bakeIntoMatrix(bakedMatrix).invertAffine(), dest);
   }

   default BoundingBox3d transformInverse(Matrix4dc mpose, BoundingBox3d dest) {
      double minX = this.minX();
      double minY = this.minY();
      double minZ = this.minZ();
      double maxX = this.maxX();
      double maxY = this.maxY();
      double maxZ = this.maxZ();
      dest.setUnchecked(
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      );
      Vector3d corner = new Vector3d();

      for (int i = 0; i <= 7; i++) {
         corner.set((i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ);
         dest.expandTo(mpose.transformPosition(corner), dest);
      }

      return dest;
   }

   default Vector3d center() {
      return this.center(new Vector3d());
   }

   default Vector3d center(Vector3d dest) {
      return dest.set((this.minX() + this.maxX()) / 2.0, (this.minY() + this.maxY()) / 2.0, (this.minZ() + this.maxZ()) / 2.0);
   }

   default Vector3d size() {
      return this.size(new Vector3d());
   }

   default Vector3d size(Vector3d dest) {
      return dest.set(this.maxX() - this.minX(), this.maxY() - this.minY(), this.maxZ() - this.minZ());
   }

   @Contract(
      value = "->new",
      pure = true
   )
   default BoundingBox3i chunkBoundsFrom() {
      return this.chunkBoundsFrom(new BoundingBox3i());
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default BoundingBox3i chunkBoundsFrom(BoundingBox3i dest) {
      return dest.set(
         Mth.floor(this.minX()) >> 4,
         Mth.floor(this.minY()) >> 4,
         Mth.floor(this.minZ()) >> 4,
         Mth.floor(this.maxX()) >> 4,
         Mth.floor(this.maxY()) >> 4,
         Mth.floor(this.maxZ()) >> 4
      );
   }

   @Contract(
      pure = true
   )
   default double width() {
      return this.maxX() - this.minX() + 1.0;
   }

   @Contract(
      pure = true
   )
   default double height() {
      return this.maxY() - this.minY() + 1.0;
   }

   @Contract(
      pure = true
   )
   default double length() {
      return this.maxZ() - this.minZ() + 1.0;
   }

   @Contract(
      pure = true
   )
   default double volume() {
      return (this.maxX() - this.minX()) * (this.maxY() - this.minY()) * (this.maxZ() - this.minZ());
   }

   @Contract(
      value = "->new",
      pure = true
   )
   default AABB toMojang() {
      return new AABB(this.minX(), this.minY(), this.minZ(), this.maxX(), this.maxY(), this.maxZ());
   }
}
