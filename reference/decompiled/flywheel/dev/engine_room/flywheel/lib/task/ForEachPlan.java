package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.task.functional.ConsumerWithContext;
import dev.engine_room.flywheel.lib.task.functional.SupplierWithContext;
import java.util.List;

public record ForEachPlan<T, C>(SupplierWithContext<C, List<T>> listSupplier, ConsumerWithContext<T, C> action) implements SimplyComposedPlan<C> {
   public static <T, C> ForEachPlan<T, C> of(SupplierWithContext<C, List<T>> iterable, ConsumerWithContext<T, C> forEach) {
      return new ForEachPlan<>(iterable, forEach);
   }

   public static <T, C> ForEachPlan<T, C> of(SupplierWithContext<C, List<T>> iterable, ConsumerWithContext.Ignored<T, C> forEach) {
      return new ForEachPlan<>(iterable, forEach);
   }

   public static <T, C> ForEachPlan<T, C> of(SupplierWithContext.Ignored<C, List<T>> iterable, ConsumerWithContext<T, C> forEach) {
      return new ForEachPlan<>(iterable, forEach);
   }

   public static <T, C> ForEachPlan<T, C> of(SupplierWithContext.Ignored<C, List<T>> iterable, ConsumerWithContext.Ignored<T, C> forEach) {
      return new ForEachPlan<>(iterable, forEach);
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      taskExecutor.execute(() -> Distribute.tasks(taskExecutor, context, onCompletion, this.listSupplier.get(context), this.action));
   }
}
