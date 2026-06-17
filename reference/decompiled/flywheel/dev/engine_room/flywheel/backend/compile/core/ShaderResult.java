package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.gl.shader.GlShader;

public sealed interface ShaderResult permits ShaderResult.Success, ShaderResult.Failure {
   GlShader unwrap();

   static ShaderResult success(GlShader program, String infoLog) {
      return new ShaderResult.Success(program, infoLog);
   }

   static ShaderResult failure(FailedCompilation failure) {
      return new ShaderResult.Failure(failure);
   }

   public static record Failure(FailedCompilation failure) implements ShaderResult {
      @Override
      public GlShader unwrap() {
         throw new ShaderException.Compile(this.failure.generateMessage());
      }
   }

   public static record Success(GlShader shader, String infoLog) implements ShaderResult {
      @Override
      public GlShader unwrap() {
         return this.shader;
      }
   }
}
