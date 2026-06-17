package dev.engine_room.flywheel.impl.task;

import dev.engine_room.flywheel.impl.FlwImpl;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import net.minecraft.util.Mth;

public class ParallelTaskExecutor implements TaskExecutorImpl {
   private static final int MAX_ERRORS_LOGGED_PER_THREAD = 10;
   private final String name;
   private final int threadCount;
   private final AtomicBoolean running = new AtomicBoolean(false);
   private final List<ParallelTaskExecutor.WorkerThread> threads = new ArrayList<>();
   private final Deque<Runnable> taskQueue = new ConcurrentLinkedDeque<>();
   private final ThreadGroupNotifier taskNotifier = new ThreadGroupNotifier();
   private final WaitGroup waitGroup = new WaitGroup();
   private int mainThreadErrorLogLatch = 10;

   public ParallelTaskExecutor(String name, int threadCount) {
      this.name = name;
      this.threadCount = threadCount;
   }

   @Override
   public int threadCount() {
      return this.threadCount;
   }

   public void startWorkers() {
      if (!this.running.getAndSet(true)) {
         if (!this.threads.isEmpty()) {
            throw new IllegalStateException("Threads are still alive while in the STOPPED state");
         } else {
            for (int i = 0; i < this.threadCount; i++) {
               ParallelTaskExecutor.WorkerThread thread = new ParallelTaskExecutor.WorkerThread(this.name + " Task Executor #" + i);
               thread.setPriority(Mth.clamp(3, 1, 10));
               thread.start();
               this.threads.add(thread);
            }

            FlwImpl.LOGGER.info("Started {} worker threads", this.threads.size());
         }
      }
   }

   public void stopWorkers() {
      if (this.running.getAndSet(false)) {
         if (this.threads.isEmpty()) {
            throw new IllegalStateException("No threads are alive but the executor is in the RUNNING state");
         } else {
            FlwImpl.LOGGER.info("Stopping worker threads");
            synchronized (this.taskNotifier) {
               this.taskNotifier.notifyAll();
            }

            for (Thread thread : this.threads) {
               try {
                  thread.join();
               } catch (InterruptedException var4) {
               }
            }

            this.threads.clear();
            this.taskQueue.clear();
            this.waitGroup._reset();
         }
      }
   }

   @Override
   public void execute(Runnable task) {
      if (!this.running.get()) {
         throw new IllegalStateException("Executor is stopped");
      } else {
         this.waitGroup.add();
         this.taskQueue.add(task);
         this.taskNotifier.postNotification();
      }
   }

   @Override
   public boolean syncUntil(BooleanSupplier cond) {
      while (!cond.getAsBoolean()) {
         if (this.syncOneTask()) {
            return cond.getAsBoolean();
         }
      }

      return true;
   }

   @Override
   public boolean syncWhile(BooleanSupplier cond) {
      while (cond.getAsBoolean()) {
         if (this.syncOneTask()) {
            return !cond.getAsBoolean();
         }
      }

      return true;
   }

   @Override
   public void syncPoint() {
      while (!this.syncOneTask()) {
      }
   }

   private boolean syncOneTask() {
      Runnable task;
      if ((task = this.taskQueue.pollLast()) != null) {
         this.processTask(task);
         return false;
      } else {
         return this.waitGroup.await(10000);
      }
   }

   private void processTask(Runnable task) {
      try {
         task.run();
      } catch (Exception var6) {
         if (this.mainThreadErrorLogLatch > 0) {
            FlwImpl.LOGGER.error("Error running task", var6);
            this.mainThreadErrorLogLatch--;
         } else if (this.mainThreadErrorLogLatch == 0) {
            FlwImpl.LOGGER.error("Too many errors emitted by main thread, silencing.");
            this.mainThreadErrorLogLatch--;
         }
      } finally {
         this.waitGroup.done();
      }
   }

   private class WorkerThread extends Thread {
      private int errorLogLatch = 10;

      public WorkerThread(String name) {
         super(name);
      }

      @Override
      public void run() {
         while (ParallelTaskExecutor.this.running.get()) {
            Runnable task = ParallelTaskExecutor.this.taskQueue.pollFirst();
            if (task != null) {
               this.processTask(task);
            } else {
               this.spinThenWait();
            }
         }
      }

      private void processTask(Runnable task) {
         try {
            task.run();
         } catch (Exception var6) {
            if (this.errorLogLatch > 0) {
               FlwImpl.LOGGER.error("Error running task", var6);
               this.errorLogLatch--;
            } else if (this.errorLogLatch == 0) {
               FlwImpl.LOGGER.error("Too many errors emitted by thread {}, silencing.", this);
               this.errorLogLatch--;
            }
         } finally {
            ParallelTaskExecutor.this.waitGroup.done();
         }
      }

      private void spinThenWait() {
         long waitStart = System.nanoTime();

         while (System.nanoTime() - waitStart < 10000L) {
            if (!ParallelTaskExecutor.this.taskQueue.isEmpty()) {
               return;
            }

            Thread.onSpinWait();
         }

         ParallelTaskExecutor.this.taskNotifier.awaitNotification();
      }
   }
}
