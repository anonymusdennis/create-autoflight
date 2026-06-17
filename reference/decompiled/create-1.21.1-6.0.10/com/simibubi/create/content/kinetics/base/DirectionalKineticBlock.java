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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class DirectionalKineticBlock extends KineticBlock {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;

   public DirectionalKineticBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING});
      super.createBlockStateDefinition(builder);
   }

   public Direction getPreferredFacing(BlockPlaceContext context) {
      Direction prefferedSide = null;

      for (Direction side : Iterate.directions) {
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

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction preferred = this.getPreferredFacing(context);
      if (preferred == null || context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         Direction nearestLookingDirection = context.getNearestLookingDirection();
         return (BlockState)this.defaultBlockState()
            .setValue(
               FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? nearestLookingDirection : nearestLookingDirection.getOpposite()
            );
      } else {
         return (BlockState)this.defaultBlockState().setValue(FACING, preferred.getOpposite());
      }
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }
}
