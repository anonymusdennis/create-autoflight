package dev.engine_room.flywheel.lib.memory;

import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import org.lwjgl.system.MemoryUtil;

abstract non-sealed class AbstractMemoryBlockImpl implements MemoryBlock {
   static final Cleaner CLEANER = Cleaner.create();
   final long ptr;
   final long size;
   boolean freed;

   AbstractMemoryBlockImpl(long ptr, long size) {
      this.ptr = ptr;
      this.size = size;
   }

   void assertAllocated() {
      if (this.freed) {
         throw new IllegalStateException("Operation called on freed MemoryBlock!");
      }
   }

   @Override
   public long ptr() {
      this.assertAllocated();
      return this.ptr;
   }

   @Override
   public long size() {
      this.assertAllocated();
      return this.size;
   }

   @Override
   public boolean isFreed() {
      return this.freed;
   }

   @Override
   public void copyTo(MemoryBlock block) {
      this.assertAllocated();
      long bytes = Math.min(this.size, block.size());
      this.copyTo(block.ptr(), bytes);
   }

   @Override
   public void copyTo(long ptr, long bytes) {
      this.assertAllocated();
      MemoryUtil.memCopy(this.ptr, ptr, bytes);
   }

   @Override
   public void copyTo(long ptr) {
      this.assertAllocated();
      this.copyTo(ptr, this.size);
   }

   @Override
   public void clear() {
      this.assertAllocated();
      MemoryUtil.memSet(this.ptr, 0, this.size);
   }

   @Override
   public ByteBuffer asBuffer() {
      this.assertAllocated();
      int intSize = (int)this.size;
      if ((long)intSize != this.size) {
         throw new UnsupportedOperationException("Cannot create buffer with long capacity!");
      } else {
         return MemoryUtil.memByteBuffer(this.ptr, intSize);
      }
   }

   void freeInner() {
      FlwMemoryTracker._freeCpuMemory(this.size);
      this.freed = true;
   }

   @Override
   public void free() {
      this.assertAllocated();
      FlwMemoryTracker.free(this.ptr);
      this.freeInner();
   }
}
