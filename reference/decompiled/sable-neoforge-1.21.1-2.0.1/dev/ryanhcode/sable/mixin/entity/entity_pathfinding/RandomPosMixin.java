package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({RandomPos.class})
public class RandomPosMixin {
   @Overwrite
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, int someInteger, RandomSource random, BlockPos pos) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(mob);
      Vec3 effectiveMobPos = mob.position();
      if (trackingSubLevel != null) {
         effectiveMobPos = trackingSubLevel.logicalPose().transformPositionInverse(effectiveMobPos);
      }

      int ox = pos.getX();
      int oz = pos.getZ();
      if (mob.hasRestriction() && someInteger > 1) {
         BlockPos blockPos = mob.getRestrictCenter();
         if (effectiveMobPos.x() > (double)blockPos.getX()) {
            ox -= random.nextInt(someInteger / 2);
         } else {
            ox += random.nextInt(someInteger / 2);
         }

         if (effectiveMobPos.z() > (double)blockPos.getZ()) {
            oz -= random.nextInt(someInteger / 2);
         } else {
            oz += random.nextInt(someInteger / 2);
         }
      }

      return BlockPos.containing((double)ox + effectiveMobPos.x(), (double)pos.getY() + effectiveMobPos.y(), (double)oz + effectiveMobPos.z());
   }
}
