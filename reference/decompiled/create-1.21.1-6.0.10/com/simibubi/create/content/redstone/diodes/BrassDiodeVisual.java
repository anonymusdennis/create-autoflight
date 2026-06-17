package com.simibubi.create.content.redstone.diodes;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.TickableVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import java.util.function.Consumer;
import net.createmod.catnip.theme.Color;

public class BrassDiodeVisual extends AbstractBlockEntityVisual<BrassDiodeBlockEntity> implements SimpleTickableVisual {
   protected final TransformedInstance indicator = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FLEXPEATER_INDICATOR))
      .createInstance();
   protected int previousState;

   public BrassDiodeVisual(VisualizationContext context, BrassDiodeBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      ((TransformedInstance)this.indicator.setIdentityTransform().translate(this.getVisualPosition())).colorRgb(this.getColor()).setChanged();
      this.previousState = blockEntity.state;
   }

   public void tick(Context context) {
      if (this.previousState != ((BrassDiodeBlockEntity)this.blockEntity).state) {
         this.indicator.colorRgb(this.getColor());
         this.indicator.setChanged();
         this.previousState = ((BrassDiodeBlockEntity)this.blockEntity).state;
      }
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.indicator});
   }

   protected void _delete() {
      this.indicator.delete();
   }

   protected int getColor() {
      return Color.mixColors(2884352, 13434880, ((BrassDiodeBlockEntity)this.blockEntity).getProgress());
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.indicator);
   }
}
