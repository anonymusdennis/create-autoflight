package dev.engine_room.flywheel.api.layout;

public enum IntegerRepr implements ValueRepr {
   BYTE(1),
   SHORT(2),
   INT(4);

   private final int byteSize;

   private IntegerRepr(int byteSize) {
      this.byteSize = byteSize;
   }

   @Override
   public int byteSize() {
      return this.byteSize;
   }
}
