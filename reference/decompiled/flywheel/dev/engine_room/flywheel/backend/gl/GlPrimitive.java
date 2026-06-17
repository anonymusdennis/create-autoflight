package dev.engine_room.flywheel.backend.gl;

public enum GlPrimitive {
   POINTS(0),
   LINES(1),
   LINE_LOOP(2),
   LINE_STRIP(3),
   TRIANGLES(4),
   TRIANGLE_STRIP(5),
   TRIANGLE_FAN(6),
   QUADS(7),
   QUAD_STRIP(8),
   POLYGON(9);

   public final int glEnum;

   private GlPrimitive(int glEnum) {
      this.glEnum = glEnum;
   }
}
