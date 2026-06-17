package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.task.Plan;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

public interface TickableVisual extends Visual {
   Plan<TickableVisual.Context> planTick();

   @NonExtendable
   public interface Context {
   }
}
