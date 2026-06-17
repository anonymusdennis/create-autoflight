package com.simibubi.create.content.contraptions.actors.harvester;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

public class HarvesterActorVisual extends ActorVisual {
   static float originOffset = 0.0625F;
   static Vec3 rotOffset = new Vec3(0.5, (double)(-2.0F * originOffset + 0.5F), (double)(originOffset + 0.5F));
   protected TransformedInstance harvester;
   private Direction facing;
   protected float horizontalAngle;
   private double rotation;
   private double previousRotation;

   public HarvesterActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      super(visualizationContext, simulationWorld, movementContext);
      BlockState state = movementContext.state;
      this.facing = (Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING);
      this.harvester = (TransformedInstance)this.instancerProvider
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(this.getRollingPartial()))
         .createInstance();
      this.horizontalAngle = this.facing.toYRot() + (float)(this.facing.getAxis() == Axis.X ? 180 : 0);
      this.harvester.light(this.localBlockLight(), 0);
      this.harvester.setChanged();
   }

   protected PartialModel getRollingPartial() {
      return AllPartialModels.HARVESTER_BLADE;
   }

   protected Vec3 getRotationOffset() {
      return rotOffset;
   }

   protected double getRadius() {
      return 6.5;
   }

   @Override
   public void tick() {
      super.tick();
      this.previousRotation = this.rotation;
      if (!this.context.contraption.stalled
         && !this.context.disabled
         && !VecHelper.isVecPointingTowards(this.context.relativeMotion, this.facing.getOpposite())) {
         double arcLength = this.context.motion.length();
         double radians = arcLength * 16.0 / this.getRadius();
         float deg = AngleHelper.deg(radians);
         deg = (float)((int)(deg * 3000.0F) / 3000);
         this.rotation += (double)deg * 1.25;
         this.rotation %= 360.0;
      }
   }

   @Override
   public void beginFrame() {
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.harvester
                              .setIdentityTransform()
                              .translate(this.context.localPos))
                           .center())
                        .rotateYDegrees(this.horizontalAngle))
                     .uncenter())
                  .translate(this.getRotationOffset()))
               .rotateXDegrees((float)this.getRotation()))
            .translateBack(this.getRotationOffset()))
         .setChanged();
   }

   @Override
   protected void _delete() {
      this.harvester.delete();
   }

   protected double getRotation() {
      return (double)AngleHelper.angleLerp((double)AnimationTickHolder.getPartialTicks(), this.previousRotation, this.rotation);
   }
}
