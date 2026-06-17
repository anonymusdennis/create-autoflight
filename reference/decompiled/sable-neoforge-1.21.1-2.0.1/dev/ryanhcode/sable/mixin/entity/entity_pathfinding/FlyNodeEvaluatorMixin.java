package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlyNodeEvaluator.class})
public abstract class FlyNodeEvaluatorMixin extends NodeEvaluator {
   @Inject(
      method = {"getStart"},
      at = {@At("HEAD")}
   )
   private void sable$init(CallbackInfoReturnable<Node> cir, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this.mob);
      if (trackingSubLevel != null) {
         mobPosition.set(trackingSubLevel.logicalPose().transformPositionInverse(this.mob.position()));
      } else {
         mobPosition.set(this.mob.position());
      }
   }

   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getBlockY()I"
      )
   )
   private int sable$redirectGetBlockY(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return Mth.floor(((Vec3)mobPosition.get()).y);
   }

   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getX()D"
      )
   )
   private double sable$redirectGetX(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).x;
   }

   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getY()D"
      )
   )
   private double sable$redirectGetY(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).y;
   }

   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getZ()D"
      )
   )
   private double sable$redirectGetZ(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).z;
   }

   @Overwrite
   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob mob) {
      AABB mobBounds = mob.getBoundingBox();
      boolean small = mobBounds.getSize() < 1.0;
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this.mob);
      Vec3 localPosition = this.mob.position();
      if (trackingSubLevel != null) {
         localPosition = trackingSubLevel.logicalPose().transformPositionInverse(localPosition);
      }

      AABB localMobBounds = mob.getBoundingBox().move(localPosition.subtract(this.mob.position()));
      if (!small) {
         int blockY = Mth.floor(localPosition.y);
         return List.of(
            BlockPos.containing(localMobBounds.minX, (double)blockY, localMobBounds.minZ),
            BlockPos.containing(localMobBounds.minX, (double)blockY, localMobBounds.maxZ),
            BlockPos.containing(localMobBounds.maxX, (double)blockY, localMobBounds.minZ),
            BlockPos.containing(localMobBounds.maxX, (double)blockY, localMobBounds.maxZ)
         );
      } else {
         double xSize = Math.max(0.0, 1.1F - mobBounds.getXsize());
         double ySize = Math.max(0.0, 1.1F - mobBounds.getYsize());
         double zSize = Math.max(0.0, 1.1F - mobBounds.getZsize());
         AABB localBounds = localMobBounds.inflate(xSize, ySize, zSize);
         return BlockPos.randomBetweenClosed(
            mob.getRandom(),
            10,
            Mth.floor(localBounds.minX),
            Mth.floor(localBounds.minY),
            Mth.floor(localBounds.minZ),
            Mth.floor(localBounds.maxX),
            Mth.floor(localBounds.maxY),
            Mth.floor(localBounds.maxZ)
         );
      }
   }
}
