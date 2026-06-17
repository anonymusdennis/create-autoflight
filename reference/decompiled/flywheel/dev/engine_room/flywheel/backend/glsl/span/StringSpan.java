package dev.engine_room.flywheel.backend.glsl.span;

import dev.engine_room.flywheel.backend.glsl.SourceLines;

public class StringSpan extends Span {
   public StringSpan(SourceLines in, int start, int end) {
      super(in, start, end);
   }

   @Override
   public Span subSpan(int from, int to) {
      return new StringSpan(this.in, this.start.pos() + from, this.start.pos() + to);
   }

   @Override
   public String get() {
      return this.in.raw.substring(this.start.pos(), this.end.pos());
   }

   @Override
   public boolean isErr() {
      return false;
   }
}
