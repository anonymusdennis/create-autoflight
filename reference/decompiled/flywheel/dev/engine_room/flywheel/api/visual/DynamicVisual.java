package dev.engine_room.flywheel.api.visual;

import dev.engine_room.flywheel.api.task.Plan;
import net.minecraft.client.Camera;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.joml.FrustumIntersection;

public interface DynamicVisual extends Visual {
   Plan<DynamicVisual.Context> planFrame();

   @NonExtendable
   public interface Context {
      Camera camera();

      FrustumIntersection frustum();

      float partialTick();

      DistanceUpdateLimiter limiter();
   }
}
