package dev.ryanhcode.sable.companion.math;

import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.Vector3ic;

public final class JOMLConversion {
   public static final Vector3dc ZERO = new Vector3d();
   public static final Vector3dc ONE = new Vector3d(1.0, 1.0, 1.0);
   public static final Vector3dc HALF = new Vector3d(0.5, 0.5, 0.5);
   public static final Quaterniondc QUAT_IDENTITY = new Quaterniond();

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3d toJOML(Position vec3, Vector3d dest) {
      return dest.set(vec3.x(), vec3.y(), vec3.z());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3d toJOML(Position vec3) {
      return new Vector3d(vec3.x(), vec3.y(), vec3.z());
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector2f toJOML(Vec2 vec2, Vector2f dest) {
      return dest.set(vec2.x, vec2.y);
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector2f toJOML(Vec2 vec2) {
      return new Vector2f(vec2.x, vec2.y);
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3i toJOML(Vec3i vec3, Vector3i dest) {
      return dest.set(vec3.getX(), vec3.getY(), vec3.getZ());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3i toJOML(Vec3i vec3) {
      return new Vector3i(vec3.getX(), vec3.getY(), vec3.getZ());
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3d atLowerCornerOf(Vec3i toCopy, Vector3d dest) {
      return dest.set((double)toCopy.getX(), (double)toCopy.getY(), (double)toCopy.getZ());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3d atLowerCornerOf(Vec3i toCopy) {
      return new Vector3d((double)toCopy.getX(), (double)toCopy.getY(), (double)toCopy.getZ());
   }

   @Contract(
      value = "_,_,_,_,_->param5",
      mutates = "param5"
   )
   public static Vector3d atLowerCornerWithOffset(Vec3i toCopy, double offsetX, double offsetY, double offsetZ, Vector3d dest) {
      return dest.set((double)toCopy.getX() + offsetX, (double)toCopy.getY() + offsetY, (double)toCopy.getZ() + offsetZ);
   }

   @Contract(
      value = "_,_,_,_->new",
      pure = true
   )
   public static Vector3d atLowerCornerWithOffset(Vec3i toCopy, double offsetX, double offsetY, double offsetZ) {
      return new Vector3d((double)toCopy.getX() + offsetX, (double)toCopy.getY() + offsetY, (double)toCopy.getZ() + offsetZ);
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3d atCenterOf(Vec3i toCopy, Vector3d dest) {
      return dest.set((double)toCopy.getX() + 0.5, (double)toCopy.getY() + 0.5, (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3d atCenterOf(Vec3i toCopy) {
      return new Vector3d((double)toCopy.getX() + 0.5, (double)toCopy.getY() + 0.5, (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3d atBottomCenterOf(Vec3i toCopy, Vector3d dest) {
      return dest.set((double)toCopy.getX() + 0.5, (double)toCopy.getY(), (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3d atBottomCenterOf(Vec3i toCopy) {
      return new Vector3d((double)toCopy.getX() + 0.5, (double)toCopy.getY(), (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   public static Vector3d upFromBottomCenterOf(Vec3i toCopy, double verticalOffset, Vector3d dest) {
      return dest.set((double)toCopy.getX() + 0.5, (double)toCopy.getY() + verticalOffset, (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_,_->new",
      pure = true
   )
   public static Vector3d upFromBottomCenterOf(Vec3i toCopy, double verticalOffset) {
      return new Vector3d((double)toCopy.getX() + 0.5, (double)toCopy.getY() + verticalOffset, (double)toCopy.getZ() + 0.5);
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vec3 toMojang(Vector3dc vec3d) {
      return new Vec3(vec3d.x(), vec3d.y(), vec3d.z());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vec2 toMojang(Vector2fc vec2f) {
      return new Vec2(vec2f.x(), vec2f.y());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vec3i toMojang(Vector3ic vec3i) {
      return new Vec3i(vec3i.x(), vec3i.y(), vec3i.z());
   }

   @Contract(
      value = "_->new",
      pure = true
   )
   public static Vector3d getAABBCenter(AABB aabb) {
      return new Vector3d((aabb.minX + aabb.maxX) / 2.0, (aabb.minY + aabb.maxY) / 2.0, (aabb.minZ + aabb.maxZ) / 2.0);
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   public static Vector3d getAABBCenter(AABB aabb, Vector3d dest) {
      return dest.set((aabb.minX + aabb.maxX) / 2.0, (aabb.minY + aabb.maxY) / 2.0, (aabb.minZ + aabb.maxZ) / 2.0);
   }
}
