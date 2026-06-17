package dev.ryanhcode.sable.mixin.clip_overwrite;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(
   value = {BlockGetter.class},
   priority = 1100
)
public interface BlockGetterMixin {
   @Shadow
   BlockState getBlockState(BlockPos var1);

   @Overwrite
   default BlockHitResult clip(ClipContext clipContext) {
      BlockGetter self = (BlockGetter)this;
      if (!(this instanceof Level level)) {
         return originalClip(self, clipContext);
      }

      if (clipContext instanceof ClipContextExtension extension && extension.sable$doNotProject()) {
         return originalClip(self, clipContext);
      }

      SubLevel ignoredSubLevel = clipContext instanceof ClipContextExtension extension ? extension.sable$getIgnoredSubLevel() : null;
      Predicate<SubLevel> subLevelIgnoring = clipContext instanceof ClipContextExtension extensionx ? extensionx.sable$getSubLevelIgnoring() : null;
      ActiveSableCompanion helper = Sable.HELPER;
      SubLevel fromSubLevel = helper.getContaining(level, clipContext.getFrom());
      if (fromSubLevel != null) {
         Pose3dc pose = fromSubLevel.logicalPose();
         if (level instanceof LevelPoseProviderExtension extensionxx) {
            pose = extensionxx.sable$getPose(fromSubLevel);
         }

         Vector3dc from = pose.transformPosition(JOMLConversion.toJOML(clipContext.getFrom()));
         clipContext = new ClipContext(JOMLConversion.toMojang(from), clipContext.getTo(), clipContext.block, clipContext.fluid, clipContext.collisionContext);
      }

      SubLevel toSubLevel = helper.getContaining(level, clipContext.getTo());
      if (toSubLevel != null) {
         Pose3dc pose = toSubLevel.logicalPose();
         if (level instanceof LevelPoseProviderExtension extensionxx) {
            pose = extensionxx.sable$getPose(toSubLevel);
         }

         Vector3dc to = pose.transformPosition(JOMLConversion.toJOML(clipContext.getTo()));
         clipContext = new ClipContext(clipContext.getFrom(), JOMLConversion.toMojang(to), clipContext.block, clipContext.fluid, clipContext.collisionContext);
      }

      BlockHitResult minResult;
      double minDistance;
      label70: {
         minDistance = Double.MAX_VALUE;
         if (clipContext instanceof ClipContextExtension extensionxx && extensionxx.sable$isIgnoreMainLevel()) {
            Vec3 diff = clipContext.getFrom().subtract(clipContext.getTo());
            minResult = BlockHitResult.miss(clipContext.getTo(), Direction.getNearest(diff.x, diff.y, diff.z), BlockPos.containing(clipContext.getTo()));
            break label70;
         }

         minResult = originalClip(self, clipContext);
         minDistance = minResult.getLocation().distanceTo(clipContext.getFrom());
      }

      BoundingBox3d bounds = new BoundingBox3d(clipContext.getFrom(), clipContext.getTo());

      for (SubLevel subLevel : helper.getAllIntersecting(level, bounds)) {
         if (subLevel != ignoredSubLevel && (subLevelIgnoring == null || !subLevelIgnoring.test(subLevel))) {
            Pose3dc pose = subLevel.logicalPose();
            if (level instanceof LevelPoseProviderExtension extensionxx) {
               pose = extensionxx.sable$getPose(subLevel);
            }

            Vector3dc from = pose.transformPositionInverse(JOMLConversion.toJOML(clipContext.getFrom()));
            Vector3dc to = pose.transformPositionInverse(JOMLConversion.toJOML(clipContext.getTo()));
            if (helper.getContaining(level, from) == subLevel) {
               ClipContext subClipContext = new ClipContext(
                  JOMLConversion.toMojang(from), JOMLConversion.toMojang(to), clipContext.block, clipContext.fluid, clipContext.collisionContext
               );
               BlockHitResult subResult = originalClip(subLevel.getLevel(), subClipContext);
               double distance = subResult.getLocation().distanceTo(subClipContext.getFrom());
               if ((distance < minDistance || minResult.getType() == Type.MISS) && subResult.getType() != Type.MISS) {
                  minResult = subResult;
                  minDistance = distance;
               }
            }
         }
      }

      return minResult;
   }

   @Unique
   @NotNull
   private static BlockHitResult originalClip(BlockGetter level, ClipContext clipContext) {
      return (BlockHitResult)BlockGetter.traverseBlocks(clipContext.getFrom(), clipContext.getTo(), clipContext, (clipContextx, blockPos) -> {
         BlockState blockState = level.getBlockState(blockPos);
         FluidState fluidState = level.getFluidState(blockPos);
         Vec3 vec3 = clipContextx.getFrom();
         Vec3 vec32 = clipContextx.getTo();
         VoxelShape voxelShape = clipContextx.getBlockShape(blockState, level, blockPos);
         BlockHitResult blockHitResult = level.clipWithInteractionOverride(vec3, vec32, blockPos, voxelShape, blockState);
         VoxelShape voxelShape2 = clipContextx.getFluidShape(fluidState, level, blockPos);
         BlockHitResult blockHitResult2 = voxelShape2.clip(vec3, vec32, blockPos);
         double d = blockHitResult == null ? Double.MAX_VALUE : clipContextx.getFrom().distanceToSqr(blockHitResult.getLocation());
         double e = blockHitResult2 == null ? Double.MAX_VALUE : clipContextx.getFrom().distanceToSqr(blockHitResult2.getLocation());
         return d <= e ? blockHitResult : blockHitResult2;
      }, clipContextx -> {
         Vec3 vec3 = clipContextx.getFrom().subtract(clipContextx.getTo());
         return BlockHitResult.miss(clipContextx.getTo(), Direction.getNearest(vec3.x, vec3.y, vec3.z), BlockPos.containing(clipContextx.getTo()));
      });
   }
}
