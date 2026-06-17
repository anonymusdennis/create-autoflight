package dev.engine_room.flywheel.lib.memory;

import java.nio.ByteBuffer;

public sealed interface MemoryBlock permits AbstractMemoryBlockImpl {
   long ptr();

   long size();

   boolean isFreed();

   boolean isTracked();

   void copyTo(MemoryBlock var1);

   void copyTo(long var1, long var3);

   void copyTo(long var1);

   void clear();

   ByteBuffer asBuffer();

   MemoryBlock realloc(long var1);

   void free();

   static MemoryBlock malloc(long size) {
      return MemoryBlockImpl.DEBUG_MEMORY_SAFETY ? DebugMemoryBlockImpl.malloc(size) : MemoryBlockImpl.malloc(size);
   }

   static MemoryBlock mallocTracked(long size) {
      return TrackedMemoryBlockImpl.malloc(size);
   }

   static MemoryBlock calloc(long num, long size) {
      return MemoryBlockImpl.DEBUG_MEMORY_SAFETY ? DebugMemoryBlockImpl.calloc(num, size) : MemoryBlockImpl.calloc(num, size);
   }

   static MemoryBlock callocTracked(long num, long size) {
      return TrackedMemoryBlockImpl.calloc(num, size);
   }
}
