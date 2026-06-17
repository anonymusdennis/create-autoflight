package dev.engine_room.flywheel.backend.engine;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public abstract class AbstractArena {
   protected final long elementSizeBytes;
   private final IntList freeStack = new IntArrayList();
   private int top = 0;

   public AbstractArena(long elementSizeBytes) {
      this.elementSizeBytes = elementSizeBytes;
   }

   public int alloc() {
      if (!this.freeStack.isEmpty()) {
         return this.freeStack.removeInt(this.freeStack.size() - 1);
      } else {
         if ((long)this.top * this.elementSizeBytes >= this.byteCapacity()) {
            this.grow();
         }

         return this.top++;
      }
   }

   public void free(int i) {
      this.freeStack.add(i);
   }

   public long byteOffsetOf(int i) {
      return (long)i * this.elementSizeBytes;
   }

   public int capacity() {
      return this.top;
   }

   public int occupancy() {
      return this.top - this.freeStack.size();
   }

   public abstract long byteCapacity();

   protected abstract void grow();
}
