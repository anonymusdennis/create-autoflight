package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.world.level.block.state.BlockState;

public class MechanicalPistonRenderer extends KineticBlockEntityRenderer<MechanicalPistonBlockEntity> {
   public MechanicalPistonRenderer(Context context) {
      super(context);
   }

   protected BlockState getRenderedBlockState(MechanicalPistonBlockEntity be) {
      return shaft(getRotationAxisOf(be));
   }
}
