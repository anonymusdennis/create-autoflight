package dev.engine_room.flywheel.backend.compile.core;

import dev.engine_room.flywheel.backend.gl.shader.GlProgram;
import dev.engine_room.flywheel.backend.gl.shader.GlShader;
import java.util.List;
import java.util.function.Consumer;
import org.lwjgl.opengl.GL20;

public class ProgramLinker {
   public GlProgram link(List<GlShader> shaders, Consumer<GlProgram> preLink) {
      return this.linkInternal(shaders, preLink).unwrap();
   }

   private LinkResult linkInternal(List<GlShader> shaders, Consumer<GlProgram> preLink) {
      int handle = GL20.glCreateProgram();
      GlProgram out = new GlProgram(handle);

      for (GlShader shader : shaders) {
         GL20.glAttachShader(handle, shader.handle());
      }

      preLink.accept(out);
      GL20.glLinkProgram(handle);
      String log = GL20.glGetProgramInfoLog(handle);
      if (linkSuccessful(handle)) {
         return LinkResult.success(out, log);
      } else {
         out.delete();
         return LinkResult.failure(log);
      }
   }

   private static boolean linkSuccessful(int handle) {
      return GL20.glGetProgrami(handle, 35714) == 1;
   }
}
