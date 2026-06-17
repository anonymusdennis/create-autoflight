package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;

public class ToolBoxVisual extends AbstractBlockEntityVisual<ToolboxBlockEntity> implements SimpleDynamicVisual {
   private final Direction facing;
   private final TransformedInstance lid;
   private final TransformedInstance[] drawers;
   private float lastLidAngle = Float.NaN;
   private float lastDrawerOffset = Float.NaN;

   public ToolBoxVisual(VisualizationContext context, ToolboxBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.facing = ((Direction)this.blockState.getValue(ToolboxBlock.FACING)).getOpposite();
      Instancer<TransformedInstance> drawerModel = this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TOOLBOX_DRAWER));
      this.drawers = new TransformedInstance[]{(TransformedInstance)drawerModel.createInstance(), (TransformedInstance)drawerModel.createInstance()};
      this.lid = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.TOOLBOX_LIDS.get(blockEntity.getColor())))
         .createInstance();
      this.animate(partialTick);
   }

   protected void _delete() {
      this.lid.delete();

      for (TransformedInstance drawer : this.drawers) {
         drawer.delete();
      }
   }

   public void beginFrame(Context ctx) {
      this.animate(ctx.partialTick());
   }

   private void animate(float partialTicks) {
      float lidAngle = ((ToolboxBlockEntity)this.blockEntity).lid.getValue(partialTicks);
      float drawerOffset = ((ToolboxBlockEntity)this.blockEntity).drawers.getValue(partialTicks);
      if (lidAngle != this.lastLidAngle) {
         ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.lid
                              .setIdentityTransform()
                              .translate(this.getVisualPosition()))
                           .center())
                        .rotateYDegrees(-this.facing.toYRot()))
                     .uncenter())
                  .translate(0.0F, 0.375F, 0.75F)
                  .rotateXDegrees(135.0F * lidAngle))
               .translateBack(0.0F, 0.375F, 0.75F))
            .setChanged();
      }

      if (drawerOffset != this.lastDrawerOffset) {
         for (int offset : Iterate.zeroAndOne) {
            ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.drawers[offset]
                           .setIdentityTransform()
                           .translate(this.getVisualPosition()))
                        .center())
                     .rotateYDegrees(-this.facing.toYRot()))
                  .uncenter())
               .translate(0.0F, (float)(offset * 1) / 8.0F, -drawerOffset * 0.175F * (float)(2 - offset))
               .setChanged();
         }
      }

      this.lastLidAngle = lidAngle;
      this.lastDrawerOffset = drawerOffset;
   }

   public void updateLight(float partialTick) {
      this.relight(this.drawers);
      this.relight(new FlatLit[]{this.lid});
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.lid);

      for (TransformedInstance drawer : this.drawers) {
         consumer.accept(drawer);
      }
   }
}
