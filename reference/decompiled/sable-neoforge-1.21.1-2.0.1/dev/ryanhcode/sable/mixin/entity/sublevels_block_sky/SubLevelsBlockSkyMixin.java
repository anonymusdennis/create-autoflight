package dev.ryanhcode.sable.mixin.entity.sublevels_block_sky;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.entity.sublevels_block_sky.SubLevelsBlockSkyMixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({Mob.class, FleeSunGoal.class, GroundPathNavigation.class})
public class SubLevelsBlockSkyMixin {
   @WrapOperation(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;canSeeSky(Lnet/minecraft/core/BlockPos;)Z"
      )}
   )
   private boolean sable$subLevelsBlockSky(Level instance, BlockPos pos, Operation<Boolean> original) {
      boolean canSeeOriginal = (Boolean)original.call(new Object[]{instance, pos});
      return canSeeOriginal && pos.getY() < instance.getMaxBuildHeight() && SubLevelsBlockSkyMixinHelper.checkSkyWithSublevels(instance, pos)
         ? false
         : canSeeOriginal;
   }
}
