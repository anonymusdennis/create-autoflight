package com.simibubi.create.content.kinetics.base;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class HorizontalAxisKineticBlock extends KineticBlock {
   public static final Property<Axis> HORIZONTAL_AXIS = BlockStateProperties.HORIZONTAL_AXIS;

   public HorizontalAxisKineticBlock(Properties properties) {
      super(properties);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{HORIZONTAL_AXIS});
      super.createBlockStateDefinition(builder);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Axis preferredAxis = getPreferredHorizontalAxis(context);
      return preferredAxis != null
         ? (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, preferredAxis)
         : (BlockState)this.defaultBlockState().setValue(HORIZONTAL_AXIS, context.getHorizontalDirection().getClockWise().getAxis());
   }

   public static Axis getPreferredHorizontalAxis(BlockPlaceContext context) {
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

      return prefferedSide == null ? null : prefferedSide.getAxis();
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(HORIZONTAL_AXIS);
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(HORIZONTAL_AXIS);
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      Axis axis = (Axis)state.getValue(HORIZONTAL_AXIS);
      return (BlockState)state.setValue(HORIZONTAL_AXIS, rot.rotate(Direction.get(AxisDirection.POSITIVE, axis)).getAxis());
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state;
   }
}
