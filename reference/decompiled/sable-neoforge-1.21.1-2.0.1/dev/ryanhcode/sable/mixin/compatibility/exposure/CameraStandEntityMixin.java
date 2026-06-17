package dev.ryanhcode.sable.mixin.compatibility.exposure;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.mortuusars.exposure.world.entity.CameraStandEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CameraStandEntity.class})
public abstract class CameraStandEntityMixin extends Entity {
   public CameraStandEntityMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   public Vec3 getEyePosition(float f) {
      return Sable.HELPER.projectOutOfSubLevel(this.level(), super.getEyePosition(f));
   }

   @WrapOperation(
      method = {"isInInteractionRange"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 sable$isInInteractionRange(LivingEntity instance, Operation<Vec3> original) {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      return subLevel != null ? subLevel.logicalPose().transformPositionInverse(instance.getEyePosition()) : (Vec3)original.call(new Object[]{instance});
   }
}
