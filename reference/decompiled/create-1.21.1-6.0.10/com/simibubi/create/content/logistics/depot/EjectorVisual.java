package com.simibubi.create.content.logistics.depot;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.ShaftVisual;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;

public class EjectorVisual extends ShaftVisual<EjectorBlockEntity> implements SimpleDynamicVisual {
   protected final TransformedInstance plate;
   private float lastProgress = Float.NaN;

   public EjectorVisual(VisualizationContext dispatcher, EjectorBlockEntity blockEntity, float partialTick) {
      super(dispatcher, blockEntity, partialTick);
      this.plate = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.EJECTOR_TOP))
         .createInstance();
      this.pivotPlate(this.getLidProgress(partialTick));
   }

   public void beginFrame(Context ctx) {
      float lidProgress = this.getLidProgress(ctx.partialTick());
      if (lidProgress != this.lastProgress) {
         this.pivotPlate(lidProgress);
         this.lastProgress = lidProgress;
      }
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.plate});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.plate.delete();
   }

   private float getLidProgress(float pt) {
      return ((EjectorBlockEntity)this.blockEntity).getLidProgress(pt);
   }

   private void pivotPlate(float lidProgress) {
      float angle = lidProgress * 70.0F;
      EjectorRenderer.applyLidAngle(
         (KineticBlockEntity)this.blockEntity, angle, (TransformedInstance)this.plate.setIdentityTransform().translate(this.getVisualPosition())
      );
      this.plate.setChanged();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.plate);
   }
}
