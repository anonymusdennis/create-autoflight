package dev.ryanhcode.sable;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SableDistUtil;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class ActiveSableCompanion implements SableCompanion {
   public Iterable<SubLevel> getAllIntersecting(Level level, BoundingBox3dc bounds) {
      if (level instanceof SubLevelContainerHolder holder) {
         SubLevelContainer plotContainer = holder.sable$getPlotContainer();
         if (plotContainer instanceof ServerSubLevelContainer serverContainer) {
            SubLevelPhysicsSystem physicsSystem = serverContainer.physicsSystem();
            return physicsSystem.queryIntersecting(bounds);
         } else {
            return plotContainer.queryIntersecting(bounds);
         }
      } else {
         return List.of();
      }
   }

   @Nullable
   public SubLevel getContaining(Level level, int chunkX, int chunkZ) {
      if (!(level instanceof SubLevelContainerHolder)) {
         return null;
      } else {
         SubLevelContainer container = ((SubLevelContainerHolder)level).sable$getPlotContainer();
         if (container == null) {
            return null;
         } else {
            LevelPlot plot = container.getPlot(chunkX, chunkZ);
            return plot != null ? plot.getSubLevel() : null;
         }
      }
   }

   @Nullable
   public SubLevel getContaining(Level level, ChunkPos chunkPos) {
      return this.getContaining(level, chunkPos.x, chunkPos.z);
   }

   @Nullable
   public SubLevel getContaining(Level level, SectionPos pos) {
      return this.getContaining(level, pos.getX(), pos.getZ());
   }

   @Nullable
   public SubLevel getContaining(Level level, Vec3i pos) {
      return this.getContaining(level, pos.getX() >> 4, pos.getZ() >> 4);
   }

   @Nullable
   public SubLevel getContaining(Level level, Position pos) {
      return this.getContaining(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Nullable
   public SubLevel getContaining(Level level, Vector3dc pos) {
      return this.getContaining(level, Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Nullable
   public SubLevel getContaining(Level level, double blockX, double blockZ) {
      return this.getContaining(level, Mth.floor(blockX) >> 4, Mth.floor(blockZ) >> 4);
   }

   @Nullable
   public SubLevel getContaining(Entity entity) {
      ChunkPos chunkPos = entity.chunkPosition();
      return this.getContaining(entity.level(), chunkPos.x, chunkPos.z);
   }

   @Nullable
   public SubLevel getContaining(BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return this.getContaining(blockEntity.getLevel(), pos.getX() >> 4, pos.getZ() >> 4);
   }

   @Nullable
   public ClientSubLevel getContainingClient(int chunkX, int chunkZ) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), chunkX, chunkZ);
   }

   @Nullable
   public ClientSubLevel getContainingClient(ChunkPos chunkPos) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), chunkPos.x, chunkPos.z);
   }

   @Nullable
   public ClientSubLevel getContainingClient(Position pos) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Nullable
   public ClientSubLevel getContainingClient(Vector3dc pos) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), Mth.floor(pos.x()) >> 4, Mth.floor(pos.z()) >> 4);
   }

   @Nullable
   public ClientSubLevel getContainingClient(SectionPos pos) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), pos.x(), pos.z());
   }

   @Nullable
   public ClientSubLevel getContainingClient(Vec3i pos) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), pos.getX() >> 4, pos.getZ() >> 4);
   }

   @Nullable
   public ClientSubLevel getContainingClient(double blockX, double blockZ) {
      return (ClientSubLevel)this.getContaining(SableDistUtil.getClientLevel(), Mth.floor(blockX) >> 4, Mth.floor(blockZ) >> 4);
   }

   @Nullable
   public ClientSubLevel getContainingClient(Entity entity) {
      ChunkPos chunkPos = entity.chunkPosition();
      return (ClientSubLevel)this.getContaining(entity.level(), chunkPos.x, chunkPos.z);
   }

   @Nullable
   public ClientSubLevel getContainingClient(BlockEntity blockEntity) {
      BlockPos pos = blockEntity.getBlockPos();
      return (ClientSubLevel)this.getContaining(blockEntity.getLevel(), pos.getX() >> 4, pos.getZ() >> 4);
   }

   public Vector3d projectOutOfSubLevel(Level level, Vector3dc pos, Vector3d dest) {
      SubLevel subLevel = this.getContaining(level, pos);
      if (subLevel == null) {
         return dest.set(pos);
      } else {
         Pose3dc pose;
         if (level instanceof LevelPoseProviderExtension extension) {
            pose = extension.sable$getPose(subLevel);
         } else {
            pose = subLevel.logicalPose();
         }

         return pose.transformPosition(pos, dest);
      }
   }

   public Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
      return this.projectOutOfSubLevel(level, (Position)pos);
   }

   public Vec3 projectOutOfSubLevel(Level level, Position pos) {
      SubLevel subLevel = this.getContaining(level, pos);
      if (subLevel == null) {
         return pos instanceof Vec3 vec ? vec : new Vec3(pos.x(), pos.y(), pos.z());
      } else {
         Pose3dc pose;
         if (level instanceof LevelPoseProviderExtension extension) {
            pose = extension.sable$getPose(subLevel);
         } else {
            pose = subLevel.logicalPose();
         }

         return JOMLConversion.toMojang(pose.transformPosition(JOMLConversion.toJOML(pos)));
      }
   }

   @Nullable
   public <T, S extends SubLevelAccess> T runIncludingSubLevels(
      Level level, Vec3 origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, T> converter
   ) {
      return this.runIncludingSubLevels(level, (Position)origin, shouldCheckOrigin, subLevel, converter);
   }

   @Nullable
   public <T, S extends SubLevelAccess> T runIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, T> converter
   ) {
      MutableBlockPos mutableBlockPos = new MutableBlockPos(origin.x(), origin.y(), origin.z());
      Vector3d mutablePos = JOMLConversion.toJOML(origin);
      if (shouldCheckOrigin) {
         T test = converter.apply(subLevel, mutableBlockPos.immutable());
         if (test != null) {
            return test;
         }
      }

      if (subLevel != null) {
         subLevel.logicalPose().transformPosition(mutablePos);
         mutableBlockPos.set(mutablePos.x, mutablePos.y, mutablePos.z);
         T test = converter.apply(null, mutableBlockPos.immutable());
         if (test != null) {
            return test;
         }
      }

      Vec3 copyPos = JOMLConversion.toMojang(mutablePos);

      for (SubLevel otherSubLevel : this.getAllIntersecting(level, new BoundingBox3d(BlockPos.containing(JOMLConversion.toMojang(mutablePos))))) {
         if (otherSubLevel != subLevel) {
            mutablePos.set(copyPos.x, copyPos.y, copyPos.z);
            otherSubLevel.logicalPose().transformPositionInverse(mutablePos);
            mutableBlockPos.set(mutablePos.x, mutablePos.y, mutablePos.z);
            T test = converter.apply((S)otherSubLevel, mutableBlockPos.immutable());
            if (test != null) {
               return test;
            }
         }
      }

      return null;
   }

   public <S extends SubLevelAccess> boolean findIncludingSubLevels(
      Level level, Vec3 origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, Boolean> converter
   ) {
      return this.findIncludingSubLevels(level, (Position)origin, shouldCheckOrigin, subLevel, converter);
   }

   public <S extends SubLevelAccess> boolean findIncludingSubLevels(
      Level level, Position origin, boolean shouldCheckOrigin, @Nullable S subLevel, BiFunction<S, BlockPos, Boolean> converter
   ) {
      return Boolean.TRUE
         .equals(
            this.runIncludingSubLevels(
               level,
               origin,
               shouldCheckOrigin,
               subLevel,
               (candidateSublevel, pos) -> Boolean.TRUE.equals(converter.apply((S)candidateSublevel, pos)) ? true : null
            )
         );
   }

   public double distanceSquaredWithSubLevels(Level level, Vector3dc a, Vector3dc b) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, a, new Vector3d());
      Vector3dc globalB = this.projectOutOfSubLevel(level, b, new Vector3d());
      return globalA.distanceSquared(globalB);
   }

   public double distanceSquaredWithSubLevels(Level level, Position a, Position b) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(a));
      Vector3dc globalB = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(b));
      return globalA.distanceSquared(globalB);
   }

   public double distanceSquaredWithSubLevels(Level level, Vector3dc a, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, a, new Vector3d());
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return globalA.distanceSquared(globalB);
   }

   public double distanceSquaredWithSubLevels(Level level, Position a, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(a));
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return globalA.distanceSquared(globalB);
   }

   public double distanceSquaredWithSubLevels(Level level, double aX, double aY, double aZ, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, new Vector3d(aX, aY, aZ));
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return globalA.distanceSquared(globalB);
   }

   private static double rectilinearDistance(Vector3dc a, Vector3dc b) {
      double d0 = Math.abs(b.x() - a.x());
      double d1 = Math.abs(b.y() - a.y());
      double d2 = Math.abs(b.z() - a.z());
      return Math.max(d0, Math.max(d1, d2));
   }

   public double rectilinearDistanceWithSubLevels(Level level, Vector3dc a, Vector3dc b) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, a, new Vector3d());
      Vector3dc globalB = this.projectOutOfSubLevel(level, b, new Vector3d());
      return rectilinearDistance(globalA, globalB);
   }

   public double rectilinearDistanceWithSubLevels(Level level, Position a, Position b) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(a));
      Vector3dc globalB = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(b));
      return rectilinearDistance(globalA, globalB);
   }

   public double rectilinearDistanceWithSubLevels(Level level, Vector3dc a, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, a, new Vector3d());
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return rectilinearDistance(globalA, globalB);
   }

   public double rectilinearDistanceWithSubLevels(Level level, Position a, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, JOMLConversion.toJOML(a));
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return rectilinearDistance(globalA, globalB);
   }

   public double rectilinearDistanceWithSubLevels(Level level, double aX, double aY, double aZ, double bX, double bY, double bZ) {
      Vector3dc globalA = this.projectOutOfSubLevel(level, new Vector3d(aX, aY, aZ));
      Vector3dc globalB = this.projectOutOfSubLevel(level, new Vector3d(bX, bY, bZ));
      return rectilinearDistance(globalA, globalB);
   }

   public Vector3d getVelocity(Level level, Vector3dc pos, Vector3d dest) {
      SubLevel subLevel = this.getContaining(level, pos);
      return subLevel == null ? dest.zero() : this.getVelocity(level, subLevel, pos, dest);
   }

   public Vec3 getVelocity(Level level, Vec3 pos) {
      return JOMLConversion.toMojang(this.getVelocity(level, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public Vec3 getVelocity(Level level, Position pos) {
      return JOMLConversion.toMojang(this.getVelocity(level, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public Vector3d getVelocity(Level level, SubLevelAccess subLevel, Vector3dc pos, Vector3d dest) {
      Pose3dc pose = subLevel.logicalPose();
      if (subLevel instanceof ServerSubLevel serverSubLevel) {
         ServerSubLevelContainer container = SubLevelContainer.getContainer((ServerLevel)level);

         assert container != null;

         RigidBodyHandle handle = container.physicsSystem().getPhysicsHandle(serverSubLevel);
         Vector3dc linearVelocity = handle.getLinearVelocity(new Vector3d());
         Vector3dc angularVelocity = handle.getAngularVelocity(new Vector3d());
         Vector3dc localPos = pose.transformPosition(pos, dest).sub(pose.position());
         return angularVelocity.cross(localPos, dest).add(linearVelocity);
      } else {
         return pose.transformPosition(pos, new Vector3d()).sub(subLevel.lastPose().transformPosition(pos, dest), dest).mul(20.0);
      }
   }

   public Vec3 getVelocity(Level level, SubLevelAccess subLevel, Vec3 pos) {
      return JOMLConversion.toMojang(Sable.HELPER.getVelocity(level, subLevel, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public Vec3 getVelocity(Level level, SubLevelAccess subLevel, Position pos) {
      return JOMLConversion.toMojang(Sable.HELPER.getVelocity(level, subLevel, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public Vector3d getVelocityRelativeToAir(Level level, Vector3dc pos, Vector3d dest) {
      return SubLevelHelper.getVelocityRelativeToAir(level, pos, dest);
   }

   public Vec3 getVelocityRelativeToAir(Level level, Vec3 pos) {
      return JOMLConversion.toMojang(SubLevelHelper.getVelocityRelativeToAir(level, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public Vec3 getVelocityRelativeToAir(Level level, Position pos) {
      return JOMLConversion.toMojang(SubLevelHelper.getVelocityRelativeToAir(level, JOMLConversion.toJOML(pos), new Vector3d()));
   }

   public boolean isInPlotGrid(Level level, int chunkX, int chunkZ) {
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      return container != null && container.inBounds(chunkX, chunkZ);
   }

   @Nullable
   public SubLevel getTrackingSubLevel(Entity entity) {
      return ((EntityMovementExtension)entity).sable$getTrackingSubLevel();
   }

   @Nullable
   public SubLevel getLastTrackingSubLevel(Entity entity) {
      UUID uuid = ((EntityMovementExtension)entity).sable$getLastTrackingSubLevelID();
      if (uuid != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(entity.level());
         return container.getSubLevel(uuid);
      } else {
         return null;
      }
   }

   @Nullable
   public SubLevel getTrackingOrVehicleSubLevel(Entity entity) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(entity);
      if (trackingSubLevel == null) {
         trackingSubLevel = Sable.HELPER.getVehicleSubLevel(entity);
      }

      return trackingSubLevel;
   }

   @Nullable
   public SubLevel getVehicleSubLevel(Entity entity) {
      return entity.getVehicle() != null ? Sable.HELPER.getContaining(entity.getVehicle()) : null;
   }

   @NotNull
   public Vec3 getEyePositionInterpolated(Entity entity, float partialTicks) {
      if (Sable.HELPER.getTrackingOrVehicleSubLevel(entity) instanceof ClientSubLevel clientSubLevel) {
         Vector3d startPos = new Vector3d(entity.xo, entity.yo + (double)entity.getEyeHeight(), entity.zo);
         Vector3d endPos = new Vector3d(entity.getX(), entity.getY() + (double)entity.getEyeHeight(), entity.getZ());
         Pose3dc renderPose = clientSubLevel.renderPose(partialTicks);
         clientSubLevel.lastPose().transformPositionInverse(startPos);
         clientSubLevel.logicalPose().transformPositionInverse(endPos);
         startPos.lerp(endPos, (double)partialTicks);
         renderPose.transformPosition(startPos);
         return new Vec3(startPos.x, startPos.y, startPos.z);
      } else {
         return entity.getEyePosition(partialTicks);
      }
   }

   @NotNull
   public Vector3d getFeetPos(Entity entity, float distanceDown) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, 1.0F);
      return Sable.HELPER.getFeetPos(entity, distanceDown, orientation);
   }

   public Level getClientLevel() {
      throw new UnsupportedOperationException("Should not be called");
   }
}
