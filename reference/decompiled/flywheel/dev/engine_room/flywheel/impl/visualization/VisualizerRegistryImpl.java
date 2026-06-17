package dev.engine_room.flywheel.impl.visualization;

import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.impl.extension.BlockEntityTypeExtension;
import dev.engine_room.flywheel.impl.extension.EntityTypeExtension;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class VisualizerRegistryImpl {
   @Nullable
   public static <T extends BlockEntity> BlockEntityVisualizer<? super T> getVisualizer(BlockEntityType<T> type) {
      return ((BlockEntityTypeExtension)type).flywheel$getVisualizer();
   }

   @Nullable
   public static <T extends Entity> EntityVisualizer<? super T> getVisualizer(EntityType<T> type) {
      return ((EntityTypeExtension)type).flywheel$getVisualizer();
   }

   public static <T extends BlockEntity> void setVisualizer(BlockEntityType<T> type, @Nullable BlockEntityVisualizer<? super T> visualizer) {
      ((BlockEntityTypeExtension)type).flywheel$setVisualizer(visualizer);
   }

   public static <T extends Entity> void setVisualizer(EntityType<T> type, @Nullable EntityVisualizer<? super T> visualizer) {
      ((EntityTypeExtension)type).flywheel$setVisualizer(visualizer);
   }

   private VisualizerRegistryImpl() {
   }
}
