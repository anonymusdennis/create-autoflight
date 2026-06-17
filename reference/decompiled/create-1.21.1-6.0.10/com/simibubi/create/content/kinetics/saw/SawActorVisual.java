package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

public class SawActorVisual extends ActorVisual {
   private final RotatingInstance shaft;

   public SawActorVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      super(visualizationContext, simulationWorld, movementContext);
      BlockState state = movementContext.state;
      BlockPos localPos = movementContext.localPos;
      this.shaft = SawVisual.shaft(this.instancerProvider, state);
      Axis axis = KineticBlockEntityVisual.rotationAxis(state);
      this.shaft
         .setRotationAxis(axis)
         .setRotationOffset(KineticBlockEntityVisual.rotationOffset(state, axis, localPos))
         .setPosition(localPos)
         .light(this.localBlockLight(), 0)
         .setChanged();
   }

   @Override
   protected void _delete() {
      this.shaft.delete();
   }
}
