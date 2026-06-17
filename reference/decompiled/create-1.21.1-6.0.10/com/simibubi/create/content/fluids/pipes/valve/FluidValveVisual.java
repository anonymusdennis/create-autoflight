package com.simibubi.create.content.fluids.pipes.valve;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
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
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;

public class FluidValveVisual extends ShaftVisual<FluidValveBlockEntity> implements SimpleDynamicVisual {
   protected TransformedInstance pointer;
   protected boolean settled;
   protected final double xRot;
   protected final double yRot;
   protected final int pointerRotationOffset;

   public FluidValveVisual(VisualizationContext dispatcher, FluidValveBlockEntity blockEntity, float partialTick) {
      super(dispatcher, blockEntity, partialTick);
      Direction facing = (Direction)this.blockState.getValue(FluidValveBlock.FACING);
      this.yRot = (double)AngleHelper.horizontalAngle(facing);
      this.xRot = facing == Direction.UP ? 0.0 : (facing == Direction.DOWN ? 180.0 : 90.0);
      Axis pipeAxis = FluidValveBlock.getPipeAxis(this.blockState);
      Axis shaftAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
      boolean twist = pipeAxis.isHorizontal() && shaftAxis == Axis.X || pipeAxis.isVertical();
      this.pointerRotationOffset = twist ? 90 : 0;
      this.settled = false;
      this.pointer = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.FLUID_VALVE_POINTER))
         .createInstance();
      this.transformPointer(partialTick);
   }

   public void beginFrame(Context ctx) {
      if (!((FluidValveBlockEntity)this.blockEntity).pointer.settled() || !this.settled) {
         this.transformPointer(ctx.partialTick());
      }
   }

   private void transformPointer(float partialTick) {
      float value = ((FluidValveBlockEntity)this.blockEntity).pointer.getValue(partialTick);
      float pointerRotation = Mth.lerp(value, 0.0F, -90.0F);
      this.settled = (value == 0.0F || value == 1.0F) && ((FluidValveBlockEntity)this.blockEntity).pointer.settled();
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.pointer
                           .setIdentityTransform()
                           .translate(this.getVisualPosition()))
                        .center())
                     .rotateYDegrees((float)this.yRot))
                  .rotateXDegrees((float)this.xRot))
               .rotateYDegrees((float)this.pointerRotationOffset + pointerRotation))
            .uncenter())
         .setChanged();
   }

   @Override
   public void updateLight(float partialTick) {
      super.updateLight(partialTick);
      this.relight(new FlatLit[]{this.pointer});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.pointer.delete();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.pointer);
   }
}
