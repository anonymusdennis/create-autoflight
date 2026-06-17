package com.simibubi.create.content.kinetics.base;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

@MethodsReturnNonnullByDefault
public abstract class AbstractEncasedShaftBlock extends RotatedPillarKineticBlock {
   public AbstractEncasedShaftBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public PushReaction getPistonPushReaction(@Nullable BlockState state) {
      return PushReaction.NORMAL;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
         return super.getStateForPlacement(context);
      } else {
         Axis preferredAxis = getPreferredAxis(context);
         return (BlockState)this.defaultBlockState().setValue(AXIS, preferredAxis == null ? context.getNearestLookingDirection().getAxis() : preferredAxis);
      }
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == state.getValue(AXIS);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return (Axis)state.getValue(AXIS);
   }
}
