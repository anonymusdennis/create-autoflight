package dev.simulated_team.simulated.content.blocks.analog_transmission;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.simulated_team.simulated.index.SimPartialModels;
import java.util.function.Consumer;
import net.minecraft.core.Direction;

public class AnalogTransmissionVisual extends SingleAxisRotatingVisual<AnalogTransmissionBlockEntity> {
   private final RotatingInstance cogInstance;

   public AnalogTransmissionVisual(VisualizationContext context, AnalogTransmissionBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick, Direction.UP, Models.partial(AllPartialModels.SHAFT));
      this.cogInstance = ((RotatingInstance)this.instancerProvider()
            .instancer(AllInstanceTypes.ROTATING, Models.partial(SimPartialModels.ANALOG_TRANSMISSION_COG))
            .createInstance())
         .rotateToFace(Direction.UP, this.rotationAxis())
         .setup(blockEntity.getExtraKinetics())
         .setPosition(this.getVisualPosition());
      this.cogInstance.setChanged();
   }

   public void update(float pt) {
      super.update(pt);
      this.cogInstance.setup(((AnalogTransmissionBlockEntity)this.blockEntity).getExtraKinetics()).setChanged();
   }

   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.cogInstance});
   }

   protected void _delete() {
      super._delete();
      this.cogInstance.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.cogInstance);
   }
}
