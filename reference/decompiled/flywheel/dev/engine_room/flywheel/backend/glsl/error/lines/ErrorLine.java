package dev.engine_room.flywheel.backend.glsl.error.lines;

public interface ErrorLine {
   default int neededMargin() {
      return this.left().length();
   }

   default Divider divider() {
      return Divider.BAR;
   }

   default String build() {
      return this.left() + this.divider() + this.right();
   }

   default String left() {
      return "";
   }

   default String right() {
      return "";
   }
}
