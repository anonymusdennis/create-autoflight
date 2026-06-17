package dev.engine_room.flywheel.api.visualization;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import net.minecraft.world.entity.Entity;

public interface EntityVisualizer<T extends Entity> {
   EntityVisual<? super T> createVisual(VisualizationContext var1, T var2, float var3);

   boolean skipVanillaRender(T var1);
}
