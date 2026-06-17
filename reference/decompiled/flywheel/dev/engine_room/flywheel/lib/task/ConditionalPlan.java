package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.task.functional.BooleanSupplierWithContext;

public record ConditionalPlan<C>(BooleanSupplierWithContext<C> condition, Plan<C> onTrue) implements SimplyComposedPlan<C> {
   public static <C> ConditionalPlan.Builder<C> on(BooleanSupplierWithContext<C> condition) {
      return new ConditionalPlan.Builder<>(condition);
   }

   public static <C> ConditionalPlan.Builder<C> on(BooleanSupplierWithContext.Ignored<C> condition) {
      return new ConditionalPlan.Builder<>(condition);
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      if (this.condition.getAsBoolean(context)) {
         this.onTrue.execute(taskExecutor, context, onCompletion);
      } else {
         onCompletion.run();
      }
   }

   public static final class Builder<C> {
      private final BooleanSupplierWithContext<C> condition;

      public Builder(BooleanSupplierWithContext<C> condition) {
         this.condition = condition;
      }

      public ConditionalPlan<C> then(Plan<C> onTrue) {
         return new ConditionalPlan<>(this.condition, onTrue);
      }
   }
}
