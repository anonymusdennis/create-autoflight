package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.block_breakers;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

public class SubLevelBlockBreakingUtility {
   public static BlockPos findBreakingPos(
      BiPredicate<BlockPos, BlockState> canBreak, @Nullable SubLevel subLevel, Level level, Vec3 drillFacingVec, Vec3 center, BlockPos breakingPos
   ) {
      double scaleDown = 0.125;
      BoundingBox3d localMiningBox = new BoundingBox3d(
         new AABB(center.x - 0.5, center.y - 0.5, center.z - 0.5, center.x + 0.5, center.y + 0.5, center.z + 0.5)
            .inflate(-0.125)
            .move(drillFacingVec.scale(0.625))
      );
      BoundingBox3d globalMiningBox = new BoundingBox3d(localMiningBox);
      if (subLevel != null) {
         globalMiningBox.transform(subLevel.logicalPose(), globalMiningBox);
      }

      BoundingBox3i globalBlockMiningBox = new BoundingBox3i(globalMiningBox);
      BoundingBox3d otherLocalMiningBox = new BoundingBox3d();
      ObjectList<BlockPos> possiblyBreakableBlocks = new ObjectArrayList();
      collectBlocksInBounds(canBreak, level, BlockPos.containing(center), globalBlockMiningBox, possiblyBreakableBlocks);

      for (SubLevel otherSubLevel : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(globalMiningBox))) {
         if (subLevel != otherSubLevel) {
            globalMiningBox.transformInverse(otherSubLevel.logicalPose(), otherLocalMiningBox);
            globalBlockMiningBox.set(otherLocalMiningBox);
            collectBlocksInBounds(canBreak, level, BlockPos.containing(center), globalBlockMiningBox, possiblyBreakableBlocks);
         }
      }

      BlockPos closestPosition = breakingPos;
      double closestDistanceSqr = Double.MAX_VALUE;
      ObjectListIterator var16 = possiblyBreakableBlocks.iterator();

      while (var16.hasNext()) {
         BlockPos possiblyBreakableBlock = (BlockPos)var16.next();
         if (Sable.HELPER.getContaining(level, possiblyBreakableBlock) != subLevel) {
            Vec3 blockCenter = Vec3.atCenterOf(possiblyBreakableBlock);
            double distanceSqr = Sable.HELPER.distanceSquaredWithSubLevels(level, center, blockCenter);
            if (distanceSqr < closestDistanceSqr) {
               closestDistanceSqr = distanceSqr;
               closestPosition = possiblyBreakableBlock;
            }
         }
      }

      return closestPosition;
   }

   @Unique
   private static void collectBlocksInBounds(
      BiPredicate<BlockPos, BlockState> canBreak,
      Level level,
      BlockPos drillPos,
      BoundingBox3i globalBlockMiningBox,
      ObjectList<BlockPos> possiblyBreakableBlocks
   ) {
      MutableBlockPos globalBlockPos = new MutableBlockPos();

      for (int x = globalBlockMiningBox.minX(); x <= globalBlockMiningBox.maxX(); x++) {
         for (int z = globalBlockMiningBox.minZ(); z <= globalBlockMiningBox.maxZ(); z++) {
            for (int y = globalBlockMiningBox.minY(); y <= globalBlockMiningBox.maxY(); y++) {
               globalBlockPos.set(x, y, z);
               BlockState globalBlockState = level.getBlockState(globalBlockPos);
               if (canBreak.test(globalBlockPos, globalBlockState) && !globalBlockPos.equals(drillPos)) {
                  possiblyBreakableBlocks.add(globalBlockPos.immutable());
               }
            }
         }
      }
   }
}
