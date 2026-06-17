package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class DrillActorVisual extends ActorVisual {
   TransformedInstance drillHead;
   private final Direction facing;
   private double rotation;
   private double previousRotation;

   public DrillActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld contraption, MovementContext context) {
      super(visualizationContext, contraption, context);
      BlockState state = context.state;
      this.facing = (Direction)state.getValue(DrillBlock.FACING);
      this.drillHead = (TransformedInstance)this.instancerProvider
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.DRILL_HEAD))
         .createInstance();
   }

   @Override
   public void tick() {
      this.previousRotation = this.rotation;
      if (!this.context.disabled && !VecHelper.isVecPointingTowards(this.context.relativeMotion, this.facing.getOpposite())) {
         float deg = this.context.getAnimationSpeed();
         this.rotation += (double)(deg / 20.0F);
         this.rotation %= 360.0;
      }
   }

   @Override
   public void beginFrame() {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.drillHead
                        .setIdentityTransform()
                        .translate(this.context.localPos))
                     .center())
                  .rotateToFace(this.facing.getOpposite()))
               .rotateZDegrees((float)this.getRotation()))
            .uncenter())
         .setChanged();
   }

   protected double getRotation() {
      return (double)AngleHelper.angleLerp((double)AnimationTickHolder.getPartialTicks(), this.previousRotation, this.rotation);
   }

   @Override
   protected void _delete() {
      this.drillHead.delete();
   }
}
