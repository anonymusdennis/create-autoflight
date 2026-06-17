package dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerRenderer;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;

public class AndesitePropellerRenderer extends SimplePropellerRenderer<AndesitePropellerBlockEntity> {
   public AndesitePropellerRenderer(Context context) {
      super(context);
   }

   public PartialModel getCurrentModel(AndesitePropellerBlockEntity be) {
      return be.getBlockState().getValue(BasePropellerBlock.REVERSED) ? AeroPartialModels.ANDESITE_PROPELLER_REVERSED : AeroPartialModels.ANDESITE_PROPELLER;
   }

   public float getAngle(float partialTicks, Direction dir, AndesitePropellerBlockEntity be) {
      return super.getAngle(partialTicks, dir, be) + getRotationOffsetForPosition(be, be.getBlockPos(), dir.getAxis());
   }
}
