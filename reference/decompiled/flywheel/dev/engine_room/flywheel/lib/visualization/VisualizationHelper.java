package dev.engine_room.flywheel.lib.visualization;

import dev.engine_room.flywheel.api.visual.Effect;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class VisualizationHelper {
   private VisualizationHelper() {
   }

   public static void queueAdd(Effect effect) {
      VisualizationManager manager = VisualizationManager.get(effect.level());
      if (manager != null) {
         manager.effects().queueAdd(effect);
      }
   }

   public static void queueRemove(Effect effect) {
      VisualizationManager manager = VisualizationManager.get(effect.level());
      if (manager != null) {
         manager.effects().queueRemove(effect);
      }
   }

   public static void queueUpdate(BlockEntity blockEntity) {
      Level level = blockEntity.getLevel();
      VisualizationManager manager = VisualizationManager.get(level);
      if (manager != null) {
         manager.blockEntities().queueUpdate(blockEntity);
      }
   }

   public static void queueUpdate(Entity entity) {
      Level level = entity.level();
      VisualizationManager manager = VisualizationManager.get(level);
      if (manager != null) {
         manager.entities().queueUpdate(entity);
      }
   }

   public static void queueUpdate(Effect effect) {
      VisualizationManager manager = VisualizationManager.get(effect.level());
      if (manager != null) {
         manager.effects().queueUpdate(effect);
      }
   }

   @Nullable
   public static <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(T blockEntity) {
      return VisualizerRegistry.getVisualizer(blockEntity.getType());
   }

   @Nullable
   public static <T extends Entity> EntityVisualizer<? super T> getVisualizer(T entity) {
      return VisualizerRegistry.getVisualizer(entity.getType());
   }

   public static <T extends BlockEntity> boolean canVisualize(T blockEntity) {
      return getVisualizer(blockEntity) != null;
   }

   public static <T extends Entity> boolean canVisualize(T entity) {
      return getVisualizer(entity) != null;
   }

   public static <T extends BlockEntity> boolean skipVanillaRender(T blockEntity) {
      BlockEntityVisualizer<? super T> visualizer = getVisualizer(blockEntity);
      return visualizer == null ? false : visualizer.skipVanillaRender(blockEntity);
   }

   public static <T extends Entity> boolean skipVanillaRender(T entity) {
      EntityVisualizer<? super T> visualizer = getVisualizer(entity);
      return visualizer == null ? false : visualizer.skipVanillaRender(entity);
   }

   public static <T extends BlockEntity> boolean tryAddBlockEntity(T blockEntity) {
      Level level = blockEntity.getLevel();
      VisualizationManager manager = VisualizationManager.get(level);
      if (manager == null) {
         return false;
      } else {
         BlockEntityVisualizer<? super T> visualizer = getVisualizer(blockEntity);
         if (visualizer == null) {
            return false;
         } else {
            manager.blockEntities().queueAdd(blockEntity);
            return visualizer.skipVanillaRender(blockEntity);
         }
      }
   }
}
