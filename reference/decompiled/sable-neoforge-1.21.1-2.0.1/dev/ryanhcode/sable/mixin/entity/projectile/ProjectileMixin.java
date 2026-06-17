package dev.ryanhcode.sable.mixin.entity.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Projectile.class})
public abstract class ProjectileMixin extends Entity {
   public ProjectileMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @WrapOperation(
      method = {"shootFromRotation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/Projectile;shoot(DDDFF)V"
      )}
   )
   private void sable$zeroVelocityBeforeShooting(
      Projectile instance, double x, double y, double z, float velocity, float inaccuracy, Operation<Void> original, @Local(argsOnly = true) Entity shooter
   ) {
      SubLevel containing = Sable.HELPER.getVehicleSubLevel(shooter);
      if (containing == null) {
         original.call(new Object[]{instance, x, y, z, velocity, inaccuracy});
      } else {
         Vector3d out = containing.logicalPose().transformNormal(new Vector3d(x, y, z));
         original.call(new Object[]{instance, out.x, out.y, out.z, velocity, inaccuracy});
      }
   }

   @Inject(
      method = {"shootFromRotation"},
      at = {@At("TAIL")}
   )
   private void sable$shootFromRotation(Entity entity, float x, float y, float z, float i, float j, CallbackInfo ci) {
      if (entity instanceof LivingEntityMovementExtension extension) {
         this.setDeltaMovement(this.getDeltaMovement().add(JOMLConversion.toMojang(extension.sable$getInheritedVelocity())));
      }
   }
}
