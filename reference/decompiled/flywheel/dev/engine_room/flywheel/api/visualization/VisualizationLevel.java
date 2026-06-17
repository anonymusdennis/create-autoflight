package dev.engine_room.flywheel.api.visualization;

import net.minecraft.world.level.LevelAccessor;

public interface VisualizationLevel extends LevelAccessor {
   default boolean supportsVisualization() {
      return true;
   }
}
