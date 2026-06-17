package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.lib.task.RunnablePlan;

public interface SimpleDynamicVisual extends DynamicVisual {
   void beginFrame(DynamicVisual.Context var1);

   @Override
   default Plan<DynamicVisual.Context> planFrame() {
      return RunnablePlan.of(this::beginFrame);
   }
}
