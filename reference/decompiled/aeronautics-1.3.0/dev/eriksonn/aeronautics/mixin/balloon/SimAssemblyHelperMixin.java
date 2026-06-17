package dev.eriksonn.aeronautics.mixin.balloon;

import com.llamalad7.mixinextras.sugar.Local;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SimAssemblyHelper.class})
public class SimAssemblyHelperMixin {
   @Inject(
      method = {"disassembleSubLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Ldev/ryanhcode/sable/sublevel/SubLevel;getPlot()Ldev/ryanhcode/sable/sublevel/plot/LevelPlot;",
         ordinal = 1
      )}
   )
   private static void aeronautics$needsBitSet(
      Level level,
      SubLevel toDisassemble,
      BlockPos subLevelAnchor,
      BlockPos disassemblyGoal,
      Rotation rotation,
      boolean playSound,
      CallbackInfo ci,
      @Local AssemblyTransform transform
   ) {
      BalloonMap balloonMap = (BalloonMap)BalloonMap.MAP.get(level);

      for (Balloon balloon : balloonMap.getBalloons()) {
         BlockPos controllerPos = balloon.getControllerPos();
         if (toDisassemble.getPlot().contains((double)controllerPos.getX(), (double)controllerPos.getZ())) {
            balloon.setAssembling(transform);
            return;
         }
      }
   }
}
