package dev.engine_room.flywheel.impl.task;

public class ThreadGroupNotifier {
   public synchronized void awaitNotification() {
      try {
         this.wait();
      } catch (InterruptedException var2) {
      }
   }

   public synchronized void postNotification() {
      this.notifyAll();
   }
}
