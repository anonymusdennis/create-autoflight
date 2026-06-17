package dev.engine_room.flywheel.impl.visualization;

import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class VisualizationEventHandler {
   private VisualizationEventHandler() {
   }

   public static void onClientTick(Minecraft minecraft, Level level) {
      if (minecraft.player != null) {
         VisualizationManagerImpl manager = VisualizationManagerImpl.get(level);
         if (manager != null) {
            manager.tick();
         }
      }
   }

   public static void onEntityJoinLevel(Level level, Entity entity) {
      VisualizationManager manager = VisualizationManager.get(level);
      if (manager != null) {
         manager.entities().queueAdd(entity);
      }
   }

   public static void onEntityLeaveLevel(Level level, Entity entity) {
      VisualizationManager manager = VisualizationManager.get(level);
      if (manager != null) {
         manager.entities().queueRemove(entity);
      }
   }
}
