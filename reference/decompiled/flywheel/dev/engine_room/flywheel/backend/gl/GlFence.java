package dev.engine_room.flywheel.backend.gl;

import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public class GlFence {
   private final long fence = GL32.glFenceSync(37143, 0);

   public boolean isSignaled() {
      MemoryStack memoryStack = MemoryStack.stackPush();

      int result;
      try {
         long checkPtr = memoryStack.ncalloc(4, 0, 4);
         GL32.nglGetSynciv(this.fence, 37140, 1, 0L, checkPtr);
         result = MemoryUtil.memGetInt(checkPtr);
      } catch (Throwable var6) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

      return result == 37145;
   }

   public void delete() {
      GL32.glDeleteSync(this.fence);
   }
}
