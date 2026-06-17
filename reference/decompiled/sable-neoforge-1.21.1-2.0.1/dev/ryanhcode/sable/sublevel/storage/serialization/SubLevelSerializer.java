package dev.ryanhcode.sable.sublevel.storage.serialization;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableConfig;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.SableNBTUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SubLevelSerializer {
   public static final boolean SUPER_DEBUG_MODE = false;

   @NotNull
   private static CompoundTag serialize(ServerSubLevel subLevel, List<UUID> dependencies) {
      CompoundTag tag = new CompoundTag();
      ServerLevelPlot plot = subLevel.getPlot();
      ListTag dependencyTags = new ListTag();

      for (UUID dependency : dependencies) {
         dependencyTags.add(NbtUtils.createUUID(dependency));
      }

      Pose3d serializedPose = new Pose3d(subLevel.logicalPose());
      Vector3dc selfCenterOfMass = subLevel.getSelfMassTracker().getCenterOfMass();
      serializedPose.position().set(subLevel.logicalPose().transformPosition(new Vector3d(selfCenterOfMass)));
      serializedPose.rotationPoint().set(selfCenterOfMass);
      tag.putUUID("uuid", subLevel.getUniqueId());
      tag.put("plot", plot.save());
      tag.put("pose", SableNBTUtils.writePose3d(serializedPose));
      tag.put("world_bounds", SableNBTUtils.writeBoundingBox(subLevel.boundingBox()));
      RigidBodyHandle handle = RigidBodyHandle.of(subLevel);
      if (handle != null) {
         Vector3d linearVelocity = handle.getLinearVelocity(new Vector3d());
         Vector3d angularVelocity = handle.getAngularVelocity(new Vector3d());
         if (linearVelocity.lengthSquared() > 0.010000000000000002) {
            tag.put("linear_velocity", SableNBTUtils.writeVector3d(linearVelocity));
         }

         if (angularVelocity.lengthSquared() > Math.toRadians(1.0)) {
            tag.put("angular_velocity", SableNBTUtils.writeVector3d(angularVelocity));
         }
      }

      if (subLevel.getName() != null) {
         tag.putString("display_name", subLevel.getName());
      }

      if (!dependencies.isEmpty()) {
         tag.put("loading_dependencies", dependencyTags);
      }

      CompoundTag userDataTag = subLevel.getUserDataTag();
      if (userDataTag != null) {
         tag.put("user_data", userDataTag);
      }

      return tag;
   }

   @Nullable
   public static SubLevelData fromData(CompoundTag tag) {
      UUID uuid = tag.getUUID("uuid");
      List<UUID> dependencies = List.of();
      if (tag.contains("loading_dependencies")) {
         ListTag dependencyUUIDS = tag.getList("loading_dependencies", 11);
         dependencies = new ObjectArrayList();

         for (Tag dependencyUUIDTag : dependencyUUIDS) {
            UUID dependencyUUID = NbtUtils.loadUUID(dependencyUUIDTag);
            dependencies.add(dependencyUUID);
         }
      }

      return new SubLevelData(
         uuid, SableNBTUtils.readBoundingBox(tag.getCompound("world_bounds")), SableNBTUtils.readPose3d(tag.getCompound("pose")), dependencies, tag
      );
   }

   public static ServerSubLevel fullyLoad(ServerLevel level, SubLevelData halfLoadedSubLevel) {
      CompoundTag tag = halfLoadedSubLevel.fullTag();
      CompoundTag plotTag = tag.getCompound("plot");
      int plotX = plotTag.getInt("plot_x");
      int plotZ = plotTag.getInt("plot_z");
      Pose3d pose = SableNBTUtils.readPose3d(tag.getCompound("pose"));
      Vector3d position = pose.position();
      Vector3d cor = pose.rotationPoint();
      if (!Double.isNaN(position.x)
         && !Double.isNaN(position.y)
         && !Double.isNaN(position.z)
         && !Double.isNaN(cor.x)
         && !Double.isNaN(cor.y)
         && !Double.isNaN(cor.z)) {
         ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);

         ServerSubLevel subLevel;
         try {
            subLevel = (ServerSubLevel)plotContainer.allocateSubLevel(halfLoadedSubLevel.uuid(), plotX, plotZ, pose);
         } catch (IllegalArgumentException var15) {
            Sable.LOGGER.error("Failed to load sub-level, skipping", halfLoadedSubLevel, var15);
            return null;
         }

         ServerLevelPlot plot = subLevel.getPlot();
         plot.load(plotTag);
         if (plot.getBoundingBox() != BoundingBox3i.EMPTY && plot.getBoundingBox().volume() > 0) {
            SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
            subLevel.logicalPose().set(pose);
            physicsSystem.getPipeline().teleport(subLevel, position, pose.orientation());
            subLevel.updateLastPose();
            Vector3dc linearVelocity = JOMLConversion.ZERO;
            Vector3dc angularVelocity = JOMLConversion.ZERO;
            if (tag.contains("linear_velocity")) {
               linearVelocity = SableNBTUtils.readVector3d(tag.getCompound("linear_velocity")).mul(SableConfig.VELOCITY_RETAINED_ON_LOAD.getAsDouble());
            }

            if (tag.contains("angular_velocity")) {
               angularVelocity = SableNBTUtils.readVector3d(tag.getCompound("angular_velocity")).mul(SableConfig.VELOCITY_RETAINED_ON_LOAD.getAsDouble());
            }

            physicsSystem.getPipeline().addLinearAndAngularVelocity(subLevel, linearVelocity, angularVelocity);
            if (tag.contains("display_name")) {
               subLevel.setName(tag.getString("display_name"));
            }

            if (tag.contains("user_data")) {
               subLevel.setUserDataTag(tag.getCompound("user_data"));
            }

            return subLevel;
         } else {
            Sable.LOGGER
               .error("Failed to load sub-level, invalid plot bounds: {}", plot.getBoundingBox() == BoundingBox3i.EMPTY ? "EMPTY" : plot.getBoundingBox());
            plotContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            return null;
         }
      } else {
         Sable.LOGGER.error("Failed to load sub-level, invalid pose: {}", pose);
         return null;
      }
   }

   public static SubLevelData toData(ServerSubLevel subLevel, @NotNull List<UUID> dependencies) {
      List<UUID> filteredDependencies = new ObjectArrayList(dependencies);
      filteredDependencies.remove(subLevel.getUniqueId());
      CompoundTag tag = serialize(subLevel, filteredDependencies);
      return new SubLevelData(subLevel.getUniqueId(), new BoundingBox3d(subLevel.boundingBox()), new Pose3d(subLevel.logicalPose()), filteredDependencies, tag);
   }
}
