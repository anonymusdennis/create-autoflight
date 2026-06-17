package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ViewArea.class})
public class ViewAreaMixin {
   @Inject(
      method = {"setDirty"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$setDirty(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
      SubLevelContainer plotContainer = ((SubLevelContainerHolder)Minecraft.getInstance().level).sable$getPlotContainer();
      LevelPlot plot = plotContainer.getPlot(x, z);
      if (plot != null) {
         ((ClientSubLevel)plot.getSubLevel()).getRenderData().setDirty(x, y, z, playerChanged);
         ci.cancel();
      }
   }
}
