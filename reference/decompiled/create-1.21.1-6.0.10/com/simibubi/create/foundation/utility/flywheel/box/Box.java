package com.simibubi.create.foundation.utility.flywheel.box;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public interface Box {
   int getMinX();

   int getMinY();

   int getMinZ();

   int getMaxX();

   int getMaxY();

   int getMaxZ();

   default int sizeX() {
      return this.getMaxX() - this.getMinX();
   }

   default int sizeY() {
      return this.getMaxY() - this.getMinY();
   }

   default int sizeZ() {
      return this.getMaxZ() - this.getMinZ();
   }

   default int volume() {
      return this.sizeX() * this.sizeY() * this.sizeZ();
   }

   default boolean isEmpty() {
      return this.getMinX() == this.getMaxX() || this.getMinY() == this.getMaxY() || this.getMinZ() == this.getMaxZ();
   }

   default boolean sameAs(Box other) {
      return this.getMinX() == other.getMinX()
         && this.getMinY() == other.getMinY()
         && this.getMinZ() == other.getMinZ()
         && this.getMaxX() == other.getMaxX()
         && this.getMaxY() == other.getMaxY()
         && this.getMaxZ() == other.getMaxZ();
   }

   default boolean sameAs(Box other, int margin) {
      return this.getMinX() == other.getMinX() - margin
         && this.getMinY() == other.getMinY() - margin
         && this.getMinZ() == other.getMinZ() - margin
         && this.getMaxX() == other.getMaxX() + margin
         && this.getMaxY() == other.getMaxY() + margin
         && this.getMaxZ() == other.getMaxZ() + margin;
   }

   default boolean sameAs(AABB other) {
      return (double)this.getMinX() == Math.floor(other.minX)
         && (double)this.getMinY() == Math.floor(other.minY)
         && (double)this.getMinZ() == Math.floor(other.minZ)
         && (double)this.getMaxX() == Math.ceil(other.maxX)
         && (double)this.getMaxY() == Math.ceil(other.maxY)
         && (double)this.getMaxZ() == Math.ceil(other.maxZ);
   }

   default boolean intersects(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      return this.getMinX() < maxX && this.getMaxX() > minX && this.getMinY() < maxY && this.getMaxY() > minY && this.getMinZ() < maxZ && this.getMaxZ() > minZ;
   }

   default boolean intersects(Box other) {
      return this.intersects(other.getMinX(), other.getMinY(), other.getMinZ(), other.getMaxX(), other.getMaxY(), other.getMaxZ());
   }

   default boolean contains(int x, int y, int z) {
      return x >= this.getMinX() && x <= this.getMaxX() && y >= this.getMinY() && y <= this.getMaxY() && z >= this.getMinZ() && z <= this.getMaxZ();
   }

   default boolean contains(Box other) {
      return other.getMinX() >= this.getMinX()
         && other.getMaxX() <= this.getMaxX()
         && other.getMinY() >= this.getMinY()
         && other.getMaxY() <= this.getMaxY()
         && other.getMinZ() >= this.getMinZ()
         && other.getMaxZ() <= this.getMaxZ();
   }

   default void forEachContained(Box.CoordinateConsumer func) {
      int minX = this.getMinX();
      int minY = this.getMinY();
      int minZ = this.getMinZ();
      int maxX = this.getMaxX();
      int maxY = this.getMaxY();
      int maxZ = this.getMaxZ();

      for (int x = minX; x < maxX; x++) {
         for (int y = minY; y < maxY; y++) {
            for (int z = minZ; z < maxZ; z++) {
               func.accept(x, y, z);
            }
         }
      }
   }

   default boolean hasPowerOf2Sides() {
      return Mth.isPowerOfTwo(this.volume());
   }

   default MutableBox union(Box other) {
      int minX = Math.min(this.getMinX(), other.getMinX());
      int minY = Math.min(this.getMinY(), other.getMinY());
      int minZ = Math.min(this.getMinZ(), other.getMinZ());
      int maxX = Math.max(this.getMaxX(), other.getMaxX());
      int maxY = Math.max(this.getMaxY(), other.getMaxY());
      int maxZ = Math.max(this.getMaxZ(), other.getMaxZ());
      return new MutableBox(minX, minY, minZ, maxX, maxY, maxZ);
   }

   default MutableBox intersect(Box other) {
      int minX = Math.max(this.getMinX(), other.getMinX());
      int minY = Math.max(this.getMinY(), other.getMinY());
      int minZ = Math.max(this.getMinZ(), other.getMinZ());
      int maxX = Math.min(this.getMaxX(), other.getMaxX());
      int maxY = Math.min(this.getMaxY(), other.getMaxY());
      int maxZ = Math.min(this.getMaxZ(), other.getMaxZ());
      return new MutableBox(minX, minY, minZ, maxX, maxY, maxZ);
   }

   default AABB toAABB() {
      return new AABB(
         (double)this.getMinX(), (double)this.getMinY(), (double)this.getMinZ(), (double)this.getMaxX(), (double)this.getMaxY(), (double)this.getMaxZ()
      );
   }

   default MutableBox copy() {
      return new MutableBox(this.getMinX(), this.getMinY(), this.getMinZ(), this.getMaxX(), this.getMaxY(), this.getMaxZ());
   }

   @FunctionalInterface
   public interface CoordinateConsumer {
      void accept(int var1, int var2, int var3);
   }
}
