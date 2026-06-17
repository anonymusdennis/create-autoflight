package dev.engine_room.flywheel.backend.gl.shader;

public enum ShaderType {
   VERTEX("vertex", "VERTEX_SHADER", "vert", 35633),
   FRAGMENT("fragment", "FRAGMENT_SHADER", "frag", 35632),
   COMPUTE("compute", "COMPUTE_SHADER", "glsl", 37305);

   public final String name;
   public final String define;
   public final String extension;
   public final int glEnum;

   private ShaderType(String name, String define, String extension, int glEnum) {
      this.name = name;
      this.define = define;
      this.extension = extension;
      this.glEnum = glEnum;
   }
}
