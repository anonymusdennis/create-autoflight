package dev.eriksonn.aeronautics.mixin.propeller_collision;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ContraptionCollider.class})
public abstract class ContraptionColliderMixin {
   @Shadow
   static Vec3 collide(Vec3 p_20273_, Entity e) {
      return null;
   }

   @Inject(
      method = {"collideEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;isAlive()Z"
      )}
   )
   private static void sable$removeInitialDeltaMovement(
      CallbackInfo ci,
      @Local(argsOnly = true) AbstractContraptionEntity contraptionEntity,
      @Local(ordinal = 0) Entity entity,
      @Share("previousDeltaMovement") LocalRef<Vec3> previousDeltaMovement
   ) {
      previousDeltaMovement.set(null);
   }

   @Inject(
      method = {"collideEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private static void sable$saveInitialDeltaMovement(
      CallbackInfo ci,
      @Local(argsOnly = true) AbstractContraptionEntity contraptionEntity,
      @Local(ordinal = 0) Entity entity,
      @Share("previousDeltaMovement") LocalRef<Vec3> previousDeltaMovement
   ) {
      if (contraptionEntity instanceof PropellerBearingContraptionEntity propeller) {
         PropellerBearingBlockEntity bearing = propeller.getBearingEntity();
         if (bearing != null && (double)Math.abs(bearing.getDirectionIndependentSpeed()) > 32.0) {
            previousDeltaMovement.set(entity.getDeltaMovement());
         }
      }
   }

   @Redirect(
      method = {"collideEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )
   )
   private static void sable$setDeltaMovement(Entity instance, Vec3 deltaMovement, @Share("previousDeltaMovement") LocalRef<Vec3> previousDeltaMovement) {
      if (previousDeltaMovement.get() != null) {
         instance.setDeltaMovement(deltaMovement.lerp((Vec3)previousDeltaMovement.get(), 0.75));
      } else {
         instance.setDeltaMovement(deltaMovement);
      }
   }

   @Redirect(
      method = {"collideEntities"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/contraptions/ContraptionCollider;collide(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private static Vec3 sable$overrideCollisionStrength(
      Vec3 vec,
      Entity entity,
      @Local(argsOnly = true) AbstractContraptionEntity contraptionEntity,
      @Share("previousDeltaMovement") LocalRef<Vec3> previousDeltaMovement
   ) {
      return previousDeltaMovement.get() != null ? collide(vec, entity).scale(0.2) : collide(vec, entity);
   }
}
