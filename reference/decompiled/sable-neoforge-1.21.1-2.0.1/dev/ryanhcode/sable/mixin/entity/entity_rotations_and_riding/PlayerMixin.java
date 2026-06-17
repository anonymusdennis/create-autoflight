package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Player.class})
public abstract class PlayerMixin extends LivingEntity {
   protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
      super(entityType, level);
   }

   @Inject(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
         ordinal = 1
      )}
   )
   private void sable$storeUpDeltaMovement(
      Vec3 vec3, CallbackInfo ci, @Share("upDir") LocalRef<Vector3d> upDir, @Share("upDeltaMovement") LocalRef<Vector3d> upDeltaMovement
   ) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(this, 1.0F);
      if (orientation != null) {
         Vector3d dir = orientation.transform(new Vector3d(OrientedBoundingBox3d.UP));
         upDir.set(new Vector3d(dir));
         Vec3 deltaMovement = this.getDeltaMovement();
         upDeltaMovement.set(dir.mul(dir.dot(deltaMovement.x, deltaMovement.y, deltaMovement.z)));
      }
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;setDeltaMovement(DDD)V"
      )
   )
   private void sable$modifyTravelSetDeltaMovement(
      Player instance, double x, double y, double z, @Share("upDir") LocalRef<Vector3d> upDir, @Share("upDeltaMovement") LocalRef<Vector3d> upDeltaMovement
   ) {
      if (upDeltaMovement.get() == null) {
         instance.setDeltaMovement(x, y, z);
      } else {
         Vec3 deltaMovement = this.getDeltaMovement();
         double dot = ((Vector3d)upDir.get()).dot(deltaMovement.x, deltaMovement.y, deltaMovement.z);
         double scalar = 0.6;
         this.setDeltaMovement(
            deltaMovement.subtract(dot * ((Vector3d)upDir.get()).x, dot * ((Vector3d)upDir.get()).y, dot * ((Vector3d)upDir.get()).z)
               .add(((Vector3d)upDeltaMovement.get()).x * 0.6, ((Vector3d)upDeltaMovement.get()).y * 0.6, ((Vector3d)upDeltaMovement.get()).z * 0.6)
         );
      }
   }

   @Redirect(
      method = {"aiStep"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;minmax(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/AABB;"
      )
   )
   public AABB sable$fixRidingBoundingBox(AABB usBoundingBox, AABB vehicleBoundingBox) {
      Entity vehicle = this.getVehicle();
      SubLevel vehicleSubLevel = Sable.HELPER.getContaining(vehicle);
      if (vehicleSubLevel == null) {
         return usBoundingBox.minmax(vehicleBoundingBox);
      } else {
         BoundingBox3d bb = new BoundingBox3d(vehicleBoundingBox);
         vehicleBoundingBox = bb.transform(vehicleSubLevel.logicalPose(), bb).toMojang();
         return usBoundingBox.minmax(vehicleBoundingBox);
      }
   }
}
