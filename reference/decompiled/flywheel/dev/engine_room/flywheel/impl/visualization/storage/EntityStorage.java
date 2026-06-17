package dev.engine_room.flywheel.impl.visualization.storage;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class EntityStorage extends Storage<Entity> {
   protected EntityVisual<?> createRaw(VisualizationContext context, Entity obj, float partialTick) {
      EntityVisualizer<Entity> visualizer = VisualizationHelper.getVisualizer(obj);
      return visualizer == null ? null : visualizer.createVisual(context, obj, partialTick);
   }

   public boolean willAccept(Entity entity) {
      if (!entity.isAlive()) {
         return false;
      } else if (!VisualizationHelper.canVisualize(entity)) {
         return false;
      } else {
         Level level = entity.level();
         return level != null;
      }
   }
}
