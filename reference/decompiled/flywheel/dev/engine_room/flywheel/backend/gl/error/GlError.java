package dev.engine_room.flywheel.backend.gl.error;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.function.Supplier;
import org.lwjgl.opengl.GL20;

public enum GlError {
   INVALID_ENUM(1280),
   INVALID_VALUE(1281),
   INVALID_OPERATION(1282),
   INVALID_FRAMEBUFFER_OPERATION(1286),
   OUT_OF_MEMORY(1285),
   STACK_UNDERFLOW(1284),
   STACK_OVERFLOW(1283);

   private static final Int2ObjectMap<GlError> errorLookup = new Int2ObjectArrayMap();
   final int glEnum;

   private GlError(int glEnum) {
      this.glEnum = glEnum;
   }

   public static GlError poll() {
      return (GlError)errorLookup.get(GL20.glGetError());
   }

   public static void pollAndThrow(Supplier<String> context) {
   }

   static {
      errorLookup.defaultReturnValue(null);

      for (GlError value : values()) {
         errorLookup.put(value.glEnum, value);
      }
   }
}
