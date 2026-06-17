package dev.ryanhcode.sable.companion;

import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.ApiStatus.ScheduledForRemoval;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface SableCompanion {
   SableCompanion INSTANCE = ServiceLoader.load(SableCompanion.class).stream().max(Comparator.comparingInt(provider -> {
      Class<? extends SableCompanion> type = provider.type();
      SableCompanion.LoadPriority annotation = type.getAnnotation(SableCompanion.LoadPriority.class);
      return annotation != null ? annotation.value() : 1000;
   })).map(Provider::get).orElseThrow(() -> new RuntimeException("Failed to find any sable companion implementation"));

   @Contract(
      pure = true
   )
   Iterable<? extends SubLevelAccess> getAllIntersecting(Level var1, BoundingBox3dc var2);

   @Contract(
      pure = true
   )
   @Nullable
   SubLevelAccess getContaining(Level var1, int var2, int var3);

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, ChunkPos chunkPos) {
      return this.getContaining(level, chunkPos.x, chunkPos.z);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, SectionPos pos) {
      return this.getContaining(level, pos.getX(), pos.getZ());
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, Vec3i pos) {
      return this.getContaining(level, pos.getX() >> 4, pos.getZ() >> 4);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, Position pos) {
      return this.getContaining(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, Vector3dc pos) {
      return this.getContaining(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Level level, double blockX, double blockZ) {
      return this.getContaining(level, Mth.floor(blockX) >> 4, Mth.floor(blockZ) >> 4);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(Entity entity) {
      return this.getContaining(entity.level(), entity.chunkPosition());
   }

   @Contract(
      pure = true
   )
   @Nullable
   default SubLevelAccess getContaining(BlockEntity blockEntity) {
      return this.getContaining(blockEntity.getLevel(), blockEntity.getBlockPos());
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(int chunkX, int chunkZ) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), chunkX, chunkZ);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(ChunkPos chunkPos) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), chunkPos);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(Position pos) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), pos);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(Vector3dc pos) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), pos);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(SectionPos pos) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), pos);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(Vec3i pos) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), pos);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(double blockX, double blockZ) {
      return (ClientSubLevelAccess)this.getContaining(this.getClientLevel(), blockX, blockZ);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(Entity entity) {
      return (ClientSubLevelAccess)this.getContaining(entity);
   }

   @Contract(
      pure = true
   )
   @Nullable
   default ClientSubLevelAccess getContainingClient(BlockEntity blockEntity) {
      return (ClientSubLevelAccess)this.getContaining(blockEntity);
   }

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d projectOutOfSubLevel(Level level, Vector3d pos) {
      return this.projectOutOfSubLevel(level, pos, pos);
   }

   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   Vector3d projectOutOfSubLevel(Level var1, Vector3dc var2, Vector3d var3);

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      value = "_,_->new",
      pure = true
   )
   Vec3 projectOutOfSubLevel(Level var1, Vec3 var2);

   @Contract(
      value = "_,_->new",
      pure = true
   )
   default Vec3 projectOutOfSubLevel(Level level, Position pos) {
      return this.projectOutOfSubLevel(level, new Vec3(pos.x(), pos.y(), pos.z()));
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Nullable
   @Contract(
      pure = true
   )
   <T, S extends SubLevelAccess> T runIncludingSubLevels(Level var1, Vec3 var2, boolean var3, @Nullable S var4, BiFunction<S, BlockPos, T> var5);

   @Nullable
   @Contract(
      pure = true
   )
   default <T, S extends SubLevelAccess> T runIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, T> converter
   ) {
      return this.runIncludingSubLevels(level, new Vec3(origin.x(), origin.y(), origin.z()), shouldCheckOrigin, subLevel, converter);
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      pure = true
   )
   <S extends SubLevelAccess> boolean findIncludingSubLevels(Level var1, Vec3 var2, boolean var3, @Nullable S var4, BiFunction<S, BlockPos, Boolean> var5);

   @Contract(
      pure = true
   )
   default <S extends SubLevelAccess> boolean findIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, Boolean> converter
   ) {
      return this.findIncludingSubLevels(level, new Vec3(origin.x(), origin.y(), origin.z()), shouldCheckOrigin, subLevel, converter);
   }

   @Contract(
      pure = true
   )
   double distanceSquaredWithSubLevels(Level var1, Vector3dc var2, Vector3dc var3);

   @Contract(
      pure = true
   )
   double distanceSquaredWithSubLevels(Level var1, Position var2, Position var3);

   @Contract(
      pure = true
   )
   double distanceSquaredWithSubLevels(Level var1, Vector3dc var2, double var3, double var5, double var7);

   @Contract(
      pure = true
   )
   double distanceSquaredWithSubLevels(Level var1, Position var2, double var3, double var5, double var7);

   @Contract(
      pure = true
   )
   double distanceSquaredWithSubLevels(Level var1, double var2, double var4, double var6, double var8, double var10, double var12);

   @Contract(
      pure = true
   )
   double rectilinearDistanceWithSubLevels(Level var1, Vector3dc var2, Vector3dc var3);

   @Contract(
      pure = true
   )
   double rectilinearDistanceWithSubLevels(Level var1, Position var2, Position var3);

   @Contract(
      pure = true
   )
   double rectilinearDistanceWithSubLevels(Level var1, Vector3dc var2, double var3, double var5, double var7);

   @Contract(
      pure = true
   )
   double rectilinearDistanceWithSubLevels(Level var1, Position var2, double var3, double var5, double var7);

   @Contract(
      pure = true
   )
   double rectilinearDistanceWithSubLevels(Level var1, double var2, double var4, double var6, double var8, double var10, double var12);

   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   Vector3d getVelocity(Level var1, Vector3dc var2, Vector3d var3);

   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d getVelocity(Level level, Vector3d pos) {
      return this.getVelocity(level, pos, pos);
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      value = "_,_->new",
      pure = true
   )
   Vec3 getVelocity(Level var1, Vec3 var2);

   @Contract(
      value = "_,_->new",
      pure = true
   )
   default Vec3 getVelocity(Level level, Position pos) {
      return this.getVelocity(level, new Vec3(pos.x(), pos.y(), pos.z()));
   }

   @Contract(
      value = "_,_,_,_->param4",
      mutates = "param4"
   )
   Vector3d getVelocity(Level var1, SubLevelAccess var2, Vector3dc var3, Vector3d var4);

   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   default Vector3d getVelocity(Level level, SubLevelAccess subLevel, Vector3d pos) {
      return this.getVelocity(level, subLevel, pos, pos);
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      value = "_,_,_->new",
      pure = true
   )
   Vec3 getVelocity(Level var1, SubLevelAccess var2, Vec3 var3);

   @Contract(
      value = "_,_,_->new",
      pure = true
   )
   default Vec3 getVelocity(Level level, SubLevelAccess subLevel, Position pos) {
      return this.getVelocity(level, subLevel, new Vec3(pos.x(), pos.y(), pos.z()));
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      value = "_,_,_->param3",
      mutates = "param3"
   )
   Vector3d getVelocityRelativeToAir(Level var1, Vector3dc var2, Vector3d var3);

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      value = "_,_->param2",
      mutates = "param2"
   )
   default Vector3d getVelocityRelativeToAir(Level level, Vector3d pos) {
      return this.getVelocityRelativeToAir(level, pos, pos);
   }

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      pure = true
   )
   Vec3 getVelocityRelativeToAir(Level var1, Vec3 var2);

   @Deprecated
   @ScheduledForRemoval(
      inVersion = "2.0.0"
   )
   @Contract(
      pure = true
   )
   default Vec3 getVelocityRelativeToAir(Level level, Position pos) {
      return this.getVelocity(level, new Vec3(pos.x(), pos.y(), pos.z()));
   }

   @Contract(
      pure = true
   )
   boolean isInPlotGrid(Level var1, int var2, int var3);

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Level level, ChunkPos chunkPos) {
      return this.isInPlotGrid(level, chunkPos.x, chunkPos.z);
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Level level, SectionPos pos) {
      return this.isInPlotGrid(level, pos.getX(), pos.getZ());
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Level level, Vec3i pos) {
      return this.isInPlotGrid(level, pos.getX() >> 4, pos.getZ() >> 4);
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Level level, Position pos) {
      return this.isInPlotGrid(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Level level, Vector3dc pos) {
      return this.isInPlotGrid(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(Entity entity) {
      return this.isInPlotGrid(entity.level(), entity.chunkPosition());
   }

   @Contract(
      pure = true
   )
   default boolean isInPlotGrid(BlockEntity blockEntity) {
      return this.isInPlotGrid(blockEntity.getLevel(), blockEntity.getBlockPos());
   }

   @Contract(
      pure = true
   )
   @Nullable
   SubLevelAccess getTrackingSubLevel(Entity var1);

   @Contract(
      pure = true
   )
   @Nullable
   SubLevelAccess getLastTrackingSubLevel(Entity var1);

   @Contract(
      pure = true
   )
   @Nullable
   SubLevelAccess getTrackingOrVehicleSubLevel(Entity var1);

   @Contract(
      pure = true
   )
   @Nullable
   SubLevelAccess getVehicleSubLevel(Entity var1);

   @Contract(
      pure = true
   )
   Vec3 getEyePositionInterpolated(Entity var1, float var2);

   @Contract(
      pure = true
   )
   Vector3d getFeetPos(Entity var1, float var2);

   @Contract(
      pure = true
   )
   default Vector3d getFeetPos(Entity entity, float distanceDown, @Nullable Quaterniondc orientation) {
      Vector3d feetPos;
      if (orientation == null) {
         feetPos = JOMLConversion.toJOML(entity.position()).sub(0.0, (double)distanceDown, 0.0);
      } else {
         feetPos = JOMLConversion.toJOML(entity.position()).sub(orientation.transform(new Vector3d(0.0, (double)(distanceDown + entity.getEyeHeight()), 0.0)));
      }

      return feetPos;
   }

   @OverrideOnly
   Level getClientLevel();

   @Retention(RetentionPolicy.RUNTIME)
   public @interface LoadPriority {
      int value() default 1000;
   }
}
