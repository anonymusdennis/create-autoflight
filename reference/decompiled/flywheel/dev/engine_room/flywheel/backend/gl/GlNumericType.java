package dev.engine_room.flywheel.backend.gl;

public enum GlNumericType {
   FLOAT(4, "float", 5126),
   UBYTE(1, "ubyte", 5121),
   BYTE(1, "byte", 5120),
   USHORT(2, "ushort", 5123),
   SHORT(2, "short", 5122),
   UINT(4, "uint", 5125),
   INT(4, "int", 5124),
   DOUBLE(8, "double", 5130);

   public final int byteWidth;
   public final String typeName;
   public final int glEnum;

   private GlNumericType(int bytes, String name, int glEnum) {
      this.byteWidth = bytes;
      this.typeName = name;
      this.glEnum = glEnum;
   }

   public int byteWidth() {
      return this.byteWidth;
   }

   public String typeName() {
      return this.typeName;
   }

   public int glEnum() {
      return this.glEnum;
   }

   @Override
   public String toString() {
      return this.typeName;
   }
}
