package dev.engine_room.flywheel.backend.util;

import dev.engine_room.flywheel.lib.memory.MemoryBlock;
import org.jetbrains.annotations.Nullable;

public class MemoryBuffer {
   private final long stride;
   @Nullable
   private MemoryBlock block;

   public MemoryBuffer(long stride) {
      this.stride = stride;
   }

   public boolean reallocIfNeeded(int index) {
      if (this.block == null) {
         this.block = MemoryBlock.malloc(this.neededCapacityForIndex(index + 8));
         return true;
      } else if (this.block.size() < this.neededCapacityForIndex(index)) {
         this.block = this.block.realloc(this.neededCapacityForIndex(index + 8));
         return true;
      } else {
         return false;
      }
   }

   public long ptr() {
      return this.block.ptr();
   }

   public long ptrForIndex(int index) {
      return this.block.ptr() + this.bytePosForIndex(index);
   }

   public long bytePosForIndex(int index) {
      return (long)index * this.stride;
   }

   public long neededCapacityForIndex(int index) {
      return (long)(index + 1) * this.stride;
   }

   public void delete() {
      if (this.block != null) {
         this.block.free();
      }
   }
}
