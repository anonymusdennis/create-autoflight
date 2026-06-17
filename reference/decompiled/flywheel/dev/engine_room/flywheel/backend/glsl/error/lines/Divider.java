package dev.engine_room.flywheel.backend.glsl.error.lines;

public enum Divider {
   BAR(" | "),
   ARROW("-> "),
   EQUALS(" = ");

   private final String s;

   private Divider(String s) {
      this.s = s;
   }

   @Override
   public String toString() {
      return this.s;
   }
}
