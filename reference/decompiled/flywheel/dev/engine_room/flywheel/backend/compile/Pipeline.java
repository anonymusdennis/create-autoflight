package dev.engine_room.flywheel.backend.compile;

import dev.engine_room.flywheel.api.instance.InstanceType;
import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.glsl.SourceComponent;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record Pipeline(
   ResourceLocation vertexMain, ResourceLocation fragmentMain, Pipeline.InstanceAssembler assembler, String compilerMarker, Consumer<GlProgram> onLink
) {
   public static Pipeline.Builder builder() {
      return new Pipeline.Builder();
   }

   public static class Builder {
      @Nullable
      private ResourceLocation vertexMain;
      @Nullable
      private ResourceLocation fragmentMain;
      @Nullable
      private Pipeline.InstanceAssembler assembler;
      @Nullable
      private String compilerMarker;
      @Nullable
      private Consumer<GlProgram> onLink;

      public Pipeline.Builder vertexMain(ResourceLocation shader) {
         this.vertexMain = shader;
         return this;
      }

      public Pipeline.Builder fragmentMain(ResourceLocation shader) {
         this.fragmentMain = shader;
         return this;
      }

      public Pipeline.Builder assembler(Pipeline.InstanceAssembler assembler) {
         this.assembler = assembler;
         return this;
      }

      public Pipeline.Builder compilerMarker(String compilerMarker) {
         this.compilerMarker = compilerMarker;
         return this;
      }

      public Pipeline.Builder onLink(Consumer<GlProgram> onLink) {
         this.onLink = onLink;
         return this;
      }

      public Pipeline build() {
         Objects.requireNonNull(this.vertexMain);
         Objects.requireNonNull(this.fragmentMain);
         Objects.requireNonNull(this.assembler);
         Objects.requireNonNull(this.compilerMarker);
         Objects.requireNonNull(this.onLink);
         return new Pipeline(this.vertexMain, this.fragmentMain, this.assembler, this.compilerMarker, this.onLink);
      }
   }

   @FunctionalInterface
   public interface InstanceAssembler {
      SourceComponent assemble(InstanceType<?> var1);
   }
}
