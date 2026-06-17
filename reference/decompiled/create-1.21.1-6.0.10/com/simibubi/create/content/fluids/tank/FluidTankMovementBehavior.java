package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

public class FluidTankMovementBehavior implements MovementBehaviour {
   @Override
   public boolean mustTickWhileDisabled() {
      return true;
   }

   @Override
   public void tick(MovementContext context) {
      if (context.world.isClientSide && context.contraption.getBlockEntityClientSide(context.localPos) instanceof FluidTankBlockEntity tank) {
         tank.getFluidLevel().tickChaser();
      }
   }
}
