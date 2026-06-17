package dev.engine_room.flywheel.lib.task;

import java.util.concurrent.atomic.AtomicInteger;

public final class Synchronizer implements Runnable {
   private final AtomicInteger countDown;
   private final Runnable onCompletion;

   public Synchronizer(int countDown, Runnable onCompletion) {
      this.countDown = new AtomicInteger(countDown);
      this.onCompletion = onCompletion;
   }

   public void decrementAndEventuallyRun() {
      if (this.countDown.decrementAndGet() == 0) {
         this.onCompletion.run();
      }
   }

   @Override
   public void run() {
      this.decrementAndEventuallyRun();
   }
}
