package dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;

public class WoodenPropellerRenderer extends SimplePropellerRenderer<WoodenPropellerBlockEntity> {
   public WoodenPropellerRenderer(Context context) {
      super(context);
   }

   public PartialModel getCurrentModel(WoodenPropellerBlockEntity be) {
      return be.getBlockState().getValue(BasePropellerBlock.REVERSED) ? AeroPartialModels.WOODEN_PROPELLER_REVERSED : AeroPartialModels.WOODEN_PROPELLER;
   }

   public float getAngle(float partialTicks, Direction dir, WoodenPropellerBlockEntity be) {
      return super.getAngle(partialTicks, dir, be) + getRotationOffsetForPosition(be, be.getBlockPos(), dir.getAxis());
   }
}
