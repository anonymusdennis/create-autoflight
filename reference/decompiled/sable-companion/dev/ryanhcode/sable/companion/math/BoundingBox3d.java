package dev.ryanhcode.sable.companion.math;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.companion.impl.SableCompanionUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Vector3dc;

public final class BoundingBox3d implements BoundingBox3dc {
   public static final BoundingBox3d EMPTY = new BoundingBox3d(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   public static Codec<BoundingBox3d> CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         list -> SableCompanionUtil.fixedSize(list, 6)
               .map(
                  iList -> new BoundingBox3d(
                        (Double)iList.getFirst(), (Double)iList.get(1), (Double)iList.get(2), (Double)iList.get(3), (Double)iList.get(4), (Double)iList.get(5)
                     )
               ),
         bb -> List.of(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ)
      );
   public double minX;
   public double minY;
   public double minZ;
   public double maxX;
   public double maxY;
   public double maxZ;

   public BoundingBox3d(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
      this.set(minX, minY, minZ, maxX, maxY, maxZ);
   }

   public BoundingBox3d(BoundingBox3dc other) {
      this.set(other);
   }

   public BoundingBox3d(AABB other) {
      this.set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
   }

   public BoundingBox3d(BoundingBox other) {
      this.set((double)other.minX(), (double)other.minY(), (double)other.minZ(), (double)other.maxX(), (double)other.maxY(), (double)other.maxZ());
   }

   public BoundingBox3d(BlockPos pos) {
      this.set((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1));
   }

   public BoundingBox3d(BoundingBox3ic other) {
      this.set(
         (double)other.minX(), (double)other.minY(), (double)other.minZ(), (double)(other.maxX() + 1), (double)(other.maxY() + 1), (double)(other.maxZ() + 1)
      );
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   public BoundingBox3d(Vec3 from, Vec3 to) {
      this.set(from.x, from.y, from.z, to.x, to.y, to.z);
   }

   public BoundingBox3d(Position from, Position to) {
      this.set(from.x(), from.y(), from.z(), to.x(), to.y(), to.z());
   }

   public BoundingBox3d() {
      this.set(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d set(BoundingBox3dc other) {
      this.set(other.minX(), other.minY(), other.minZ(), other.maxX(), other.maxY(), other.maxZ());
      return this;
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d set(AABB other) {
      this.set(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
      return this;
   }

   @Contract(
      value = "_,_,_,_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3d set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
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
   public BoundingBox3d setUnchecked(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
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
   public BoundingBox3d setUnchecked(BoundingBox3dc other) {
      this.minX = other.minX();
      this.minY = other.minY();
      this.minZ = other.minZ();
      this.maxX = other.maxX();
      this.maxY = other.maxY();
      this.maxZ = other.maxZ();
      return this;
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d expandTo(Vector3dc point) {
      return this.expandTo(point.x(), point.y(), point.z());
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3d expandTo(double x, double y, double z) {
      return this.expandTo(x, y, z, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d expandTo(BoundingBox3dc other) {
      return this.expandTo(other, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d expand(double amount) {
      return this.expand(amount, amount, amount);
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3d expand(double amountX, double amountY, double amountZ) {
      return this.expand(amountX, amountY, amountZ, this);
   }

   @Contract(
      value = "_,_,_->this",
      mutates = "this"
   )
   public BoundingBox3d move(double amountX, double amountY, double amountZ) {
      return this.move(amountX, amountY, amountZ, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d intersect(BoundingBox3dc box) {
      return this.intersect(box, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d transform(Pose3dc pose) {
      return this.transform(pose, this);
   }

   @Contract(
      value = "_,_->this",
      mutates = "this"
   )
   public BoundingBox3d transform(Pose3dc pose, Matrix4d bakedMatrix) {
      return this.transform(pose, bakedMatrix, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d transform(Matrix4dc mpose) {
      return this.transform(mpose, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d transformInverse(Pose3dc pose) {
      return this.transformInverse(pose, this);
   }

   @Contract(
      value = "_,_->this",
      mutates = "this"
   )
   public BoundingBox3d transformInverse(Pose3dc pose, Matrix4d bakedMatrix) {
      return this.transformInverse(pose, bakedMatrix, this);
   }

   @Contract(
      value = "_->this",
      mutates = "this"
   )
   public BoundingBox3d transformInverse(Matrix4dc mpose) {
      return this.transformInverse(mpose, this);
   }

   @Override
   public double minX() {
      return this.minX;
   }

   @Override
   public double minY() {
      return this.minY;
   }

   @Override
   public double minZ() {
      return this.minZ;
   }

   @Override
   public double maxX() {
      return this.maxX;
   }

   @Override
   public double maxY() {
      return this.maxY;
   }

   @Override
   public double maxZ() {
      return this.maxZ;
   }
}
