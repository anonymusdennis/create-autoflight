package com.simibubi.create.content.redstone.analogLever;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.Rotate;
import dev.engine_room.flywheel.lib.transform.Translate;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class AnalogLeverVisual extends AbstractBlockEntityVisual<AnalogLeverBlockEntity> implements SimpleDynamicVisual {
   protected final TransformedInstance handle = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ANALOG_LEVER_HANDLE))
      .createInstance();
   protected final TransformedInstance indicator = (TransformedInstance)this.instancerProvider()
      .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ANALOG_LEVER_INDICATOR))
      .createInstance();
   final float rX;
   final float rY;

   public AnalogLeverVisual(VisualizationContext context, AnalogLeverBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      AttachFace face = (AttachFace)this.blockState.getValue(AnalogLeverBlock.FACE);
      this.rX = face == AttachFace.FLOOR ? 0.0F : (face == AttachFace.WALL ? 90.0F : 180.0F);
      this.rY = AngleHelper.horizontalAngle((Direction)this.blockState.getValue(AnalogLeverBlock.FACING));
      this.transform(this.indicator.setIdentityTransform());
      this.animateLever(partialTick);
   }

   public void beginFrame(Context ctx) {
      if (!((AnalogLeverBlockEntity)this.blockEntity).clientState.settled()) {
         this.animateLever(ctx.partialTick());
      }
   }

   protected void animateLever(float pt) {
      float state = ((AnalogLeverBlockEntity)this.blockEntity).clientState.getValue(pt);
      this.indicator.colorRgb(Color.mixColors(2884352, 13434880, state / 15.0F));
      this.indicator.setChanged();
      float angle = (float)((double)(state / 15.0F * 90.0F / 180.0F) * Math.PI);
      ((TransformedInstance)((TransformedInstance)this.transform(this.handle.setIdentityTransform()))
            .translate(0.5F, 0.0625F, 0.5F)
            .rotate(angle, Direction.EAST))
         .translate(-0.5F, -0.0625F, -0.5F)
         .setChanged();
   }

   protected void _delete() {
      this.handle.delete();
      this.indicator.delete();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.handle, this.indicator});
   }

   private <T extends Translate<T> & Rotate<T>> T transform(T msr) {
      return (T)((Translate)((Rotate)(
               (Translate)((Rotate)msr.translate(this.getVisualPosition()).center()).rotate((float)((double)(this.rY / 180.0F) * Math.PI), Direction.UP)
            ))
            .rotate((float)((double)(this.rX / 180.0F) * Math.PI), Direction.EAST))
         .uncenter();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.handle);
      consumer.accept(this.indicator);
   }
}
