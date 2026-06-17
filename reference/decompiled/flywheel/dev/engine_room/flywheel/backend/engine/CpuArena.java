package dev.engine_room.flywheel.backend.engine;

import dev.engine_room.flywheel.lib.memory.MemoryBlock;

public class CpuArena extends AbstractArena {
   private MemoryBlock memoryBlock;

   public CpuArena(long elementSizeBytes, int initialCapacity) {
      super(elementSizeBytes);
      this.memoryBlock = MemoryBlock.malloc(elementSizeBytes * (long)initialCapacity);
   }

   public long indexToPointer(int i) {
      return this.memoryBlock.ptr() + (long)i * this.elementSizeBytes;
   }

   public void delete() {
      this.memoryBlock.free();
   }

   @Override
   public long byteCapacity() {
      return this.memoryBlock.size();
   }

   @Override
   protected void grow() {
      this.memoryBlock = this.memoryBlock.realloc(this.memoryBlock.size() * 2L);
   }
}
