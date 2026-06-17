package com.simibubi.create.content.logistics.funnel;

import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class BrassFunnelBlock extends FunnelBlock {
   public BrassFunnelBlock(Properties p_i48415_1_) {
      super(p_i48415_1_);
   }

   @Override
   public BlockState getEquivalentBeltFunnel(BlockGetter world, BlockPos pos, BlockState state) {
      Direction facing = this.getFacing(state);
      return (BlockState)((BlockState)AllBlocks.BRASS_BELT_FUNNEL.getDefaultState().setValue(BeltFunnelBlock.HORIZONTAL_FACING, facing))
         .setValue(POWERED, (Boolean)state.getValue(POWERED));
   }
}
