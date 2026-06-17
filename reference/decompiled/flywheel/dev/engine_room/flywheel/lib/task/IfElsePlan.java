package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import dev.engine_room.flywheel.lib.task.functional.BooleanSupplierWithContext;

public record IfElsePlan<C>(BooleanSupplierWithContext<C> condition, Plan<C> onTrue, Plan<C> onFalse) implements SimplyComposedPlan<C> {
   public static <C> IfElsePlan.Builder<C> on(BooleanSupplierWithContext<C> condition) {
      return new IfElsePlan.Builder<>(condition);
   }

   public static <C> IfElsePlan.Builder<C> on(BooleanSupplierWithContext.Ignored<C> condition) {
      return new IfElsePlan.Builder<>(condition);
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      if (this.condition.getAsBoolean(context)) {
         this.onTrue.execute(taskExecutor, context, onCompletion);
      } else {
         this.onFalse.execute(taskExecutor, context, onCompletion);
      }
   }

   public static final class Builder<C> {
      private final BooleanSupplierWithContext<C> condition;
      private Plan<C> onTrue = UnitPlan.of();
      private Plan<C> onFalse = UnitPlan.of();

      public Builder(BooleanSupplierWithContext<C> condition) {
         this.condition = condition;
      }

      public IfElsePlan.Builder<C> ifTrue(Plan<C> onTrue) {
         this.onTrue = onTrue;
         return this;
      }

      public IfElsePlan.Builder<C> ifFalse(Plan<C> onFalse) {
         this.onFalse = onFalse;
         return this;
      }

      public IfElsePlan<C> plan() {
         return new IfElsePlan<>(this.condition, this.onTrue, this.onFalse);
      }
   }
}
