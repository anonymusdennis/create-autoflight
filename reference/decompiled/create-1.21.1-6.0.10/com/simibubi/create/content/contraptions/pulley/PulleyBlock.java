package com.simibubi.create.content.contraptions.pulley;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PulleyBlock extends HorizontalAxisKineticBlock implements IBE<PulleyBlockEntity> {
   public PulleyBlock(Properties properties) {
      super(properties);
   }

   private static void onRopeBroken(Level world, BlockPos pulleyPos) {
      if (world.getBlockEntity(pulleyPos) instanceof PulleyBlockEntity pulley) {
         pulley.initialOffset = 0;
         pulley.onLengthBroken();
      }
   }

   @Override
   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      super.onRemove(state, worldIn, pos, newState, isMoving);
      if (!state.is(newState.getBlock())) {
         if (!worldIn.isClientSide) {
            BlockState below = worldIn.getBlockState(pos.below());
            if (below.getBlock() instanceof PulleyBlock.RopeBlockBase) {
               worldIn.destroyBlock(pos.below(), true);
            }
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (stack.isEmpty()) {
         this.withBlockEntityDo(level, pos, be -> be.assembleNextTick = true);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   public Class<PulleyBlockEntity> getBlockEntityClass() {
      return PulleyBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends PulleyBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PulleyBlockEntity>)AllBlockEntityTypes.ROPE_PULLEY.get();
   }

   public static class MagnetBlock extends PulleyBlock.RopeBlockBase {
      public MagnetBlock(Properties properties) {
         super(properties);
      }

      public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
         return AllShapes.PULLEY_MAGNET;
      }
   }

   public static class RopeBlock extends PulleyBlock.RopeBlockBase {
      public RopeBlock(Properties properties) {
         super(properties);
      }

      public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
         return AllShapes.FOUR_VOXEL_POLE.get(Direction.UP);
      }
   }

   private static class RopeBlockBase extends Block implements SimpleWaterloggedBlock {
      public RopeBlockBase(Properties properties) {
         super(properties);
         this.registerDefaultState((BlockState)super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
      }

      protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
         return false;
      }

      public PushReaction getPistonPushReaction(BlockState state) {
         return PushReaction.BLOCK;
      }

      public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
         return AllBlocks.ROPE_PULLEY.asStack();
      }

      public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
         if (!isMoving
            && (
               !state.hasProperty(BlockStateProperties.WATERLOGGED)
                  || !newState.hasProperty(BlockStateProperties.WATERLOGGED)
                  || state.getValue(BlockStateProperties.WATERLOGGED) == newState.getValue(BlockStateProperties.WATERLOGGED)
            )) {
            PulleyBlock.onRopeBroken(worldIn, pos.above());
            if (!worldIn.isClientSide) {
               BlockState above = worldIn.getBlockState(pos.above());
               BlockState below = worldIn.getBlockState(pos.below());
               if (above.getBlock() instanceof PulleyBlock.RopeBlockBase) {
                  worldIn.destroyBlock(pos.above(), true);
               }

               if (below.getBlock() instanceof PulleyBlock.RopeBlockBase) {
                  worldIn.destroyBlock(pos.below(), true);
               }
            }
         }

         if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
            worldIn.removeBlockEntity(pos);
         }
      }

      public FluidState getFluidState(BlockState state) {
         return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{BlockStateProperties.WATERLOGGED});
         super.createBlockStateDefinition(builder);
      }

      public BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
         if ((Boolean)state.getValue(BlockStateProperties.WATERLOGGED)) {
            world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
         }

         return state;
      }

      public BlockState getStateForPlacement(BlockPlaceContext context) {
         FluidState FluidState = context.getLevel().getFluidState(context.getClickedPos());
         return (BlockState)super.getStateForPlacement(context).setValue(BlockStateProperties.WATERLOGGED, FluidState.getType() == Fluids.WATER);
      }
   }
}
