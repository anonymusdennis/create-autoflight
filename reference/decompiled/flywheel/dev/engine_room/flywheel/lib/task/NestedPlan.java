package dev.engine_room.flywheel.lib.task;

import com.google.common.collect.ImmutableList;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import java.util.List;

public record NestedPlan<C>(List<Plan<C>> parallelPlans) implements SimplyComposedPlan<C> {
   @SafeVarargs
   public static <C> NestedPlan<C> of(Plan<C>... plans) {
      return new NestedPlan<>(ImmutableList.copyOf(plans));
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      if (this.parallelPlans.isEmpty()) {
         onCompletion.run();
      } else {
         int size = this.parallelPlans.size();
         if (size == 1) {
            this.parallelPlans.get(0).execute(taskExecutor, context, onCompletion);
         } else {
            Synchronizer wait = new Synchronizer(size, onCompletion);

            for (Plan<C> plan : this.parallelPlans) {
               plan.execute(taskExecutor, context, wait);
            }
         }
      }
   }

   @Override
   public Plan<C> and(Plan<C> plan) {
      return new NestedPlan<>(ImmutableList.builder().addAll(this.parallelPlans).add(plan).build());
   }
}
