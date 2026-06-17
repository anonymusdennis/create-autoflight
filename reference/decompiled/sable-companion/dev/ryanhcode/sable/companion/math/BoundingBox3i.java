package dev.ryanhcode.sable.companion.math;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.companion.impl.SableCompanionUtil;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3ic;

public final class BoundingBox3i implements BoundingBox3ic {
   public static final BoundingBox3ic EMPTY = new BoundingBox3i()
      .setUnchecked(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
   public static Codec<BoundingBox3i> CODEC = Codec.INT
      .listOf()
      .comapFlatMap(
         list -> SableCompanionUtil.fixedSize(list, 6)
               .map(
                  iList -> new BoundingBox3i(
                        (Integer)iList.getFirst(),
                        (Integer)iList.get(1),
                        (Integer)iList.get(2),
                        (Integer)iList.get(3),
                        (Integer)iList.get(4),
                        (Integer)iList.get(5)
                     )
               ),
         bb -> List.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
      );
   public int minX;
   public int minY;
   public int minZ;
   public int maxX;
   public int maxY;
   public int maxZ;

   public BoundingBox3i(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      this.set(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public BoundingBox3i(BlockPos min, BlockPos max) {
      this.set(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
   }

   public BoundingBox3i(BoundingBox3ic other) {
      this.set(other);
   }

   public BoundingBox3i(BoundingBox other) {
      this.set(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
   }

   public BoundingBox3i() {
      this(0, 0, 0, 0, 0, 0);
   }

   public BoundingBox3i(BoundingBox3d other) {
      this.set(Mth.floor(other.minX), Mth.floor(other.minY), Mth.floor(other.minZ), Mth.floor(other.maxX), Mth.floor(other.maxY), Mth.floor(other.maxZ));
   }

   @Nullable
   public static BoundingBox3i from(Iterable<BlockPos> blocks) {
      Iterator<BlockPos> iterator = blocks.iterator();
      if (!iterator.hasNext()) {
         return null;
      } else {
         BlockPos pos = iterator.next();
         int minX = pos.getX();
         int minY = pos.getY();
         int minZ = pos.getZ();
         int maxX = minX;
         int maxY = minY;
         int maxZ = minZ;

         while (iterator.hasNext()) {
            pos = iterator.next();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
         }

         return new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);
      }
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i set(BoundingBox3d other) {
      this.set(Mth.floor(other.minX), Mth.floor(other.minY), Mth.floor(other.minZ), Mth.floor(other.maxX), Mth.floor(other.maxY), Mth.floor(other.maxZ));
      return this;
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i set(BoundingBox3ic other) {
      this.set(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
      return this;
   }

   @Contract(
      value = "_,_,_,_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3i set(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      this.minX = Math.min(minX, maxX);
      this.minY = Math.min(minY, maxY);
      this.minZ = Math.min(minZ, maxZ);
      this.maxX = Math.max(minX, maxX);
      this.maxY = Math.max(minY, maxY);
      this.maxZ = Math.max(minZ, maxZ);
      return this;
   }

   @Contract(
      value = "_,_,_,_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3i setUnchecked(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
      this.minX = minX;
      this.minY = minY;
      this.minZ = minZ;
      this.maxX = maxX;
      this.maxY = maxY;
      this.maxZ = maxZ;
      return this;
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i setUnchecked(BoundingBox3ic other) {
      this.minX = other.minX();
      this.minY = other.minY();
      this.minZ = other.minZ();
      this.maxX = other.maxX();
      this.maxY = other.maxY();
      this.maxZ = other.maxZ();
      return this;
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3i expand(int xExpansion, int yExpansion, int zExpansion) {
      this.minX -= xExpansion;
      this.minY -= yExpansion;
      this.minZ -= zExpansion;
      this.maxX += xExpansion;
      this.maxY += yExpansion;
      this.maxZ += zExpansion;
      return this;
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i expandTo(Vector3ic point) {
      return this.expandTo(point, this);
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3i expandTo(int x, int y, int z) {
      return this.expandTo(x, y, z, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i expandTo(BoundingBox3ic other) {
      return this.expandTo(other, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i move(Vector3ic vec) {
      return this.move(vec.x(), vec.y(), vec.z(), this);
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3i move(int x, int y, int z) {
      return this.move(x, y, z, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3i intersect(BoundingBox3ic box) {
      return this.set(
         Math.max(this.minX(), box.minX()),
         Math.max(this.minY(), box.minY()),
         Math.max(this.minZ(), box.minZ()),
         Math.min(this.maxX(), box.maxX()),
         Math.min(this.maxY(), box.maxY()),
         Math.min(this.maxZ(), box.maxZ())
      );
   }

   @Override
   public int minX() {
      return this.minX;
   }

   @Override
   public int minY() {
      return this.minY;
   }

   @Override
   public int minZ() {
      return this.minZ;
   }

   @Override
   public int maxX() {
      return this.maxX;
   }

   @Override
   public int maxY() {
      return this.maxY;
   }

   @Override
   public int maxZ() {
      return this.maxZ;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else {
         return !(obj instanceof BoundingBox3ic other)
            ? false
            : this.minX() == other.minX()
               && this.minY() == other.minY()
               && this.minZ() == other.minZ()
               && this.maxX() == other.maxX()
               && this.maxY() == other.maxY()
               && this.maxZ() == other.maxZ();
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
      return "BoundingBox3i{minX="
         + this.minX
         + ", minY="
         + this.minY
         + ", minZ="
         + this.minZ
         + ", maxX="
         + this.maxX
         + ", maxY="
         + this.maxY
         + ", maxZ="
         + this.maxZ
         + "}";
   }
}
