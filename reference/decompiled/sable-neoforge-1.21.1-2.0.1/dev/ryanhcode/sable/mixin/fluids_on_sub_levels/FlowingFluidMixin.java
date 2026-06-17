package dev.ryanhcode.sable.mixin.fluids_on_sub_levels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlowingFluid.class})
public class FlowingFluidMixin {
   @Inject(
      at = {@At("HEAD")},
      method = {"canSpreadTo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/material/Fluid;)Z"},
      cancellable = true
   )
   private void sable$canSpreadTo(
      BlockGetter pLevel,
      BlockPos pFromPos,
      BlockState pFromBlockState,
      Direction pDirection,
      BlockPos pToPos,
      BlockState pToBlockState,
      FluidState pToFluidState,
      Fluid pFluid,
      CallbackInfoReturnable<Boolean> cir
   ) {
      if (pLevel instanceof Level level) {
         SubLevel subLevel = Sable.HELPER.getContaining(level, Vec3.atCenterOf(pToPos));
         if (subLevel != null) {
            BlockPos mut = pToPos;
            boolean ableToFlow = false;

            while (subLevel.getPlot().getBoundingBox().contains(mut.getX(), mut.getY(), mut.getZ())) {
               mut = mut.below();
               if (mut.getY() < 0 || !pLevel.getBlockState(mut).isAir()) {
                  ableToFlow = true;
                  break;
               }
            }

            if (!ableToFlow) {
               cir.setReturnValue(false);
            }
         }
      }
   }
}
