package dev.engine_room.flywheel.lib.instance;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstanceHandle;
import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.api.instance.InstanceWriter;
import dev.engine_room.flywheel.api.layout.Layout;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public final class SimpleInstanceType<I extends Instance> implements InstanceType<I> {
   private final SimpleInstanceType.Factory<I> factory;
   private final Layout layout;
   private final InstanceWriter<I> writer;
   private final ResourceLocation vertexShader;
   private final ResourceLocation cullShader;

   public SimpleInstanceType(
      SimpleInstanceType.Factory<I> factory, Layout layout, InstanceWriter<I> writer, ResourceLocation vertexShader, ResourceLocation cullShader
   ) {
      this.factory = factory;
      this.layout = layout;
      this.writer = writer;
      this.vertexShader = vertexShader;
      this.cullShader = cullShader;
   }

   public static <I extends Instance> SimpleInstanceType.Builder<I> builder(SimpleInstanceType.Factory<I> factory) {
      return new SimpleInstanceType.Builder<>(factory);
   }

   @Override
   public I create(InstanceHandle handle) {
      return this.factory.create(this, handle);
   }

   @Override
   public Layout layout() {
      return this.layout;
   }

   @Override
   public InstanceWriter<I> writer() {
      return this.writer;
   }

   @Override
   public ResourceLocation vertexShader() {
      return this.vertexShader;
   }

   @Override
   public ResourceLocation cullShader() {
      return this.cullShader;
   }

   public static final class Builder<I extends Instance> {
      private final SimpleInstanceType.Factory<I> factory;
      private Layout layout;
      private InstanceWriter<I> writer;
      private ResourceLocation vertexShader;
      private ResourceLocation cullShader;

      public Builder(SimpleInstanceType.Factory<I> factory) {
         this.factory = factory;
      }

      public SimpleInstanceType.Builder<I> layout(Layout layout) {
         this.layout = layout;
         return this;
      }

      public SimpleInstanceType.Builder<I> writer(InstanceWriter<I> writer) {
         this.writer = writer;
         return this;
      }

      public SimpleInstanceType.Builder<I> vertexShader(ResourceLocation vertexShader) {
         this.vertexShader = vertexShader;
         return this;
      }

      public SimpleInstanceType.Builder<I> cullShader(ResourceLocation cullShader) {
         this.cullShader = cullShader;
         return this;
      }

      public SimpleInstanceType<I> build() {
         Objects.requireNonNull(this.layout);
         Objects.requireNonNull(this.writer);
         Objects.requireNonNull(this.vertexShader);
         Objects.requireNonNull(this.cullShader);
         return new SimpleInstanceType<>(this.factory, this.layout, this.writer, this.vertexShader, this.cullShader);
      }
   }

   @FunctionalInterface
   public interface Factory<I extends Instance> {
      I create(InstanceType<I> var1, InstanceHandle var2);
   }
}
