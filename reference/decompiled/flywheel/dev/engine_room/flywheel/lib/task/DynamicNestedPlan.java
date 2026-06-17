package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.task.functional.SupplierWithContext;
import java.util.Collection;

public record DynamicNestedPlan<C>(SupplierWithContext<C, Collection<? extends Plan<C>>> plans) implements SimplyComposedPlan<C> {
   public static <C> DynamicNestedPlan<C> of(SupplierWithContext.Ignored<C, Collection<? extends Plan<C>>> supplier) {
      return new DynamicNestedPlan<>(supplier);
   }

   public static <C> DynamicNestedPlan<C> of(SupplierWithContext<C, Collection<? extends Plan<C>>> supplier) {
      return new DynamicNestedPlan<>(supplier);
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      Collection<? extends Plan<C>> plans = this.plans.get(context);
      if (plans.isEmpty()) {
         onCompletion.run();
      } else {
         Synchronizer sync = new Synchronizer(plans.size(), onCompletion);

         for (Plan<C> plan : plans) {
            plan.execute(taskExecutor, context, sync);
         }
      }
   }
}
