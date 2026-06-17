package dev.engine_room.flywheel.lib.visual;

import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.lib.task.RunnablePlan;

public interface SimpleTickableVisual extends TickableVisual {
   void tick(TickableVisual.Context var1);

   @Override
   default Plan<TickableVisual.Context> planTick() {
      return RunnablePlan.of(this::tick);
   }
}
