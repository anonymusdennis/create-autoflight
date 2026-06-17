package dev.engine_room.flywheel.lib.model;

import dev.engine_room.flywheel.api.model.IndexSequence;
import org.lwjgl.system.MemoryUtil;

public final class QuadIndexSequence implements IndexSequence {
   public static final QuadIndexSequence INSTANCE = new QuadIndexSequence();

   private QuadIndexSequence() {
   }

   @Override
   public void fill(long ptr, int count) {
      int numVertices = 4 * (count / 6);
      int baseVertex = 0;

      while (baseVertex < numVertices) {
         MemoryUtil.memPutInt(ptr, baseVertex);
         MemoryUtil.memPutInt(ptr + 4L, baseVertex + 1);
         MemoryUtil.memPutInt(ptr + 8L, baseVertex + 2);
         MemoryUtil.memPutInt(ptr + 12L, baseVertex);
         MemoryUtil.memPutInt(ptr + 16L, baseVertex + 2);
         MemoryUtil.memPutInt(ptr + 20L, baseVertex + 3);
         baseVertex += 4;
         ptr += 24L;
      }
   }
}
