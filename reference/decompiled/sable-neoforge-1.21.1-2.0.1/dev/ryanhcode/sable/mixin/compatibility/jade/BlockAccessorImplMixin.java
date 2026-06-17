package dev.ryanhcode.sable.mixin.compatibility.jade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import snownee.jade.impl.BlockAccessorImpl;

@Mixin({BlockAccessorImpl.class})
public class BlockAccessorImplMixin {
   @WrapOperation(
      method = {"lambda$handleRequest$0"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"
      )}
   )
   private static double sable$distSqr(BlockPos instance, Vec3i vec3i, Operation<Double> original, @Local ServerLevel world) {
      return Sable.HELPER
         .distanceSquaredWithSubLevels(
            world,
            (double)instance.getX(),
            (double)instance.getY(),
            (double)instance.getZ(),
            (double)vec3i.getX() + 0.5,
            (double)vec3i.getY() + 0.5,
            (double)vec3i.getZ() + 0.5
         );
   }
}
