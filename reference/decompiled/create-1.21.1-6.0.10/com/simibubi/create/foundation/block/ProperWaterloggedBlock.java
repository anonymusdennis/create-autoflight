package com.simibubi.create.foundation.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public interface ProperWaterloggedBlock extends SimpleWaterloggedBlock {
   BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

   default FluidState fluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   default void updateWater(LevelAccessor level, BlockState state, BlockPos pos) {
      if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
         level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }
   }

   default BlockState withWater(BlockState placementState, BlockPlaceContext ctx) {
      return withWater(ctx.getLevel(), placementState, ctx.getClickedPos());
   }

   static BlockState withWater(LevelAccessor level, BlockState placementState, BlockPos pos) {
      if (placementState == null) {
         return null;
      } else {
         FluidState ifluidstate = level.getFluidState(pos);
         if (placementState.isAir()) {
            return ifluidstate.getType() == Fluids.WATER ? ifluidstate.createLegacyBlock() : placementState;
         } else {
            return !(placementState.getBlock() instanceof SimpleWaterloggedBlock)
               ? placementState
               : (BlockState)placementState.setValue(BlockStateProperties.WATERLOGGED, ifluidstate.getType() == Fluids.WATER);
         }
      }
   }
}
