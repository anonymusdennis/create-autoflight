package dev.ryanhcode.sable.neoforge.mixin.compatibility.pmweather;

import dev.protomanly.pmweather.block.AnemometerBlock;
import dev.protomanly.pmweather.weather.WindEngine;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({AnemometerBlock.class})
public class AnemometerBlockMixin {
   @Redirect(
      method = {"useWithoutItem"},
      at = @At(
         value = "INVOKE",
         target = "Ldev/protomanly/pmweather/weather/WindEngine;getWind(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/phys/Vec3;"
      )
   )
   private Vec3 sable$redirectGetWind(BlockPos position, Level level) {
      Vec3 pos = Sable.HELPER.projectOutOfSubLevel(level, new Vec3((double)position.getX(), (double)(position.getY() + 1), (double)position.getZ()));
      return WindEngine.getWind(pos, level);
   }
}
