package dev.ryanhcode.sable.companion.impl;

import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

@SableCompanion.LoadPriority(500)
@Internal
public final class DefaultSableCompanion implements SableCompanion {
   @Override
   public Iterable<SubLevelAccess> getAllIntersecting(Level level, BoundingBox3dc bounds) {
      return List.of();
   }

   @Nullable
   @Override
   public SubLevelAccess getContaining(Level level, int chunkX, int chunkZ) {
      return null;
   }

   @Override
   public Vector3d projectOutOfSubLevel(Level level, Vector3dc pos, Vector3d dest) {
      return dest.set(pos);
   }

   @Override
   public Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
      return pos;
   }

   @Override
   public Vec3 projectOutOfSubLevel(Level level, Position pos) {
      return pos instanceof Vec3 vec3 ? vec3 : new Vec3(pos.x(), pos.y(), pos.z());
   }

   @Nullable
   @Override
   public <T, S extends SubLevelAccess> T runIncludingSubLevels(
      Level level, Vec3 origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, T> converter
   ) {
      return shouldCheckOrigin ? converter.apply(subLevel, BlockPos.containing(origin)) : null;
   }

   @Nullable
   @Override
   public <T, S extends SubLevelAccess> T runIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, T> converter
   ) {
      return shouldCheckOrigin ? converter.apply(subLevel, BlockPos.containing(origin)) : null;
   }

   @Override
   public <S extends SubLevelAccess> boolean findIncludingSubLevels(
      Level level, Vec3 origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, Boolean> converter
   ) {
      return shouldCheckOrigin ? converter.apply(subLevel, BlockPos.containing(origin)) : false;
   }

   @Override
   public <S extends SubLevelAccess> boolean findIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, Boolean> converter
   ) {
      return shouldCheckOrigin ? converter.apply(subLevel, BlockPos.containing(origin)) : false;
   }

   @Override
   public double distanceSquaredWithSubLevels(Level level, Vector3dc a, Vector3dc b) {
      return a.distanceSquared(b);
   }

   @Override
   public double distanceSquaredWithSubLevels(Level level, Vector3dc a, double bX, double bY, double bZ) {
      return a.distanceSquared(bX, bY, bZ);
   }

   @Override
   public double distanceSquaredWithSubLevels(Level level, Position a, Position b) {
      double d0 = a.x() - b.x();
      double d1 = b.y() - b.y();
      double d2 = b.z() - b.z();
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   @Override
   public double distanceSquaredWithSubLevels(Level level, Position a, double bX, double bY, double bZ) {
      double d0 = a.x() - bX;
      double d1 = a.y() - bY;
      double d2 = a.z() - bZ;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   @Override
   public double distanceSquaredWithSubLevels(Level level, double aX, double aY, double aZ, double bX, double bY, double bZ) {
      double d0 = aX - bX;
      double d1 = aY - bY;
      double d2 = aZ - bZ;
      return d0 * d0 + d1 * d1 + d2 * d2;
   }

   @Override
   public double rectilinearDistanceWithSubLevels(Level level, Vector3dc a, Vector3dc b) {
      return this.rectilinearDistanceWithSubLevels(level, a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
   }

   @Override
   public double rectilinearDistanceWithSubLevels(Level level, Position a, Position b) {
      return this.rectilinearDistanceWithSubLevels(level, a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
   }

   @Override
   public double rectilinearDistanceWithSubLevels(Level level, Vector3dc a, double bX, double bY, double bZ) {
      return this.rectilinearDistanceWithSubLevels(level, a.x(), a.y(), a.z(), bX, bY, bZ);
   }

   @Override
   public double rectilinearDistanceWithSubLevels(Level level, Position a, double bX, double bY, double bZ) {
      return this.rectilinearDistanceWithSubLevels(level, a.x(), a.y(), a.z(), bX, bY, bZ);
   }

   @Override
   public double rectilinearDistanceWithSubLevels(Level level, double aX, double aY, double aZ, double bX, double bY, double bZ) {
      double d0 = Math.abs(aX - bX);
      double d1 = Math.abs(aY - bY);
      double d2 = Math.abs(aZ - bZ);
      return Math.max(d0, Math.max(d1, d2));
   }

   @Override
   public Vector3d getVelocity(Level level, Vector3dc pos, Vector3d dest) {
      return dest.zero();
   }

   @Override
   public Vec3 getVelocity(Level level, Vec3 pos) {
      return Vec3.ZERO;
   }

   @Override
   public Vec3 getVelocity(Level level, Position pos) {
      return Vec3.ZERO;
   }

   @Override
   public Vector3d getVelocity(Level level, SubLevelAccess subLevel, Vector3dc pos, Vector3d dest) {
      return dest.zero();
   }

   @Override
   public Vec3 getVelocity(Level level, SubLevelAccess subLevel, Vec3 pos) {
      return Vec3.ZERO;
   }

   @Override
   public Vec3 getVelocity(Level level, SubLevelAccess subLevel, Position pos) {
      return Vec3.ZERO;
   }

   @Override
   public Vector3d getVelocityRelativeToAir(Level level, Vector3dc pos, Vector3d dest) {
      return dest.zero();
   }

   @Override
   public Vec3 getVelocityRelativeToAir(Level level, Vec3 pos) {
      return Vec3.ZERO;
   }

   @Override
   public Vec3 getVelocityRelativeToAir(Level level, Position pos) {
      return Vec3.ZERO;
   }

   @Override
   public boolean isInPlotGrid(Level level, int chunkX, int chunkZ) {
      return false;
   }

   @Nullable
   @Override
   public SubLevelAccess getTrackingSubLevel(Entity entity) {
      return null;
   }

   @Nullable
   @Override
   public SubLevelAccess getLastTrackingSubLevel(Entity entity) {
      return null;
   }

   @Nullable
   @Override
   public SubLevelAccess getTrackingOrVehicleSubLevel(Entity entity) {
      return null;
   }

   @Nullable
   @Override
   public SubLevelAccess getVehicleSubLevel(Entity entity) {
      return null;
   }

   @Override
   public Vec3 getEyePositionInterpolated(Entity entity, float partialTicks) {
      return entity.getEyePosition(partialTicks);
   }

   @NotNull
   @Override
   public Vector3d getFeetPos(Entity entity, float distanceDown) {
      return this.getFeetPos(entity, distanceDown, null);
   }

   @Override
   public Level getClientLevel() {
      return DefaultSableCompanion.DistHelper.getClientLevel();
   }

   private static final class DistHelper {
      public static Level getClientLevel() {
         return Minecraft.getInstance().level;
      }
   }
}
