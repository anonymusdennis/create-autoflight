package dev.ryanhcode.sable.api.block;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface BlockSubLevelLiftProvider {
   Direction[] DIRECTIONS = Direction.values();
   Vector3d LIFT_FORCE = new Vector3d();
   Vector3d LIFT_POS = new Vector3d();
   Vector3d LIFT_NORMAL = new Vector3d();
   Vector3d LIFT_VELO = new Vector3d();
   Vector3d DRAG = new Vector3d();
   Vector3d TEMP = new Vector3d();

   static void resetVectors() {
      LIFT_VELO.set(0.0, 0.0, 0.0);
      LIFT_POS.set(0.0, 0.0, 0.0);
      LIFT_FORCE.set(0.0, 0.0, 0.0);
      LIFT_NORMAL.set(0.0, 0.0, 0.0);
      DRAG.set(0.0, 0.0, 0.0);
   }

   static List<BlockSubLevelLiftProvider.LiftProviderGroup> groupLiftProviders(Collection<BlockSubLevelLiftProvider.LiftProviderContext> liftProviders) {
      List<BlockSubLevelLiftProvider.LiftProviderGroup> groups = new ObjectArrayList();
      Set<BlockPos> positions = new ObjectOpenHashSet(liftProviders.size());

      for (BlockSubLevelLiftProvider.LiftProviderContext liftProvider : liftProviders) {
         positions.add(liftProvider.pos);
      }

      while (!positions.isEmpty()) {
         Set<BlockPos> groupBlocks = new ObjectOpenHashSet();
         List<BlockPos> toVisit = new ObjectArrayList();
         toVisit.add(positions.iterator().next());

         while (!toVisit.isEmpty()) {
            BlockPos pos = toVisit.removeLast();
            if (!groupBlocks.contains(pos)) {
               groupBlocks.add(pos);
               positions.remove(pos);

               for (Direction direction : DIRECTIONS) {
                  BlockPos offsetPos = pos.relative(direction);
                  if (positions.contains(offsetPos)) {
                     toVisit.add(offsetPos);
                  }
               }
            }
         }

         groups.add(new BlockSubLevelLiftProvider.LiftProviderGroup(groupBlocks));
      }

      return groups;
   }

   @NotNull
   Direction sable$getNormal(BlockState var1);

   default float sable$getParallelDragScalar() {
      return 0.75F;
   }

   default float sable$getDirectionlessDragScalar() {
      return 0.068882026F;
   }

   default float sable$getLiftScalar() {
      return 0.475F;
   }

   default void sable$contributeLiftAndDrag(
      BlockSubLevelLiftProvider.LiftProviderContext ctx,
      ServerSubLevel subLevel,
      @NotNull Pose3d localPose,
      double timeStep,
      Vector3dc linearVelocity,
      Vector3dc angularVelocity,
      Vector3d linearImpulse,
      Vector3d angularImpulse,
      @Nullable BlockSubLevelLiftProvider.LiftProviderGroup group
   ) {
      resetVectors();
      LIFT_NORMAL.set(ctx.dir.x(), ctx.dir.y(), ctx.dir.z());
      LIFT_POS.set((double)ctx.pos.getX() + 0.5, (double)ctx.pos.getY() + 0.5, (double)ctx.pos.getZ() + 0.5);
      if (localPose != null) {
         localPose.transformNormal(LIFT_NORMAL);
         localPose.transformPosition(LIFT_POS);
      }

      Pose3d pose = subLevel.logicalPose();
      double pressure = DimensionPhysicsData.getAirPressure(subLevel.getLevel(), pose.transformPosition(LIFT_POS, TEMP));
      pose.transformPosition(LIFT_POS, TEMP).sub(pose.position());
      LIFT_VELO.set(linearVelocity).add(angularVelocity.cross(TEMP, TEMP));
      pose.transformNormalInverse(LIFT_VELO);
      LIFT_FORCE.zero();
      if (this.sable$getParallelDragScalar() > 0.0F) {
         double dragStrength = LIFT_NORMAL.dot(LIFT_VELO) * (double)this.sable$getParallelDragScalar() * pressure * timeStep;
         Vector3d parallelDrag = LIFT_NORMAL.mul(dragStrength, DRAG);
         LIFT_FORCE.add(parallelDrag);
         if (group != null) {
            group.totalDrag.sub(parallelDrag);
            group.dragCenter.fma(Math.abs(dragStrength), LIFT_POS);
            group.totalDragStrength = group.totalDragStrength + Math.abs(dragStrength);
         }
      }

      if (this.sable$getDirectionlessDragScalar() > 0.0F) {
         double dragStrength = (double)this.sable$getDirectionlessDragScalar() * pressure * timeStep;
         Vector3d directionlessDrag = LIFT_VELO.mul(dragStrength, TEMP);
         LIFT_FORCE.add(directionlessDrag);
         if (group != null) {
            group.totalDrag.sub(directionlessDrag);
            group.dragCenter.fma(directionlessDrag.length(), LIFT_POS);
            group.totalDragStrength = group.totalDragStrength + directionlessDrag.length();
         }
      }

      if (this.sable$getLiftScalar() > 0.0F) {
         double liftStrength = LIFT_VELO.sub(DRAG, TEMP).length() * (double)this.sable$getLiftScalar() * pressure * timeStep;
         Vector3d lift = LIFT_NORMAL.mul(liftStrength, TEMP);
         LIFT_FORCE.add(lift);
         if (group != null) {
            group.totalLift.sub(lift);
            group.liftCenter.fma(Math.abs(liftStrength), LIFT_POS);
            group.totalLiftStrength += liftStrength;
         }
      }

      linearImpulse.sub(LIFT_FORCE);
      LIFT_POS.sub(subLevel.getMassTracker().getCenterOfMass(), TEMP);
      angularImpulse.sub(TEMP.cross(LIFT_FORCE));
      resetVectors();
   }

   public static record LiftProviderContext(BlockPos pos, BlockState state, Vec3 dir) {
   }

   public static final class LiftProviderGroup {
      private final Set<BlockPos> positions;
      private final Vector3d totalLift = new Vector3d();
      private final Vector3d liftCenter = new Vector3d();
      private final Vector3d totalDrag = new Vector3d();
      private final Vector3d dragCenter = new Vector3d();
      public double totalLiftStrength;
      public double totalDragStrength;

      public LiftProviderGroup(Set<BlockPos> positions) {
         this.positions = positions;
      }

      public Set<BlockPos> positions() {
         return this.positions;
      }

      public Vector3d totalLift() {
         return this.totalLift;
      }

      public Vector3d liftCenter() {
         return this.liftCenter;
      }

      public Vector3d totalDrag() {
         return this.totalDrag;
      }

      public Vector3d dragCenter() {
         return this.dragCenter;
      }
   }
}
