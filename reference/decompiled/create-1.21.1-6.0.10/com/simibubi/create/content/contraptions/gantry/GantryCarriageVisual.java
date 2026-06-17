package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
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
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.util.Mth;

public class GantryCarriageVisual extends ShaftVisual<GantryCarriageBlockEntity> implements SimpleDynamicVisual {
   private final TransformedInstance gantryCogs;
   final Direction facing;
   final Boolean alongFirst;
   final Axis rotationAxis;
   final float rotationMult;
   final BlockPos visualPos;
   private float lastAngle = Float.NaN;

   public GantryCarriageVisual(VisualizationContext context, GantryCarriageBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.gantryCogs = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.GANTRY_COGS))
         .createInstance();
      this.facing = (Direction)this.blockState.getValue(GantryCarriageBlock.FACING);
      this.alongFirst = (Boolean)this.blockState.getValue(GantryCarriageBlock.AXIS_ALONG_FIRST_COORDINATE);
      this.rotationAxis = KineticBlockEntityRenderer.getRotationAxisOf(blockEntity);
      this.rotationMult = getRotationMultiplier(this.getGantryAxis(), this.facing);
      this.visualPos = this.facing.getAxisDirection() == AxisDirection.POSITIVE
         ? blockEntity.getBlockPos()
         : blockEntity.getBlockPos().relative(this.facing.getOpposite());
      this.animateCogs(this.getCogAngle());
   }

   public void beginFrame(Context ctx) {
      float cogAngle = this.getCogAngle();
      if (!Mth.equal(cogAngle, this.lastAngle)) {
         this.animateCogs(cogAngle);
      }
   }

   private float getCogAngle() {
      return GantryCarriageRenderer.getAngleForBE((KineticBlockEntity)this.blockEntity, this.visualPos, this.rotationAxis) * this.rotationMult;
   }

   private void animateCogs(float cogAngle) {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.gantryCogs
                              .setIdentityTransform()
                              .translate(this.getVisualPosition()))
                           .center())
                        .rotateYDegrees(AngleHelper.horizontalAngle(this.facing)))
                     .rotateXDegrees(this.facing == Direction.UP ? 0.0F : (this.facing == Direction.DOWN ? 180.0F : 90.0F)))
                  .rotateYDegrees(this.alongFirst ^ this.facing.getAxis() == Axis.X ? 0.0F : 90.0F))
               .translate(0.0F, -0.5625F, 0.0F)
               .rotateXDegrees(-cogAngle))
            .translate(0.0F, 0.5625F, 0.0F)
            .uncenter())
         .setChanged();
   }

   static float getRotationMultiplier(Axis gantryAxis, Direction facing) {
      float multiplier = 1.0F;
      if (gantryAxis == Axis.X && facing == Direction.UP) {
         multiplier *= -1.0F;
      }

      if (gantryAxis == Axis.Y && (facing == Direction.NORTH || facing == Direction.EAST)) {
         multiplier *= -1.0F;
      }

      return multiplier;
   }

   private Axis getGantryAxis() {
      Axis gantryAxis = Axis.X;

      for (Axis axis : Iterate.axes) {
         if (axis != this.rotationAxis && axis != this.facing.getAxis()) {
            gantryAxis = axis;
         }
      }

      return gantryAxis;
   }

   @Override
   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.gantryCogs, this.rotatingModel});
   }

   @Override
   protected void _delete() {
      super._delete();
      this.gantryCogs.delete();
   }

   @Override
   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      super.collectCrumblingInstances(consumer);
      consumer.accept(this.gantryCogs);
   }
}
