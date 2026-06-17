package com.simibubi.create.foundation.utility.flywheel.box;

import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class MutableBox implements Box {
   protected int minX;
   protected int minY;
   protected int minZ;
   protected int maxX;
   protected int maxY;
   protected int maxZ;

   public MutableBox() {
   }

   public MutableBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
   }

   public static MutableBox from(AABB aabb) {
      int minX = (int)Math.floor(aabb.minX);
      int minY = (int)Math.floor(aabb.minY);
      int minZ = (int)Math.floor(aabb.minZ);
      int maxX = (int)Math.ceil(aabb.maxX);
      int maxY = (int)Math.ceil(aabb.maxY);
      int maxZ = (int)Math.ceil(aabb.maxZ);
      return new MutableBox(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public static MutableBox from(Vec3i pos) {
      return new MutableBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
   }

   public static MutableBox from(SectionPos pos) {
      return new MutableBox(pos.minBlockX(), pos.minBlockY(), pos.minBlockZ(), pos.maxBlockX() + 1, pos.maxBlockY() + 1, pos.maxBlockZ() + 1);
   }

   public static MutableBox from(Vec3i start, Vec3i end) {
      return new MutableBox(start.getX(), start.getY(), start.getZ(), end.getX() + 1, end.getY() + 1, end.getZ() + 1);
   }

   public static MutableBox ofRadius(int radius) {
      return new MutableBox(-radius, -radius, -radius, radius + 1, radius + 1, radius + 1);
   }

   public static Box containingAll(Collection<BlockPos> positions) {
      if (positions.isEmpty()) {
         return new MutableBox();
      } else {
         int minX = Integer.MAX_VALUE;
         int minY = Integer.MAX_VALUE;
         int minZ = Integer.MAX_VALUE;
         int maxX = Integer.MIN_VALUE;
         int maxY = Integer.MIN_VALUE;
         int maxZ = Integer.MIN_VALUE;

         for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
         }

         return new MutableBox(minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Override
   public int getMinX() {
      return this.minX;
   }

   @Override
   public int getMinY() {
      return this.minY;
   }

   @Override
   public int getMinZ() {
      return this.minZ;
   }

   @Override
   public int getMaxX() {
      return this.maxX;
   }

   @Override
   public int getMaxY() {
      return this.maxY;
   }

   @Override
   public int getMaxZ() {
      return this.maxZ;
   }

   public void setMinX(int minX) {
      this.minX = minX;
   }

   public void setMinY(int minY) {
      this.minY = minY;
   }

   public MutableBox setMinZ(int minZ) {
      this.minZ = minZ;
      return this;
   }

   public void setMaxX(int maxX) {
      this.maxX = maxX;
   }

   public void setMaxY(int maxY) {
      this.maxY = maxY;
   }

   public void setMaxZ(int maxZ) {
      this.maxZ = maxZ;
   }

   public void setMin(int x, int y, int z) {
      this.minX = x;
      this.minY = y;
      this.minZ = z;
   }

   public void setMax(int x, int y, int z) {
      this.maxX = x;
      this.maxY = y;
      this.maxZ = z;
   }

   public void setMin(Vec3i v) {
      this.setMin(v.getX(), v.getY(), v.getZ());
   }

   public void setMax(Vec3i v) {
      this.setMax(v.getX(), v.getY(), v.getZ());
   }

   public void assign(Box other) {
      this.minX = other.getMinX();
      this.minY = other.getMinY();
      this.minZ = other.getMinZ();
      this.maxX = other.getMaxX();
      this.maxY = other.getMaxY();
      this.maxZ = other.getMaxZ();
   }

   public void assign(AABB other) {
      this.minX = (int)Math.floor(other.minX);
      this.minY = (int)Math.floor(other.minY);
      this.minZ = (int)Math.floor(other.minZ);
      this.maxX = (int)Math.ceil(other.maxX);
      this.maxY = (int)Math.ceil(other.maxY);
      this.maxZ = (int)Math.ceil(other.maxZ);
   }

   public void assign(Vec3i start, Vec3i end) {
      this.minX = start.getX();
      this.minY = start.getY();
      this.minZ = start.getZ();
      this.maxX = end.getX() + 1;
      this.maxY = end.getY() + 1;
      this.maxZ = end.getZ() + 1;
   }

   public void unionAssign(Box other) {
      this.minX = Math.min(this.minX, other.getMinX());
      this.minY = Math.min(this.minY, other.getMinY());
      this.minZ = Math.min(this.minZ, other.getMinZ());
      this.maxX = Math.max(this.maxX, other.getMaxX());
      this.maxY = Math.max(this.maxY, other.getMaxY());
      this.maxZ = Math.max(this.maxZ, other.getMaxZ());
   }

   public void unionAssign(AABB other) {
      this.minX = Math.min(this.minX, (int)Math.floor(other.minX));
      this.minY = Math.min(this.minY, (int)Math.floor(other.minY));
      this.minZ = Math.min(this.minZ, (int)Math.floor(other.minZ));
      this.maxX = Math.max(this.maxX, (int)Math.ceil(other.maxX));
      this.maxY = Math.max(this.maxY, (int)Math.ceil(other.maxY));
      this.maxZ = Math.max(this.maxZ, (int)Math.ceil(other.maxZ));
   }

   public void intersectAssign(Box other) {
      this.minX = Math.max(this.minX, other.getMinX());
      this.minY = Math.max(this.minY, other.getMinY());
      this.minZ = Math.max(this.minZ, other.getMinZ());
      this.maxX = Math.min(this.maxX, other.getMaxX());
      this.maxY = Math.min(this.maxY, other.getMaxY());
      this.maxZ = Math.min(this.maxZ, other.getMaxZ());
   }

   public void fixMinMax() {
      int minX = Math.min(this.minX, this.maxX);
      int minY = Math.min(this.minY, this.maxY);
      int minZ = Math.min(this.minZ, this.maxZ);
      int maxX = Math.max(this.minX, this.maxX);
      int maxY = Math.max(this.minY, this.maxY);
      int maxZ = Math.max(this.minZ, this.maxZ);
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
   }

   public void translate(int x, int y, int z) {
      this.minX += x;
      this.maxX += x;
      this.minY += y;
      this.maxY += y;
      this.minZ += z;
      this.maxZ += z;
   }

   public void translate(Vec3i by) {
      this.translate(by.getX(), by.getY(), by.getZ());
   }

   public void grow(int x, int y, int z) {
      this.minX -= x;
      this.minY -= y;
      this.minZ -= z;
      this.maxX += x;
      this.maxY += y;
      this.maxZ += z;
   }

   public void grow(int s) {
      this.grow(s, s, s);
   }

   public void nextPowerOf2() {
      int sizeX = Mth.smallestEncompassingPowerOfTwo(this.sizeX());
      int sizeY = Mth.smallestEncompassingPowerOfTwo(this.sizeY());
      int sizeZ = Mth.smallestEncompassingPowerOfTwo(this.sizeZ());
      this.maxX = this.minX + sizeX;
      this.maxY = this.minY + sizeY;
      this.maxZ = this.minZ + sizeZ;
   }

   public void nextPowerOf2Centered() {
      int sizeX = this.sizeX();
      int sizeY = this.sizeY();
      int sizeZ = this.sizeZ();
      int newSizeX = Mth.smallestEncompassingPowerOfTwo(sizeX);
      int newSizeY = Mth.smallestEncompassingPowerOfTwo(sizeY);
      int newSizeZ = Mth.smallestEncompassingPowerOfTwo(sizeZ);
      int diffX = newSizeX - sizeX;
      int diffY = newSizeY - sizeY;
      int diffZ = newSizeZ - sizeZ;
      this.minX -= diffX / 2;
      this.minY -= diffY / 2;
      this.minZ -= diffZ / 2;
      this.maxX += (diffX + 1) / 2;
      this.maxY += (diffY + 1) / 2;
      this.maxZ += (diffZ + 1) / 2;
   }

   public void mirrorAbout(Axis axis) {
      Vec3i axisVec = Direction.get(AxisDirection.POSITIVE, axis).getNormal();
      int flipX = axisVec.getX() - 1;
      int flipY = axisVec.getY() - 1;
      int flipZ = axisVec.getZ() - 1;
      int maxX = this.maxX * flipX;
      int maxY = this.maxY * flipY;
      int maxZ = this.maxZ * flipZ;
      this.maxX = this.minX * flipX;
      this.maxY = this.minY * flipY;
      this.maxZ = this.minZ * flipZ;
      this.minX = maxX;
      this.minY = maxY;
      this.minZ = maxZ;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o == null) {
         return false;
      } else {
         return o instanceof Box that ? this.sameAs(that) : false;
      }
   }

   @Override
   public int hashCode() {
      int result = this.minX;
      result = 31 * result + this.minY;
      result = 31 * result + this.minZ;
      result = 31 * result + this.maxX;
      result = 31 * result + this.maxY;
      return 31 * result + this.maxZ;
   }

   @Override
   public String toString() {
      return "(" + this.minX + ", " + this.minY + ", " + this.minZ + ")->(" + this.maxX + ", " + this.maxY + ", " + this.maxZ + ")";
   }
}
