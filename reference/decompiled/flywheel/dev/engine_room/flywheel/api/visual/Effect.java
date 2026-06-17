package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.world.level.LevelAccessor;

public interface Effect {
   LevelAccessor level();

   EffectVisual<?> visualize(VisualizationContext var1, float var2);
}
