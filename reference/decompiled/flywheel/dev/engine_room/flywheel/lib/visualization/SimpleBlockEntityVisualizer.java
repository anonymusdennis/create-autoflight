package dev.engine_room.flywheel.lib.visualization;

import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.BlockEntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

public final class SimpleBlockEntityVisualizer<T extends BlockEntity> implements BlockEntityVisualizer<T> {
   private final SimpleBlockEntityVisualizer.Factory<T> visualFactory;
   private final Predicate<T> skipVanillaRender;

   public SimpleBlockEntityVisualizer(SimpleBlockEntityVisualizer.Factory<T> visualFactory, Predicate<T> skipVanillaRender) {
      this.visualFactory = visualFactory;
      this.skipVanillaRender = skipVanillaRender;
   }

   @Override
   public BlockEntityVisual<? super T> createVisual(VisualizationContext ctx, T blockEntity, float partialTick) {
      return this.visualFactory.create(ctx, blockEntity, partialTick);
   }

   @Override
   public boolean skipVanillaRender(T blockEntity) {
      return this.skipVanillaRender.test(blockEntity);
   }

   public static <T extends BlockEntity> SimpleBlockEntityVisualizer.Builder<T> builder(BlockEntityType<T> type) {
      return new SimpleBlockEntityVisualizer.Builder<>(type);
   }

   public static final class Builder<T extends BlockEntity> {
      private final BlockEntityType<T> type;
      @Nullable
      private SimpleBlockEntityVisualizer.Factory<T> visualFactory;
      @Nullable
      private Predicate<T> skipVanillaRender;

      public Builder(BlockEntityType<T> type) {
         this.type = type;
      }

      public SimpleBlockEntityVisualizer.Builder<T> factory(SimpleBlockEntityVisualizer.Factory<T> visualFactory) {
         this.visualFactory = visualFactory;
         return this;
      }

      public SimpleBlockEntityVisualizer.Builder<T> skipVanillaRender(Predicate<T> skipVanillaRender) {
         this.skipVanillaRender = skipVanillaRender;
         return this;
      }

      public SimpleBlockEntityVisualizer.Builder<T> neverSkipVanillaRender() {
         this.skipVanillaRender = blockEntity -> false;
         return this;
      }

      public SimpleBlockEntityVisualizer<T> apply() {
         Objects.requireNonNull(this.visualFactory, "Visual factory cannot be null!");
         if (this.skipVanillaRender == null) {
            this.skipVanillaRender = blockEntity -> true;
         }

         SimpleBlockEntityVisualizer<T> visualizer = new SimpleBlockEntityVisualizer<>(this.visualFactory, this.skipVanillaRender);
         VisualizerRegistry.setVisualizer(this.type, visualizer);
         return visualizer;
      }
   }

   @FunctionalInterface
   public interface Factory<T extends BlockEntity> {
      BlockEntityVisual<? super T> create(VisualizationContext var1, T var2, float var3);
   }
}
