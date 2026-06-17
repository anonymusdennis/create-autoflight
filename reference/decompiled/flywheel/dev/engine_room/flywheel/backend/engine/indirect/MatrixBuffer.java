package dev.engine_room.flywheel.backend.engine.indirect;

import dev.engine_room.flywheel.backend.engine.CpuArena;
import dev.engine_room.flywheel.backend.engine.embed.EnvironmentStorage;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

public class MatrixBuffer {
   private final ResizableStorageArray matrices = new ResizableStorageArray(112L);

   public void flush(StagingBuffer stagingBuffer, EnvironmentStorage environmentStorage) {
      CpuArena arena = environmentStorage.arena;
      int capacity = arena.capacity();
      if (capacity != 0) {
         this.matrices.ensureCapacity((long)capacity);
         stagingBuffer.enqueueCopy(
            arena.byteCapacity(), this.matrices.handle(), 0L, ptr -> MemoryUtil.memCopy(arena.indexToPointer(0), ptr, arena.byteCapacity())
         );
      }
   }

   public void bind() {
      if (this.matrices.capacity() != 0L) {
         GL46.glBindBufferRange(37074, 7, this.matrices.handle(), 0L, this.matrices.byteCapacity());
      }
   }

   public void delete() {
      this.matrices.delete();
   }
}
