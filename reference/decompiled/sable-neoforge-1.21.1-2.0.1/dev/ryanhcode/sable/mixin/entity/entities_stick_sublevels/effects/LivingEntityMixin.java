package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.effects;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   @WrapOperation(
      method = {"checkFallDamage"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;blockPosition()Lnet/minecraft/core/BlockPos;"
      )}
   )
   private BlockPos sable$fallDamageParticlesPosition(LivingEntity instance, Operation<BlockPos> original, @Local(argsOnly = true) BlockPos blockPos) {
      return Sable.HELPER.getContaining(instance.level(), blockPos) != null ? blockPos : (BlockPos)original.call(new Object[]{instance});
   }
}
