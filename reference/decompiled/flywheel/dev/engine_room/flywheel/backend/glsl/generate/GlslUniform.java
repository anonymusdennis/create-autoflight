package dev.engine_room.flywheel.backend.glsl.generate;

public class GlslUniform implements GlslBuilder.Declaration {
   private String type;
   private String name;

   public GlslUniform type(String typeName) {
      this.type = typeName;
      return this;
   }

   public GlslUniform name(String name) {
      this.name = name;
      return this;
   }

   @Override
   public String prettyPrint() {
      return "uniform " + this.type + " " + this.name + ";";
   }
}
