package dev.engine_room.flywheel.impl.task;

import dev.engine_room.flywheel.impl.FlwConfig;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.util.Mth;

public final class FlwTaskExecutor {
   private static final FlwTaskExecutor.AtomicLazy INSTANCE = new FlwTaskExecutor.AtomicLazy();

   private FlwTaskExecutor() {
   }

   public static TaskExecutorImpl get() {
      return INSTANCE.get();
   }

   private static int getOptimalThreadCount() {
      return Mth.clamp(Math.max(getMaxThreadCount() / 3, getMaxThreadCount() - 6), 1, 10);
   }

   private static int getMaxThreadCount() {
      return Runtime.getRuntime().availableProcessors();
   }

   private static class AtomicLazy {
      private final AtomicReference<FlwTaskExecutor.AtomicLazy> factory = new AtomicReference<>();
      private final AtomicReference<TaskExecutorImpl> reference = new AtomicReference<>();

      public final TaskExecutorImpl get() {
         TaskExecutorImpl result;
         while ((result = this.reference.get()) == null) {
            if (this.factory.compareAndSet(null, this)) {
               this.reference.set(this.initialize());
            }
         }

         return result;
      }

      protected TaskExecutorImpl initialize() {
         int threadCount = FlwConfig.INSTANCE.workerThreads();
         if (threadCount == 0) {
            return SerialTaskExecutor.INSTANCE;
         } else {
            if (threadCount < 0) {
               threadCount = FlwTaskExecutor.getOptimalThreadCount();
            } else {
               threadCount = Mth.clamp(threadCount, 1, FlwTaskExecutor.getMaxThreadCount());
            }

            ParallelTaskExecutor executor = new ParallelTaskExecutor("Flywheel", threadCount);
            executor.startWorkers();
            return executor;
         }
      }
   }
}
