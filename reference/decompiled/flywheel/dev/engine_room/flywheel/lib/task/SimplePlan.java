package dev.engine_room.flywheel.lib.task;

import com.google.common.collect.ImmutableList;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.task.functional.RunnableWithContext;
import java.util.List;

public record SimplePlan<C>(List<RunnableWithContext<C>> parallelTasks) implements SimplyComposedPlan<C> {
   @SafeVarargs
   public static <C> SimplePlan<C> of(RunnableWithContext.Ignored<C>... tasks) {
      return new SimplePlan<>(List.of(tasks));
   }

   @SafeVarargs
   public static <C> SimplePlan<C> of(RunnableWithContext<C>... tasks) {
      return new SimplePlan<>(List.of(tasks));
   }

   public static <C> SimplePlan<C> of(List<RunnableWithContext<C>> tasks) {
      return new SimplePlan<>(tasks);
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      if (this.parallelTasks.isEmpty()) {
         onCompletion.run();
      } else {
         taskExecutor.execute(() -> Distribute.tasks(taskExecutor, context, onCompletion, this.parallelTasks, RunnableWithContext::run));
      }
   }

   @Override
   public Plan<C> and(Plan<C> plan) {
      return (Plan<C>)(plan instanceof SimplePlan<C> simple
         ? of(ImmutableList.builder().addAll(this.parallelTasks).addAll(simple.parallelTasks).build())
         : SimplyComposedPlan.super.and(plan));
   }
}
