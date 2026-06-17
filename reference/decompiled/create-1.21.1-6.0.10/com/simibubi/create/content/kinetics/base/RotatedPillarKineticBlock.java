package com.simibubi.create.content.kinetics.base;

import net.createmod.catnip.data.Iterate;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class RotatedPillarKineticBlock extends KineticBlock {
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.AXIS;

   public RotatedPillarKineticBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(AXIS, Axis.Y));
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      switch (rot) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            switch ((Axis)state.getValue(AXIS)) {
               case X:
                  return (BlockState)state.setValue(AXIS, Axis.Z);
               case Z:
                  return (BlockState)state.setValue(AXIS, Axis.X);
               default:
                  return state;
            }
         default:
            return state;
      }
   }

   public static Axis getPreferredAxis(BlockPlaceContext context) {
      Axis prefferedAxis = null;

      for (Direction side : Iterate.directions) {
         BlockState blockState = context.getLevel().getBlockState(context.getClickedPos().relative(side));
         if (blockState.getBlock() instanceof IRotate
            && ((IRotate)blockState.getBlock()).hasShaftTowards(context.getLevel(), context.getClickedPos().relative(side), blockState, side.getOpposite())) {
            if (prefferedAxis != null && prefferedAxis != side.getAxis()) {
               prefferedAxis = null;
               break;
            }

            prefferedAxis = side.getAxis();
         }
      }

      return prefferedAxis;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AXIS});
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Axis preferredAxis = getPreferredAxis(context);
      return preferredAxis == null || context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
         ? (BlockState)this.defaultBlockState()
            .setValue(
               AXIS,
               preferredAxis != null && context.getPlayer().isShiftKeyDown()
                  ? context.getClickedFace().getAxis()
                  : context.getNearestLookingDirection().getAxis()
            )
         : (BlockState)this.defaultBlockState().setValue(AXIS, preferredAxis);
   }
}
