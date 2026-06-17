package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.math.MoreMath;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

public final class Distribute {
   private Distribute() {
   }

   public static <C, T> void tasks(TaskExecutor taskExecutor, C context, Runnable onCompletion, List<T> list, BiConsumer<T, C> action) {
      int size = list.size();
      if (size == 0) {
         onCompletion.run();
      } else {
         int sliceSize = sliceSize(taskExecutor, size);
         if (size <= sliceSize) {
            for (T t : list) {
               action.accept(t, context);
            }

            onCompletion.run();
         } else if (sliceSize == 1) {
            Synchronizer synchronizer = new Synchronizer(size, onCompletion);

            for (T t : list) {
               taskExecutor.execute(() -> {
                  action.accept(t, context);
                  synchronizer.decrementAndEventuallyRun();
               });
            }
         } else {
            Synchronizer synchronizer = new Synchronizer(MoreMath.ceilingDiv(size, sliceSize), onCompletion);
            int remaining = size;

            while (remaining > 0) {
               int end = remaining;
               remaining -= sliceSize;
               int start = Math.max(remaining, 0);
               List<T> subList = list.subList(start, end);
               taskExecutor.execute(() -> {
                  for (T t : subList) {
                     action.accept(t, context);
                  }

                  synchronizer.decrementAndEventuallyRun();
               });
            }
         }
      }
   }

   public static <C, T> void slices(TaskExecutor taskExecutor, C context, Runnable onCompletion, List<T> list, BiConsumer<List<T>, C> action) {
      int size = list.size();
      if (size == 0) {
         onCompletion.run();
      } else {
         int sliceSize = sliceSize(taskExecutor, size);
         if (size <= sliceSize) {
            action.accept(list, context);
            onCompletion.run();
         } else if (sliceSize == 1) {
            Synchronizer synchronizer = new Synchronizer(size, onCompletion);

            for (T t : list) {
               taskExecutor.execute(() -> {
                  action.accept(Collections.singletonList(t), context);
                  synchronizer.decrementAndEventuallyRun();
               });
            }
         } else {
            Synchronizer synchronizer = new Synchronizer(MoreMath.ceilingDiv(size, sliceSize), onCompletion);
            int remaining = size;

            while (remaining > 0) {
               int end = remaining;
               remaining -= sliceSize;
               int start = Math.max(remaining, 0);
               List<T> subList = list.subList(start, end);
               taskExecutor.execute(() -> {
                  action.accept(subList, context);
                  synchronizer.decrementAndEventuallyRun();
               });
            }
         }
      }
   }

   public static <C> void plans(TaskExecutor taskExecutor, C context, Runnable onCompletion, List<Plan<C>> plans) {
      int size = plans.size();
      if (size == 0) {
         onCompletion.run();
      } else {
         Synchronizer synchronizer = new Synchronizer(size, onCompletion);
         int sliceSize = sliceSize(taskExecutor, size, 8);
         if (size <= sliceSize) {
            for (Plan<C> t : plans) {
               t.execute(taskExecutor, context, synchronizer);
            }
         } else if (sliceSize == 1) {
            for (Plan<C> t : plans) {
               taskExecutor.execute(() -> t.execute(taskExecutor, context, synchronizer));
            }
         } else {
            int remaining = size;

            while (remaining > 0) {
               int end = remaining;
               remaining -= sliceSize;
               int start = Math.max(remaining, 0);
               List<Plan<C>> subList = plans.subList(start, end);
               taskExecutor.execute(() -> {
                  for (Plan<C> t : subList) {
                     t.execute(taskExecutor, context, synchronizer);
                  }
               });
            }
         }
      }
   }

   public static int sliceSize(TaskExecutor taskExecutor, int totalSize) {
      return sliceSize(taskExecutor, totalSize, 32);
   }

   public static int sliceSize(TaskExecutor taskExecutor, int totalSize, int denominator) {
      return MoreMath.ceilingDiv(totalSize, taskExecutor.threadCount() * denominator);
   }
}
