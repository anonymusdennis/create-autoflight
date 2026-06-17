package dev.ryanhcode.sable.mixin.prevent_freezing;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Biome.class})
public class BiomeMixin {
   @WrapMethod(
      method = {"shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z"}
   )
   public boolean sable$preventFreezing(LevelReader levelReader, BlockPos blockPos, boolean bl, Operation<Boolean> original) {
      if (!(Boolean)original.call(new Object[]{levelReader, blockPos, bl})) {
         return false;
      } else {
         if (levelReader instanceof Level level) {
            ActiveSableCompanion helper = Sable.HELPER;
            SubLevel parent = helper.getContaining(level, blockPos);
            BlockPos projectedPos = blockPos;
            if (parent != null) {
               projectedPos = BlockPos.containing(parent.logicalPose().transformPosition(blockPos.getCenter()));
            }

            BoundingBox3d bb3d = new BoundingBox3d(projectedPos);

            for (SubLevel subLevel : helper.getAllIntersecting(level, bb3d)) {
               if (subLevel != parent) {
                  bb3d.set(
                     (double)projectedPos.getX(),
                     (double)projectedPos.getY(),
                     (double)projectedPos.getZ(),
                     (double)(projectedPos.getX() + 1),
                     (double)(projectedPos.getY() + 1),
                     (double)(projectedPos.getZ() + 1)
                  );
                  bb3d.transformInverse(subLevel.logicalPose());
                  if (BlockPos.betweenClosedStream(bb3d.toMojang()).anyMatch(p -> !level.getBlockState(p).canBeReplaced())) {
                     return false;
                  }
               }
            }
         }

         return true;
      }
   }
}
