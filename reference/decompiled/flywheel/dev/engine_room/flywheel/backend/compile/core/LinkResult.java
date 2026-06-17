package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import org.jetbrains.annotations.NotNull;

public sealed interface LinkResult permits LinkResult.Success, LinkResult.Failure {
   GlProgram unwrap();

   static LinkResult success(GlProgram program, String log) {
      return new LinkResult.Success(program, log);
   }

   static LinkResult failure(String failure) {
      return new LinkResult.Failure(failure);
   }

   public static record Failure(String failure) implements LinkResult {
      @Override
      public GlProgram unwrap() {
         throw new ShaderException.Link(this.failure);
      }
   }

   public static record Success(GlProgram program, String log) implements LinkResult {
      @NotNull
      @Override
      public GlProgram unwrap() {
         return this.program;
      }
   }
}
