package com.simibubi.create.content.kinetics.base;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class HorizontalKineticBlock extends KineticBlock {
   public static final Property<Direction> HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

   public HorizontalKineticBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{HORIZONTAL_FACING});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)this.defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
   }

   public Direction getPreferredHorizontalFacing(BlockPlaceContext context) {
      Direction prefferedSide = null;

      for (Direction side : Iterate.horizontalDirections) {
         BlockState blockState = context.getLevel().getBlockState(context.getClickedPos().relative(side));
         if (blockState.getBlock() instanceof IRotate
            && ((IRotate)blockState.getBlock()).hasShaftTowards(context.getLevel(), context.getClickedPos().relative(side), blockState, side.getOpposite())) {
            if (prefferedSide != null && prefferedSide.getAxis() != side.getAxis()) {
               prefferedSide = null;
               break;
            }

            prefferedSide = side;
         }
      }

      return prefferedSide;
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(HORIZONTAL_FACING, rot.rotate((Direction)state.getValue(HORIZONTAL_FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(HORIZONTAL_FACING)));
   }
}
