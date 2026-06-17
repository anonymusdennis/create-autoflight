package dev.eriksonn.aeronautics.content.blocks.propeller.small.wooden;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerVisual;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite.AndesitePropellerBlock;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class WoodenPropellerVisual extends SimplePropellerVisual<WoodenPropellerBlockEntity> {
   public WoodenPropellerVisual(VisualizationContext context, WoodenPropellerBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
   }

   @Override
   public PartialModel getModel(BlockState state) {
      return state.getValue(BasePropellerBlock.REVERSED) ? AeroPartialModels.WOODEN_PROPELLER_REVERSED : AeroPartialModels.WOODEN_PROPELLER;
   }

   @Override
   public float getAngle(float partialTicks) {
      BlockState state = ((WoodenPropellerBlockEntity)this.blockEntity).getBlockState();
      BlockPos pos = ((WoodenPropellerBlockEntity)this.blockEntity).getBlockPos();
      return super.getAngle(partialTicks) + rotationOffset(state, ((Direction)state.getValue(AndesitePropellerBlock.FACING)).getAxis(), pos);
   }
}
