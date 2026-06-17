package dev.engine_room.flywheel.backend.gl.array;

import dev.engine_room.flywheel.backend.gl.GlNumericType;

public sealed interface VertexAttribute permits VertexAttribute.Float, VertexAttribute.Int {
   int byteWidth();

   public static record Float(GlNumericType type, int size, boolean normalized) implements VertexAttribute {
      @Override
      public int byteWidth() {
         return this.size * this.type.byteWidth();
      }
   }

   public static record Int(GlNumericType type, int size) implements VertexAttribute {
      @Override
      public int byteWidth() {
         return this.size * this.type.byteWidth();
      }
   }
}
