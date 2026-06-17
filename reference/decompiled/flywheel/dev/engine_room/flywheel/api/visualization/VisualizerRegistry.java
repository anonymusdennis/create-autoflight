package dev.engine_room.flywheel.api.visualization;

import dev.engine_room.flywheel.api.internal.FlwApiLink;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class VisualizerRegistry {
   private VisualizerRegistry() {
   }

   @Nullable
   public static <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(BlockEntityType<T> type) {
      return FlwApiLink.INSTANCE.getVisualizer(type);
   }

   @Nullable
   public static <T extends Entity> EntityVisualizer<? super T> getVisualizer(EntityType<T> type) {
      return FlwApiLink.INSTANCE.getVisualizer(type);
   }

   public static <T extends BlockEntity> void setVisualizer(BlockEntityType<T> type, @Nullable BlockEntityVisualizer<? super T> visualizer) {
      FlwApiLink.INSTANCE.setVisualizer(type, visualizer);
   }

   public static <T extends Entity> void setVisualizer(EntityType<T> type, @Nullable EntityVisualizer<? super T> visualizer) {
      FlwApiLink.INSTANCE.setVisualizer(type, visualizer);
   }
}
