package dev.engine_room.flywheel.backend.glsl.generate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlslBuilder {
   private final List<GlslBuilder.Declaration> elements = new ArrayList<>();

   public void define(String name, String value) {
      this.add(new GlslBuilder.Define(name, value));
   }

   public void undef(String key) {
      this.add(new GlslBuilder.Undef(key));
   }

   public GlslStruct struct() {
      return this.add(new GlslStruct());
   }

   public GlslVertexInput vertexInput() {
      return this.add(new GlslVertexInput());
   }

   public GlslUniform uniform() {
      return this.add(new GlslUniform());
   }

   public GlslUniformBlock uniformBlock() {
      return this.add(new GlslUniformBlock());
   }

   public GlslFn function() {
      return this.add(new GlslFn());
   }

   public void blankLine() {
      this.add(GlslBuilder.Separators.BLANK_LINE);
   }

   public void _raw(String sourceString) {
      this.add(new GlslBuilder.Raw(sourceString));
   }

   public <T extends GlslBuilder.Declaration> T add(T element) {
      this.elements.add(element);
      return element;
   }

   public String build() {
      return this.elements.stream().map(GlslBuilder.Declaration::prettyPrint).collect(Collectors.joining("\n"));
   }

   public interface Declaration {
      String prettyPrint();
   }

   public static record Define(String name, String value) implements GlslBuilder.Declaration {
      @Override
      public String prettyPrint() {
         return "#define " + this.name + " " + this.value;
      }
   }

   public static record Raw(String sourceString) implements GlslBuilder.Declaration {
      @Override
      public String prettyPrint() {
         return this.sourceString;
      }
   }

   public static enum Separators implements GlslBuilder.Declaration {
      BLANK_LINE("");

      private final String separator;

      private Separators(String separator) {
         this.separator = separator;
      }

      @Override
      public String prettyPrint() {
         return this.separator;
      }
   }

   public static record Undef(String name) implements GlslBuilder.Declaration {
      @Override
      public String prettyPrint() {
         return "#undef " + this.name;
      }
   }
}
