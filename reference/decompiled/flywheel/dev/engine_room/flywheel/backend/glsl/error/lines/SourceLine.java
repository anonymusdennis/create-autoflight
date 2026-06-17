package dev.engine_room.flywheel.backend.glsl.error.lines;

public record SourceLine(String number, String line) implements ErrorLine {
   public static SourceLine numbered(int number, String line) {
      return new SourceLine(Integer.toString(number), line);
   }

   @Override
   public String left() {
      return this.number;
   }

   @Override
   public String right() {
      return this.line;
   }
}
