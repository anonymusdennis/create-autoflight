package dev.ryanhcode.sable.mixin.plot.lighting;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({BlockAndTintGetter.class})
public interface BlockAndTintGetterMixin {
   @Shadow
   LevelLightEngine getLightEngine();

   @Overwrite
   default int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
      LevelLightEngine engine = this.getLightEngine();
      if (this instanceof SubLevelContainerHolder holder) {
         SubLevelContainer plotContainer = holder.sable$getPlotContainer();
         if (plotContainer.getLevel() instanceof ServerLevel) {
            LevelPlot plot = plotContainer.getPlot(new ChunkPos(blockPos));
            if (plot != null) {
               engine = plot.getLightEngine();
            }
         }
      }

      return engine.getLayerListener(lightLayer).getLightValue(blockPos);
   }

   @Overwrite
   default int getRawBrightness(BlockPos blockPos, int i) {
      LevelLightEngine engine = this.getLightEngine();
      if (this instanceof SubLevelContainerHolder holder) {
         SubLevelContainer plotContainer = holder.sable$getPlotContainer();
         if (plotContainer.getLevel() instanceof ServerLevel) {
            LevelPlot plot = plotContainer.getPlot(new ChunkPos(blockPos));
            if (plot != null) {
               engine = plot.getLightEngine();
            }
         }
      }

      return engine.getRawBrightness(blockPos, i);
   }
}
