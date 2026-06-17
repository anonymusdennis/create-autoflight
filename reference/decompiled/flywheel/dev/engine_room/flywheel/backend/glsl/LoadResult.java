package dev.engine_room.flywheel.backend.glsl;

import dev.engine_room.flywheel.backend.compile.core.ShaderException;

public sealed interface LoadResult permits LoadResult.Success, LoadResult.Failure {
   SourceFile unwrap();

   public static record Failure(LoadError error) implements LoadResult {
      @Override
      public SourceFile unwrap() {
         throw new ShaderException.Load(this.error.generateMessage().build());
      }
   }

   public static record Success(SourceFile source) implements LoadResult {
      @Override
      public SourceFile unwrap() {
         return this.source;
      }
   }
}
