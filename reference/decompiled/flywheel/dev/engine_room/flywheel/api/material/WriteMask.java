package dev.engine_room.flywheel.api.material;

public enum WriteMask {
   COLOR_DEPTH,
   COLOR,
   DEPTH;

   public boolean color() {
      return this == COLOR_DEPTH || this == COLOR;
   }

   public boolean depth() {
      return this == COLOR_DEPTH || this == DEPTH;
   }
}
