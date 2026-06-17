package dev.ryanhcode.sable.mixin.entity.entity_interaction;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ProjectileUtil.class})
public class ProjectileUtilMixin {
   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )
   )
   private static double sable$fixDistance(Vec3 start, Vec3 hitPos, @Local(argsOnly = true) Entity source) {
      return Sable.HELPER.distanceSquaredWithSubLevels(source.level(), start, hitPos);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )
   )
   private static double sable$fixDistance2(Vec3 start, Vec3 hitPos, @Local(argsOnly = true) Level level) {
      return Sable.HELPER.distanceSquaredWithSubLevels(level, start, hitPos);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )
   )
   private static Optional<Vec3> sable$getBoundingBox(
      AABB toClip, Vec3 start, Vec3 end, @Local(argsOnly = true) Entity source, @Local(ordinal = 2) Entity clipping
   ) {
      ActiveSableCompanion helper = Sable.HELPER;
      return sable$getHitPosWithSublevels(
         source.level(), toClip, start, end, helper.getContaining(source.level(), start), helper.getContaining(clipping.level(), clipping.position())
      );
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )
   )
   private static Optional<Vec3> sable$getBoundingBox2(
      AABB toClip, Vec3 start, Vec3 end, @Local(argsOnly = true) Level level, @Local(ordinal = 2) Entity clipping
   ) {
      ActiveSableCompanion helper = Sable.HELPER;
      return sable$getHitPosWithSublevels(
         level, toClip, start, end, helper.getContaining(level, start), helper.getContaining(clipping.level(), clipping.position())
      );
   }

   @Unique
   @NotNull
   private static Optional<Vec3> sable$getHitPosWithSublevels(
      Level level, AABB toClip, Vec3 start, Vec3 end, SubLevel sourceSubLevel, SubLevel clippingSubLevel
   ) {
      if (sourceSubLevel == clippingSubLevel) {
         return toClip.clip(start, end);
      } else {
         if (level instanceof LevelPoseProviderExtension poseProvider) {
            if (sourceSubLevel != null) {
               start = poseProvider.sable$getPose(sourceSubLevel).transformPosition(start);
               end = poseProvider.sable$getPose(sourceSubLevel).transformPosition(end);
            }

            if (clippingSubLevel != null) {
               start = poseProvider.sable$getPose(clippingSubLevel).transformPositionInverse(start);
               end = poseProvider.sable$getPose(clippingSubLevel).transformPositionInverse(end);
            }
         } else {
            if (sourceSubLevel != null) {
               start = sourceSubLevel.logicalPose().transformPosition(start);
               end = sourceSubLevel.logicalPose().transformPosition(end);
            }

            if (clippingSubLevel != null) {
               start = clippingSubLevel.logicalPose().transformPositionInverse(start);
               end = clippingSubLevel.logicalPose().transformPositionInverse(end);
            }
         }

         return toClip.clip(start, end);
      }
   }
}
