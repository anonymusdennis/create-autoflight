package dev.engine_room.flywheel.impl.task;

import com.google.common.base.Preconditions;
import java.util.concurrent.atomic.AtomicInteger;

public class WaitGroup {
   private final AtomicInteger counter = new AtomicInteger(0);

   public void add() {
      this.add(1);
   }

   public void add(int i) {
      Preconditions.checkArgument(i >= 0, "Cannot add a negative number of tasks to a WaitGroup!");
      if (i != 0) {
         this.counter.addAndGet(i);
      }
   }

   public void done() {
      if (this.counter.decrementAndGet() < 0) {
         throw new IllegalStateException("WaitGroup counter is negative!");
      }
   }

   public boolean await(int nsTimeout) {
      long startTime = System.nanoTime();

      while (this.counter.get() > 0) {
         if (System.nanoTime() - startTime > (long)nsTimeout) {
            return false;
         }

         Thread.onSpinWait();
      }

      return true;
   }

   public void _reset() {
      this.counter.set(0);
   }
}
