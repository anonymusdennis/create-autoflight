package dev.engine_room.flywheel.backend.gl.buffer;

import dev.engine_room.flywheel.backend.gl.GlStateTracker;

public enum GlBufferType {
   ARRAY_BUFFER(34962, 34964),
   ELEMENT_ARRAY_BUFFER(34963, 34965),
   PIXEL_PACK_BUFFER(35051, 35053),
   PIXEL_UNPACK_BUFFER(35052, 35055),
   TRANSFORM_FEEDBACK_BUFFER(35982, 35983),
   UNIFORM_BUFFER(35345, 35368),
   TEXTURE_BUFFER(35882, 35882),
   COPY_READ_BUFFER(36662, 36662),
   COPY_WRITE_BUFFER(36663, 36663),
   DRAW_INDIRECT_BUFFER(36671, 36675),
   ATOMIC_COUNTER_BUFFER(37568, 37569),
   DISPATCH_INDIRECT_BUFFER(37102, 37103),
   SHADER_STORAGE_BUFFER(37074, 37075);

   public final int glEnum;
   public final int glBindingEnum;

   private GlBufferType(int glEnum, int glBindingEnum) {
      this.glEnum = glEnum;
      this.glBindingEnum = glBindingEnum;
   }

   public static GlBufferType fromTarget(int pTarget) {
      return switch (pTarget) {
         case 34962 -> ARRAY_BUFFER;
         case 34963 -> ELEMENT_ARRAY_BUFFER;
         case 35051 -> PIXEL_PACK_BUFFER;
         case 35052 -> PIXEL_UNPACK_BUFFER;
         case 35345 -> UNIFORM_BUFFER;
         case 35882 -> TEXTURE_BUFFER;
         case 35982 -> TRANSFORM_FEEDBACK_BUFFER;
         case 36662 -> COPY_READ_BUFFER;
         case 36663 -> COPY_WRITE_BUFFER;
         case 36671 -> DRAW_INDIRECT_BUFFER;
         case 37074 -> SHADER_STORAGE_BUFFER;
         case 37102 -> DISPATCH_INDIRECT_BUFFER;
         case 37568 -> ATOMIC_COUNTER_BUFFER;
         default -> throw new IllegalArgumentException("Unknown target: " + pTarget);
      };
   }

   public void bind(int buffer) {
      GlStateTracker.bindBuffer(this, buffer);
   }
}
