package dev.engine_room.flywheel.lib.memory;

import dev.engine_room.flywheel.lib.internal.FlwLibLink;
import dev.engine_room.flywheel.lib.util.StringUtil;
import java.lang.StackWalker.StackFrame;
import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;

class DebugMemoryBlockImpl extends AbstractMemoryBlockImpl {
   final Cleaner cleaner;
   final DebugMemoryBlockImpl.CleaningAction cleaningAction;
   final Cleanable cleanable;

   DebugMemoryBlockImpl(long ptr, long size, Cleaner cleaner, int skipFrames) {
      super(ptr, size);
      this.cleaner = cleaner;
      this.cleaningAction = new DebugMemoryBlockImpl.CleaningAction(ptr, size, getStackTrace(skipFrames + 1));
      this.cleanable = cleaner.register(this, this.cleaningAction);
   }

   @Override
   public boolean isTracked() {
      return false;
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
      MemoryBlock block = new DebugMemoryBlockImpl(FlwMemoryTracker.realloc(this.ptr, size), size, this.cleaner, 1);
      FlwMemoryTracker._allocCpuMemory(block.size());
      this.freeInner();
      return block;
   }

   static StackFrame[] getStackTrace(int skipFrames) {
      return StackWalker.getInstance().walk(s -> s.skip((long)(skipFrames + 1)).toArray(StackFrame[]::new));
   }

   static MemoryBlock malloc(long size) {
      MemoryBlock block = new DebugMemoryBlockImpl(FlwMemoryTracker.malloc(size), size, CLEANER, 2);
      FlwMemoryTracker._allocCpuMemory(block.size());
      return block;
   }

   static MemoryBlock calloc(long num, long size) {
      MemoryBlock block = new DebugMemoryBlockImpl(FlwMemoryTracker.calloc(num, size), num * size, CLEANER, 2);
      FlwMemoryTracker._allocCpuMemory(block.size());
      return block;
   }

   static class CleaningAction implements Runnable {
      final long ptr;
      final long size;
      final StackFrame[] allocationSite;
      boolean freed;

      CleaningAction(long ptr, long size, StackFrame[] allocationSite) {
         this.ptr = ptr;
         this.size = size;
         this.allocationSite = allocationSite;
      }

      @Override
      public void run() {
         if (!this.freed) {
            StringBuilder builder = new StringBuilder();
            builder.append("Reclaimed ")
               .append(this.size)
               .append(" bytes at address ")
               .append(StringUtil.formatAddress(this.ptr))
               .append(" that were leaked from allocation site:");

            for (StackFrame frame : this.allocationSite) {
               builder.append("\n\t");
               builder.append(frame);
            }

            FlwLibLink.INSTANCE.getLogger().warn(builder.toString());
            FlwMemoryTracker.free(this.ptr);
            FlwMemoryTracker._freeCpuMemory(this.size);
         }
      }
   }
}
