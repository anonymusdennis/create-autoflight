package dev.engine_room.flywheel.lib.memory;

import dev.engine_room.flywheel.lib.util.StringUtil;
import java.util.concurrent.atomic.AtomicLong;
import org.lwjgl.system.MemoryUtil;

public final class FlwMemoryTracker {
   private static final AtomicLong CPU_MEMORY = new AtomicLong(0L);
   private static final AtomicLong GPU_MEMORY = new AtomicLong(0L);

   private FlwMemoryTracker() {
   }

   public static long malloc(long size) {
      long ptr = MemoryUtil.nmemAlloc(size);
      if (ptr == 0L) {
         throw new OutOfMemoryError("Failed to allocate " + size + " bytes");
      } else {
         return ptr;
      }
   }

   public static long calloc(long num, long size) {
      long ptr = MemoryUtil.nmemCalloc(num, size);
      if (ptr == 0L) {
         throw new OutOfMemoryError("Failed to allocate " + num + " elements of size " + size + " bytes");
      } else {
         return ptr;
      }
   }

   public static long realloc(long ptr, long size) {
      long newPtr = MemoryUtil.nmemRealloc(ptr, size);
      if (newPtr == 0L) {
         throw new OutOfMemoryError("Failed to reallocate " + size + " bytes for address " + StringUtil.formatAddress(ptr));
      } else {
         return newPtr;
      }
   }

   public static void free(long ptr) {
      MemoryUtil.nmemFree(ptr);
   }

   public static void _allocCpuMemory(long size) {
      CPU_MEMORY.getAndAdd(size);
   }

   public static void _freeCpuMemory(long size) {
      CPU_MEMORY.getAndAdd(-size);
   }

   public static void _allocGpuMemory(long size) {
      GPU_MEMORY.getAndAdd(size);
   }

   public static void _freeGpuMemory(long size) {
      GPU_MEMORY.getAndAdd(-size);
   }

   public static long getCpuMemory() {
      return CPU_MEMORY.get();
   }

   public static long getGpuMemory() {
      return GPU_MEMORY.get();
   }
}
