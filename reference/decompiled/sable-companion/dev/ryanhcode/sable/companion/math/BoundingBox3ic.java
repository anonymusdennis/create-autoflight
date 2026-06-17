package dev.ryanhcode.sable.companion.math;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public sealed interface BoundingBox3ic permits BoundingBox3i {
   @Contract(
      pure = true
   )
   default boolean intersects(BoundingBox3ic other) {
      return this.intersects(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
   }

   @Contract(
      pure = true
   )
   default boolean intersects(BoundingBox other) {
      return this.intersects(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
   }

   @Contract(
      pure = true
   )
   default boolean intersects(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      return this.maxX() >= minX && this.maxY() >= minY && this.maxZ() >= minZ && this.minX() <= maxX && this.minY() <= maxY && this.minZ() <= maxZ;
   }

   @Contract(
      pure = true
   )
   default boolean contains(Vector3ic point) {
      return this.contains(point.x(), point.y(), point.z());
   }

   @Contract(
      pure = true
   )
   default boolean contains(int x, int y, int z) {
      return x >= this.minX() && x <= this.maxX() && y >= this.minY() && y <= this.maxY() && z >= this.minZ() && z <= this.maxZ();
   }

   @Contract(
      pure = true
   )
   default boolean contains(Vector3dc other) {
      return other.x() >= (double)this.minX()
         && other.x() <= (double)(this.maxX() + 1)
         && other.y() >= (double)this.minY()
         && other.y() <= (double)(this.maxY() + 1)
         && other.z() >= (double)this.minZ()
         && other.z() <= (double)(this.maxZ() + 1);
   }

   @Contract(
      pure = true
   )
   int minX();

   @Contract(
      pure = true
   )
   int minY();

   @Contract(
      pure = true
   )
   int minZ();

   @Contract(
      pure = true
   )
   int maxX();

   @Contract(
      pure = true
   )
   int maxY();

   @Contract(
      pure = true
   )
   int maxZ();

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default BoundingBox3i expandTo(Vector3ic point, BoundingBox3i dest) {
      return this.expandTo(point.x(), point.y(), point.z(), dest);
   }

   @Contract(
      value = "_,_,_,_->param4",
      mutates = "param4"
   )
   default BoundingBox3i expandTo(int x, int y, int z, BoundingBox3i dest) {
      dest.set(this);
      dest.maxX = Math.max(dest.maxX, x);
      dest.maxY = Math.max(dest.maxY, y);
      dest.maxZ = Math.max(dest.maxZ, z);
      dest.minX = Math.min(dest.minX, x);
      dest.minY = Math.min(dest.minY, y);
      dest.minZ = Math.min(dest.minZ, z);
      return dest;
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default BoundingBox3i expandTo(BoundingBox3ic other, BoundingBox3i dest) {
      dest.set(this);
      dest.maxX = Math.max(dest.maxX, other.maxX());
      dest.maxY = Math.max(dest.maxY, other.maxY());
      dest.maxZ = Math.max(dest.maxZ, other.maxZ());
      dest.minX = Math.min(dest.minX, other.minX());
      dest.minY = Math.min(dest.minY, other.minY());
      dest.minZ = Math.min(dest.minZ, other.minZ());
      return dest;
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default BoundingBox3i move(Vector3ic vec, BoundingBox3i dest) {
      return this.move(vec.x(), vec.y(), vec.z(), dest);
   }

   @Contract(
      value = "_,_,_,_->param4",
      mutates = "param4"
   )
   default BoundingBox3i move(int x, int y, int z, BoundingBox3i dest) {
      dest.set(this);
      dest.minX += x;
      dest.minY += y;
      dest.minZ += z;
      dest.maxX += x;
      dest.maxY += y;
      dest.maxZ += z;
      return dest;
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default BoundingBox3i intersect(BoundingBox3ic box, BoundingBox3i dest) {
      dest.setUnchecked(
         Math.max(this.minX(), box.minX()),
         Math.max(this.minY(), box.minY()),
         Math.max(this.minZ(), box.minZ()),
         Math.min(this.maxX(), box.maxX()),
         Math.min(this.maxY(), box.maxY()),
         Math.min(this.maxZ(), box.maxZ())
      );
      return dest;
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3i center(Vector3i dest) {
      return dest.set((this.minX() + this.maxX()) / 2, (this.minY() + this.maxY()) / 2, (this.minZ() + this.maxZ()) / 2);
   }

   @Contract(
      value = "_->param1",
      mutates = "param1"
   )
   default Vector3i size(Vector3i dest) {
      return dest.set(this.maxX() - this.minX(), this.maxY() - this.minY(), this.maxZ() - this.minZ());
   }

   @Contract(
      pure = true
   )
   default int width() {
      return this.maxX() - this.minX() + 1;
   }

   @Contract(
      pure = true
   )
   default int height() {
      return this.maxY() - this.minY() + 1;
   }

   @Contract(
      pure = true
   )
   default int length() {
      return this.maxZ() - this.minZ() + 1;
   }

   @Contract(
      pure = true
   )
   default int volume() {
      return (this.maxX() - this.minX() + 1) * (this.maxY() - this.minY() + 1) * (this.maxZ() - this.minZ() + 1);
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
      return dest.set(this.minX() >> 4, this.minY() >> 4, this.minZ() >> 4, this.maxX() >> 4, this.maxY() >> 4, this.maxZ() >> 4);
   }

   @Contract(
      value = "->new",
      pure = true
   )
   default AABB toAABB() {
      return new AABB(
         (double)this.minX(), (double)this.minY(), (double)this.minZ(), (double)(this.maxX() + 1), (double)(this.maxY() + 1), (double)(this.maxZ() + 1)
      );
   }

   @Contract(
      value = "->new",
      pure = true
   )
   default BoundingBox toMojang() {
      return new BoundingBox(this.minX(), this.minY(), this.minZ(), this.maxX(), this.maxY(), this.maxZ());
   }
}
