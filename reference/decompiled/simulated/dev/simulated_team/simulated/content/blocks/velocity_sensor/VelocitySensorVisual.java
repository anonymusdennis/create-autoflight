package dev.simulated_team.simulated.content.blocks.velocity_sensor;

import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;

public class VelocitySensorVisual extends AbstractBlockEntityVisual<VelocitySensorBlockEntity> implements SimpleDynamicVisual {
   public VelocitySensorVisual(VisualizationContext ctx, VelocitySensorBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
   }

   public void beginFrame(Context context) {
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
   }

   public void updateLight(float v) {
   }

   protected void _delete() {
   }
}
