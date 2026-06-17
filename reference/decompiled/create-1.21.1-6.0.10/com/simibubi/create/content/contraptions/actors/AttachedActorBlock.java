package com.simibubi.create.content.contraptions.actors;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class AttachedActorBlock extends HorizontalDirectionalBlock implements IWrenchable, ProperWaterloggedBlock {
   protected AttachedActorBlock(Properties p_i48377_1_) {
      super(p_i48377_1_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      return InteractionResult.FAIL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      Direction direction = (Direction)state.getValue(FACING);
      return AllShapes.HARVESTER_BASE.get(direction);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACING, WATERLOGGED});
      super.createBlockStateDefinition(builder);
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      Direction direction = (Direction)state.getValue(FACING);
      BlockPos offset = pos.relative(direction.getOpposite());
      return BlockHelper.hasBlockSolidSide(worldIn.getBlockState(offset), worldIn, offset, direction);
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction facing;
      if (context.getClickedFace().getAxis().isVertical()) {
         facing = context.getHorizontalDirection().getOpposite();
      } else {
         BlockState blockState = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
         if (blockState.getBlock() instanceof AttachedActorBlock) {
            facing = (Direction)blockState.getValue(FACING);
         } else {
            facing = context.getClickedFace();
         }
      }

      return this.withWater((BlockState)this.defaultBlockState().setValue(FACING, facing), context);
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }
}
