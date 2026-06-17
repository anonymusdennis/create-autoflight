package dev.engine_room.flywheel.lib.visualization;

import dev.engine_room.flywheel.api.visual.EntityVisual;
import dev.engine_room.flywheel.api.visualization.EntityVisualizer;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizerRegistry;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

public final class SimpleEntityVisualizer<T extends Entity> implements EntityVisualizer<T> {
   private final SimpleEntityVisualizer.Factory<T> visualFactory;
   private final Predicate<T> skipVanillaRender;

   public SimpleEntityVisualizer(SimpleEntityVisualizer.Factory<T> visualFactory, Predicate<T> skipVanillaRender) {
      this.visualFactory = visualFactory;
      this.skipVanillaRender = skipVanillaRender;
   }

   @Override
   public EntityVisual<? super T> createVisual(VisualizationContext ctx, T entity, float partialTick) {
      return this.visualFactory.create(ctx, entity, partialTick);
   }

   @Override
   public boolean skipVanillaRender(T entity) {
      return this.skipVanillaRender.test(entity);
   }

   public static <T extends Entity> SimpleEntityVisualizer.Builder<T> builder(EntityType<T> type) {
      return new SimpleEntityVisualizer.Builder<>(type);
   }

   public static final class Builder<T extends Entity> {
      private final EntityType<T> type;
      @Nullable
      private SimpleEntityVisualizer.Factory<T> visualFactory;
      @Nullable
      private Predicate<T> skipVanillaRender;

      public Builder(EntityType<T> type) {
         this.type = type;
      }

      public SimpleEntityVisualizer.Builder<T> factory(SimpleEntityVisualizer.Factory<T> visualFactory) {
         this.visualFactory = visualFactory;
         return this;
      }

      public SimpleEntityVisualizer.Builder<T> skipVanillaRender(Predicate<T> skipVanillaRender) {
         this.skipVanillaRender = skipVanillaRender;
         return this;
      }

      public SimpleEntityVisualizer.Builder<T> neverSkipVanillaRender() {
         this.skipVanillaRender = entity -> false;
         return this;
      }

      public SimpleEntityVisualizer<T> apply() {
         Objects.requireNonNull(this.visualFactory, "Visual factory cannot be null!");
         if (this.skipVanillaRender == null) {
            this.skipVanillaRender = entity -> true;
         }

         SimpleEntityVisualizer<T> visualizer = new SimpleEntityVisualizer<>(this.visualFactory, this.skipVanillaRender);
         VisualizerRegistry.setVisualizer(this.type, visualizer);
         return visualizer;
      }
   }

   @FunctionalInterface
   public interface Factory<T extends Entity> {
      EntityVisual<? super T> create(VisualizationContext var1, T var2, float var3);
   }
}
