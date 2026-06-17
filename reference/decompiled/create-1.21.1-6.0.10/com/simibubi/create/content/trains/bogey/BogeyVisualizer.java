package com.simibubi.create.content.trains.bogey;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;

@FunctionalInterface
public interface BogeyVisualizer {
   BogeyVisual createVisual(VisualizationContext var1, float var2, boolean var3);
}
