package dev.ryanhcode.sable.mixin.prevent_overgrowth;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({VineBlock.class})
public class VineBlockMixin {
   @WrapOperation(
      method = {"randomTick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
      )},
      require = 0
   )
   public boolean stopSpreadBeyondSubLevel(
      ServerLevel level, BlockPos spreadPos, BlockState blockState, int flags, Operation<Boolean> original, @Local(argsOnly = true) BlockPos vinePos
   ) {
      SubLevel subLevel = Sable.HELPER.getContaining(level, vinePos);
      return subLevel != null && !subLevel.getPlot().getBoundingBox().contains(spreadPos.getX(), spreadPos.getY(), spreadPos.getZ())
         ? true
         : (Boolean)original.call(new Object[]{level, spreadPos, blockState, flags});
   }
}
