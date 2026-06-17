package dev.engine_room.flywheel.backend.glsl.error.lines;

public record NestedLine(String right) implements ErrorLine {
   @Override
   public Divider divider() {
      return Divider.EQUALS;
   }
}
