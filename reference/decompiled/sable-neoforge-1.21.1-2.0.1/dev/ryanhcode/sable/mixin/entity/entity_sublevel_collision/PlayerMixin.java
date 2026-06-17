package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.CanFallAtleastHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Player.class})
public abstract class PlayerMixin extends LivingEntity {
   @Shadow
   public float bob;
   @Shadow
   @Final
   private Abilities abilities;

   @Shadow
   protected abstract boolean isStayingOnGroundSurface();

   @Shadow
   protected abstract boolean isAboveGround(float var1);

   @Shadow
   protected abstract boolean canFallAtLeast(double var1, double var3, float var5);

   protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"maybeBackOffFromEdge"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$maybeBackOffFromEdge(Vec3 movement, MoverType moverType, CallbackInfoReturnable<Vec3> cir) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this);
      if (trackingSubLevel != null) {
         float maxUpStep = this.maxUpStep();
         if (!this.abilities.flying
            && !(movement.y > 0.0)
            && (moverType == MoverType.SELF || moverType == MoverType.PLAYER)
            && this.isStayingOnGroundSurface()
            && this.isAboveGround(maxUpStep)) {
            Pose3dc pose = trackingSubLevel.lastPose();
            double originalYaw = pose.orientation().getEulerAnglesYXZ(new Vector3d()).y;
            Quaterniondc frameOrientation = new Quaterniond().rotateY(originalYaw);
            Vector3dc localMovement = frameOrientation.transformInverse(new Vector3d(movement.x, 0.0, movement.z));
            double xMovement = localMovement.x();
            double zMovement = localMovement.z();
            double step = 0.05;
            double signedStep = Math.signum(xMovement) * 0.05;

            double i;
            for (i = Math.signum(zMovement) * 0.05;
               xMovement != 0.0 && this.sable$wouldSlideOff(xMovement, 0.0, maxUpStep, frameOrientation);
               xMovement -= signedStep
            ) {
               if (Math.abs(xMovement) <= 0.05) {
                  xMovement = 0.0;
                  break;
               }
            }

            while (zMovement != 0.0 && this.sable$wouldSlideOff(0.0, zMovement, maxUpStep, frameOrientation)) {
               if (Math.abs(zMovement) <= 0.05) {
                  zMovement = 0.0;
                  break;
               }

               zMovement -= i;
            }

            while (xMovement != 0.0 && zMovement != 0.0 && this.sable$wouldSlideOff(xMovement, zMovement, maxUpStep, frameOrientation)) {
               if (Math.abs(xMovement) <= 0.05) {
                  xMovement = 0.0;
               } else {
                  xMovement -= signedStep;
               }

               if (Math.abs(zMovement) <= 0.05) {
                  zMovement = 0.0;
               } else {
                  zMovement -= i;
               }
            }

            Vector3d globalMovement = frameOrientation.transform(new Vector3d(xMovement, 0.0, zMovement));
            Vec3 finalMovement = new Vec3(globalMovement.x, movement.y, globalMovement.z);
            cir.setReturnValue(finalMovement);
         }
      }
   }

   @Unique
   private boolean sable$wouldSlideOff(double localXMovement, double localZMovement, float fallDistance, Quaterniondc frameOrientation) {
      Vector3d movement = new Vector3d(localXMovement, 0.0, localZMovement);
      frameOrientation.transform(movement);
      double xMovement = movement.x;
      double zMovement = movement.z;
      AABB bounds = this.getBoundingBox();
      AABB boundsToCheck = new AABB(
         bounds.minX + xMovement,
         bounds.minY - (double)fallDistance - 1.0E-5F,
         bounds.minZ + zMovement,
         bounds.maxX + xMovement,
         bounds.minY,
         bounds.maxZ + zMovement
      );
      return CanFallAtleastHelper.canFallAtleastWithSubLevels(this.level(), boundsToCheck) == null;
   }

   @Redirect(
      method = {"canFallAtLeast"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"
      )
   )
   private boolean sable$noCollision(Level level, Entity entity, AABB aabb) {
      boolean original = level.noCollision(entity, aabb);
      return !original ? false : CanFallAtleastHelper.canFallAtleastWithSubLevels(level, aabb) == null;
   }
}
