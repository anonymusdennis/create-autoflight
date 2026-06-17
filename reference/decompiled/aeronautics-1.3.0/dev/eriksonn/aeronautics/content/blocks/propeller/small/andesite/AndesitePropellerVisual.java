package dev.eriksonn.aeronautics.content.blocks.propeller.small.andesite;

import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.BasePropellerBlock;
import dev.eriksonn.aeronautics.content.blocks.propeller.small.SimplePropellerVisual;
import dev.eriksonn.aeronautics.index.AeroPartialModels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class AndesitePropellerVisual extends SimplePropellerVisual<AndesitePropellerBlockEntity> {
   public AndesitePropellerVisual(VisualizationContext context, AndesitePropellerBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
   }

   @Override
   public PartialModel getModel(BlockState state) {
      return state.getValue(BasePropellerBlock.REVERSED) ? AeroPartialModels.ANDESITE_PROPELLER_REVERSED : AeroPartialModels.ANDESITE_PROPELLER;
   }

   @Override
   public float getAngle(float partialTicks) {
      BlockState state = ((AndesitePropellerBlockEntity)this.blockEntity).getBlockState();
      BlockPos pos = ((AndesitePropellerBlockEntity)this.blockEntity).getBlockPos();
      return super.getAngle(partialTicks) + rotationOffset(state, ((Direction)state.getValue(AndesitePropellerBlock.FACING)).getAxis(), pos);
   }
}
