package dev.engine_room.flywheel.lib.task;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.task.TaskExecutor;
import java.util.ArrayList;
import java.util.List;

public final class PlanMap<K, C> implements SimplyComposedPlan<C> {
   private final List<K> keys = new ArrayList<>();
   private final List<Plan<C>> values = new ArrayList<>();

   public void add(K object, Plan<C> plan) {
      this.keys.add(object);
      this.values.add(plan);
   }

   public void remove(K object) {
      int index = this.keys.indexOf(object);
      if (index != -1) {
         this.keys.remove(index);
         this.values.remove(index);
      }
   }

   public void clear() {
      this.keys.clear();
      this.values.clear();
   }

   @Override
   public void execute(TaskExecutor taskExecutor, C context, Runnable onCompletion) {
      Distribute.plans(taskExecutor, context, onCompletion, this.values);
   }
}
