package com.simibubi.create.content.contraptions.bearing;

import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.OrientedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;

public class StabilizedBearingVisual extends ActorVisual {
   final OrientedInstance topInstance;
   final RotatingInstance shaft;
   final Direction facing;
   final Axis rotationAxis;
   final Quaternionf blockOrientation;

   public StabilizedBearingVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      super(visualizationContext, simulationWorld, movementContext);
      BlockState blockState = movementContext.state;
      this.facing = (Direction)blockState.getValue(BlockStateProperties.FACING);
      this.rotationAxis = Axis.of(Direction.get(AxisDirection.POSITIVE, this.facing.getAxis()).step());
      this.blockOrientation = BearingVisual.getBlockStateOrientation(this.facing);
      this.topInstance = (OrientedInstance)this.instancerProvider
         .instancer(InstanceTypes.ORIENTED, Models.partial(AllPartialModels.BEARING_TOP))
         .createInstance();
      int blockLight = this.localBlockLight();
      this.topInstance.position(movementContext.localPos).rotation(this.blockOrientation).light(blockLight, 0).setChanged();
      this.shaft = (RotatingInstance)this.instancerProvider.instancer(AllInstanceTypes.ROTATING, Models.partial(AllPartialModels.SHAFT_HALF)).createInstance();
      net.minecraft.core.Direction.Axis axis = KineticBlockEntityVisual.rotationAxis(blockState);
      this.shaft
         .setRotationAxis(axis)
         .setRotationOffset(KineticBlockEntityVisual.rotationOffset(blockState, axis, movementContext.localPos))
         .setPosition(movementContext.localPos)
         .rotateToFace(Direction.SOUTH, ((Direction)blockState.getValue(BlockStateProperties.FACING)).getOpposite())
         .light(blockLight, 0)
         .setChanged();
   }

   @Override
   public void beginFrame() {
      float counterRotationAngle = StabilizedBearingMovementBehaviour.getCounterRotationAngle(this.context, this.facing, AnimationTickHolder.getPartialTicks());
      Quaternionf rotation = this.rotationAxis.rotationDegrees(counterRotationAngle);
      rotation.mul(this.blockOrientation);
      this.topInstance.rotation(rotation).setChanged();
   }

   @Override
   protected void _delete() {
      this.topInstance.delete();
      this.shaft.delete();
   }
}
