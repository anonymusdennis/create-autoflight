package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.raycasts;

import com.simibubi.create.foundation.utility.RaycastHelper;
import com.simibubi.create.foundation.utility.RaycastHelper.PredicateTraceResult;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SableRaycastHelper {
   @Nullable
   public static PredicateTraceResult rayCastUntilWithSublevels(Level level, Vec3 start, Vec3 end, BiPredicate<SubLevel, BlockPos> predicate) {
      return rayCastUntilWithSublevels(level, start, end, pos -> predicate.test(null, pos), predicate);
   }

   @Nullable
   public static PredicateTraceResult rayCastUntilWithSublevels(Level level, Vec3 start, Vec3 end, Predicate<BlockPos> predicate) {
      return rayCastUntilWithSublevels(level, start, end, predicate, (sublevel, pos) -> predicate.test(pos));
   }

   @Nullable
   public static PredicateTraceResult rayCastUntilWithSublevels(
      Level level, Vec3 start, Vec3 end, Predicate<BlockPos> predicate, BiPredicate<SubLevel, BlockPos> subLevelPredicate
   ) {
      PredicateTraceResult closestRay = RaycastHelper.rayTraceUntil(start, end, predicate);
      double closestDistance = closestRay != null && !closestRay.missed() ? Vec3.atCenterOf(closestRay.getPos()).distanceToSqr(start) : Double.MAX_VALUE;

      for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(start, end))) {
         Vec3 plotStart;
         Vec3 plotEnd;
         if (level instanceof LevelPoseProviderExtension poseProvider) {
            plotStart = poseProvider.sable$getPose(subLevel).transformPositionInverse(start);
            plotEnd = poseProvider.sable$getPose(subLevel).transformPositionInverse(end);
         } else {
            plotStart = subLevel.logicalPose().transformPositionInverse(start);
            plotEnd = subLevel.logicalPose().transformPositionInverse(end);
         }

         PredicateTraceResult plotRay = RaycastHelper.rayTraceUntil(plotStart, plotEnd, pos -> subLevelPredicate.test(subLevel, pos));
         double plotDistance = plotRay.getPos() != null ? Vec3.atCenterOf(plotRay.getPos()).distanceToSqr(plotStart) : Double.MAX_VALUE;
         if (plotDistance < closestDistance) {
            closestRay = plotRay;
            closestDistance = plotDistance;
         }
      }

      return closestRay;
   }
}
