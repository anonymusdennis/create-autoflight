package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({WalkNodeEvaluator.class})
public abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {
   @Redirect(
      method = {"getStart"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getBlockY()I"
      )
   )
   private int sable$redirectGetBlockY(Mob mob) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(mob);
      return trackingSubLevel != null ? Mth.floor(trackingSubLevel.logicalPose().transformPositionInverse(mob.position()).y) : mob.getBlockY();
   }

   @Inject(
      method = {"getStart"},
      at = {@At("HEAD")}
   )
   private void sable$init(CallbackInfoReturnable<Node> cir, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      SubLevel trackingSubLevel = this.sable$getTrackingSubLevel();
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
         target = "Lnet/minecraft/world/entity/Mob;getZ()D"
      )
   )
   private double sable$redirectGetZ(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).z;
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
         target = "Lnet/minecraft/world/entity/Mob;blockPosition()Lnet/minecraft/core/BlockPos;"
      )
   )
   private BlockPos sable$redirectBlockPosition(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return BlockPos.containing((Position)mobPosition.get());
   }

   @Inject(
      method = {"canReachWithoutCollision"},
      at = {@At("HEAD")}
   )
   private void sable$canReachWithoutCollision(CallbackInfoReturnable<Boolean> cir, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      SubLevel trackingSubLevel = this.sable$getTrackingSubLevel();
      if (trackingSubLevel != null) {
         mobPosition.set(trackingSubLevel.logicalPose().transformPositionInverse(this.mob.position()));
      } else {
         mobPosition.set(this.mob.position());
      }
   }

   @Unique
   private SubLevel sable$getTrackingSubLevel() {
      return Sable.HELPER.getTrackingSubLevel(this.mob);
   }

   @Redirect(
      method = {"canReachWithoutCollision"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getX()D"
      )
   )
   private double sable$redirectGetX2(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).x;
   }

   @Redirect(
      method = {"canReachWithoutCollision"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getZ()D"
      )
   )
   private double sable$redirectGetZ2(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).z;
   }

   @Redirect(
      method = {"canReachWithoutCollision"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getY()D"
      )
   )
   private double sable$redirectGetY2(Mob mob, @Share("mobPosition") LocalRef<Vec3> mobPosition) {
      return ((Vec3)mobPosition.get()).y;
   }

   @Redirect(
      method = {"canReachWithoutCollision"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
      )
   )
   private AABB sable$canReachWithoutCollision(Mob instance) {
      SubLevel trackingSubLevel = this.sable$getTrackingSubLevel();
      return trackingSubLevel != null
         ? instance.getBoundingBox().move(trackingSubLevel.logicalPose().transformPositionInverse(this.mob.position()).subtract(this.mob.position()))
         : instance.getBoundingBox();
   }
}
