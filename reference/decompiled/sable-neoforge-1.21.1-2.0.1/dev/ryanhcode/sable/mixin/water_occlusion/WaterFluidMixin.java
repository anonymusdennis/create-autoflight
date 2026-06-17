package dev.ryanhcode.sable.mixin.water_occlusion;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({WaterFluid.class})
public class WaterFluidMixin {
   @WrapOperation(
      method = {"animateTick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
      )}
   )
   public void sable$addUnderwaterParticle(
      Level level, ParticleOptions particleOptions, double x, double y, double z, double g, double h, double i, Operation<Void> original
   ) {
      WaterOcclusionContainer<?> container = WaterOcclusionContainer.getContainer(level);
      if (container != null) {
         Vec3 pos = new Vec3(x, y, z);
         if (!container.isOccluded(pos)) {
            original.call(new Object[]{level, particleOptions, x, y, z, g, h, i});
         }
      }
   }
}
