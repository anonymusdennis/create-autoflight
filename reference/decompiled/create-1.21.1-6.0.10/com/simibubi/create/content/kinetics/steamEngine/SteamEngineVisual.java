package com.simibubi.create.content.kinetics.steamEngine;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Objects;
import java.util.function.Consumer;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;

public class SteamEngineVisual extends AbstractBlockEntityVisual<SteamEngineBlockEntity> implements SimpleDynamicVisual {
   protected final TransformedInstance piston;
   protected final TransformedInstance linkage;
   protected final TransformedInstance connector;
   private Float lastAngle = Float.NaN;
   private Axis lastAxis = null;

   public SteamEngineVisual(VisualizationContext context, SteamEngineBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      this.piston = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ENGINE_PISTON))
         .createInstance();
      this.linkage = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ENGINE_LINKAGE))
         .createInstance();
      this.connector = (TransformedInstance)this.instancerProvider()
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.ENGINE_CONNECTOR))
         .createInstance();
      this.animate();
   }

   public void beginFrame(Context ctx) {
      this.animate();
   }

   private void animate() {
      Float angle = ((SteamEngineBlockEntity)this.blockEntity).getTargetAngle();
      Axis axis = Axis.Y;
      PoweredShaftBlockEntity shaft = ((SteamEngineBlockEntity)this.blockEntity).getShaft();
      if (shaft != null) {
         axis = KineticBlockEntityRenderer.getRotationAxisOf(shaft);
      }

      if (!Objects.equals(angle, this.lastAngle) || this.lastAxis != axis) {
         this.lastAngle = angle;
         this.lastAxis = axis;
         if (angle == null) {
            this.piston.setVisible(false);
            this.linkage.setVisible(false);
            this.connector.setVisible(false);
         } else {
            this.piston.setVisible(true);
            this.linkage.setVisible(true);
            this.connector.setVisible(true);
            Direction facing = SteamEngineBlock.getFacing(this.blockState);
            Axis facingAxis = facing.getAxis();
            boolean roll90 = facingAxis.isHorizontal() && axis == Axis.Y || facingAxis.isVertical() && axis == Axis.Z;
            float piston = 0.375F * Mth.sin(angle) - Mth.sqrt(Mth.square(0.875F) - Mth.square(0.375F) * Mth.square(Mth.cos(angle)));
            float distance = Mth.sqrt(Mth.square(piston - 0.375F * Mth.sin(angle)));
            float angle2 = (float)Math.acos((double)(distance / 0.875F)) * (Mth.cos(angle) >= 0.0F ? 1.0F : -1.0F);
            this.transformed(this.piston, facing, roll90).translate(0.0F, piston + 1.25F, 0.0F).setChanged();
            ((TransformedInstance)((TransformedInstance)this.transformed(this.linkage, facing, roll90).center()).translate(0.0F, 1.0F, 0.0F).uncenter())
               .translate(0.0F, piston + 1.25F, 0.0F)
               .translate(0.0F, 0.25F, 0.5F)
               .rotateX(angle2)
               .translate(0.0F, -0.25F, -0.5F)
               .setChanged();
            ((TransformedInstance)((TransformedInstance)this.transformed(this.connector, facing, roll90).translate(0.0F, 2.0F, 0.0F).center())
                  .rotateX(-(angle + (float) (Math.PI / 2)))
                  .uncenter())
               .setChanged();
         }
      }
   }

   protected TransformedInstance transformed(TransformedInstance modelData, Direction facing, boolean roll90) {
      return (TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)modelData.setIdentityTransform()
                        .translate(this.getVisualPosition()))
                     .center())
                  .rotateYDegrees(AngleHelper.horizontalAngle(facing)))
               .rotateXDegrees(AngleHelper.verticalAngle(facing) + 90.0F))
            .rotateYDegrees(roll90 ? -90.0F : 0.0F))
         .uncenter();
   }

   public void updateLight(float partialTick) {
      this.relight(new FlatLit[]{this.piston, this.linkage, this.connector});
   }

   protected void _delete() {
      this.piston.delete();
      this.linkage.delete();
      this.connector.delete();
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      consumer.accept(this.piston);
      consumer.accept(this.linkage);
      consumer.accept(this.connector);
   }
}
