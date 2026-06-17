package dev.engine_room.flywheel.lib.memory;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;

class TrackedMemoryBlockImpl extends AbstractMemoryBlockImpl {
   final Cleaner cleaner;
   final TrackedMemoryBlockImpl.CleaningAction cleaningAction;
   final Cleanable cleanable;

   TrackedMemoryBlockImpl(long ptr, long size, Cleaner cleaner) {
      super(ptr, size);
      this.cleaner = cleaner;
      this.cleaningAction = new TrackedMemoryBlockImpl.CleaningAction(ptr, size);
      this.cleanable = cleaner.register(this, this.cleaningAction);
   }

   @Override
   public boolean isTracked() {
      return true;
   }

   @Override
   void freeInner() {
      super.freeInner();
      this.cleaningAction.freed = true;
      this.cleanable.clean();
   }

   @Override
   public MemoryBlock realloc(long size) {
      this.assertAllocated();
      MemoryBlock block = new TrackedMemoryBlockImpl(FlwMemoryTracker.realloc(this.ptr, size), size, this.cleaner);
      FlwMemoryTracker._allocCpuMemory(block.size());
      this.freeInner();
      return block;
   }

   static MemoryBlock malloc(long size) {
      MemoryBlock block = new TrackedMemoryBlockImpl(FlwMemoryTracker.malloc(size), size, CLEANER);
      FlwMemoryTracker._allocCpuMemory(block.size());
      return block;
   }

   static MemoryBlock calloc(long num, long size) {
      MemoryBlock block = new TrackedMemoryBlockImpl(FlwMemoryTracker.calloc(num, size), num * size, CLEANER);
      FlwMemoryTracker._allocCpuMemory(block.size());
      return block;
   }

   static class CleaningAction implements Runnable {
      final long ptr;
      final long size;
      boolean freed;

      CleaningAction(long ptr, long size) {
         this.ptr = ptr;
         this.size = size;
      }

      @Override
      public void run() {
         if (!this.freed) {
            FlwMemoryTracker.free(this.ptr);
            FlwMemoryTracker._freeCpuMemory(this.size);
         }
      }
   }
}
