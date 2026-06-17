package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public class DeployerActorVisual extends ActorVisual {
   Direction facing;
   boolean stationaryTimer;
   TransformedInstance pole;
   TransformedInstance hand;
   RotatingInstance shaft;
   Matrix4fc baseHandTransform;
   Matrix4fc basePoleTransform;

   public DeployerActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext context) {
      super(visualizationContext, simulationWorld, context);
      BlockState state = context.state;
      DeployerBlockEntity.Mode mode = (DeployerBlockEntity.Mode)NBTHelper.readEnum(context.blockEntityData, "Mode", DeployerBlockEntity.Mode.class);
      PartialModel handPose = DeployerRenderer.getHandPose(mode);
      this.stationaryTimer = context.data.contains("StationaryTimer");
      this.facing = (Direction)state.getValue(DirectionalKineticBlock.FACING);
      boolean rotatePole = (Boolean)state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE) ^ this.facing.getAxis() == Axis.Z;
      float yRot = AngleHelper.horizontalAngle(this.facing);
      float xRot = this.facing == Direction.UP ? 270.0F : (this.facing == Direction.DOWN ? 90.0F : 0.0F);
      float zRot = rotatePole ? 90.0F : 0.0F;
      this.pole = (TransformedInstance)this.instancerProvider
         .instancer(InstanceTypes.TRANSFORMED, Models.partial(AllPartialModels.DEPLOYER_POLE))
         .createInstance();
      this.hand = (TransformedInstance)this.instancerProvider.instancer(InstanceTypes.TRANSFORMED, Models.partial(handPose)).createInstance();
      Axis axis = KineticBlockEntityVisual.rotationAxis(state);
      this.shaft = ((RotatingInstance)this.instancerProvider.instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT)).createInstance())
         .rotateToFace(axis);
      int blockLight = this.localBlockLight();
      this.shaft
         .setRotationAxis(axis)
         .setRotationOffset(KineticBlockEntityVisual.rotationOffset(state, axis, context.localPos))
         .setPosition(context.localPos)
         .light(blockLight, 0)
         .setChanged();
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.pole
                           .translate(context.localPos))
                        .center())
                     .rotate(yRot * (float) (Math.PI / 180.0), Direction.UP))
                  .rotate(xRot * (float) (Math.PI / 180.0), Direction.EAST))
               .rotate(zRot * (float) (Math.PI / 180.0), Direction.SOUTH))
            .uncenter())
         .light(blockLight, 0)
         .setChanged();
      this.basePoleTransform = new Matrix4f(this.pole.pose);
      ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.hand.translate(context.localPos))
                     .center())
                  .rotate(yRot * (float) (Math.PI / 180.0), Direction.UP))
               .rotate(xRot * (float) (Math.PI / 180.0), Direction.EAST))
            .uncenter())
         .light(blockLight, 0)
         .setChanged();
      this.baseHandTransform = new Matrix4f(this.hand.pose);
   }

   @Override
   public void beginFrame() {
      float distance = this.deploymentDistance();
      ((TransformedInstance)this.pole.setTransform(this.basePoleTransform).translateZ(distance)).setChanged();
      ((TransformedInstance)this.hand.setTransform(this.baseHandTransform).translateZ(distance)).setChanged();
   }

   private float deploymentDistance() {
      double factor;
      if (this.context.disabled) {
         factor = 0.0;
      } else if (!this.context.contraption.stalled && this.context.position != null && !this.context.data.contains("StationaryTimer")) {
         Vec3 center = VecHelper.getCenterOf(BlockPos.containing(this.context.position));
         double distance = this.context.position.distanceTo(center);
         double nextDistance = this.context.position.add(this.context.motion).distanceTo(center);
         factor = 0.5 - Mth.clamp(Mth.lerp((double)AnimationTickHolder.getPartialTicks(), distance, nextDistance), 0.0, 1.0);
      } else {
         factor = (double)(Mth.sin(AnimationTickHolder.getRenderTime() * 0.5F) * 0.25F + 0.25F);
      }

      return (float)factor;
   }

   @Override
   protected void _delete() {
      this.pole.delete();
      this.hand.delete();
      this.shaft.delete();
   }
}
