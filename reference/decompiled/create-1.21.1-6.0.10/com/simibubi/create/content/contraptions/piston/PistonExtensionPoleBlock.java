package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PistonExtensionPoleBlock extends WrenchableDirectionalBlock implements IWrenchable, SimpleWaterloggedBlock {
   private static final int placementHelperId = PlacementHelpers.register(PistonExtensionPoleBlock.PlacementHelper.get());

   public PistonExtensionPoleBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, Direction.UP)).setValue(BlockStateProperties.WATERLOGGED, false)
      );
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
      Axis axis = ((Direction)state.getValue(FACING)).getAxis();
      Direction direction = Direction.get(AxisDirection.POSITIVE, axis);
      BlockPos pistonHead = null;
      BlockPos pistonBase = null;

      for (int modifier : new int[]{1, -1}) {
         for (int offset = modifier; modifier * offset < MechanicalPistonBlock.maxAllowedPistonPoles(); offset += modifier) {
            BlockPos currentPos = pos.relative(direction, offset);
            BlockState block = worldIn.getBlockState(currentPos);
            if (!MechanicalPistonBlock.isExtensionPole(block) || axis != ((Direction)block.getValue(FACING)).getAxis()) {
               if (MechanicalPistonBlock.isPiston(block) && ((Direction)block.getValue(BlockStateProperties.FACING)).getAxis() == axis) {
                  pistonBase = currentPos;
               }

               if (MechanicalPistonBlock.isPistonHead(block) && ((Direction)block.getValue(BlockStateProperties.FACING)).getAxis() == axis) {
                  pistonHead = currentPos;
               }
               break;
            }
         }
      }

      if (pistonHead != null
         && pistonBase != null
         && worldIn.getBlockState(pistonHead).getValue(BlockStateProperties.FACING) == worldIn.getBlockState(pistonBase).getValue(BlockStateProperties.FACING)) {
         BlockPos basePos = pistonBase;
         BlockPos.betweenClosedStream(pistonBase, pistonHead)
            .filter(p -> !p.equals(pos) && !p.equals(basePos))
            .forEach(p -> worldIn.destroyBlock(p, !player.isCreative()));
         worldIn.setBlockAndUpdate(
            basePos, (BlockState)worldIn.getBlockState(basePos).setValue(MechanicalPistonBlock.STATE, MechanicalPistonBlock.PistonState.RETRACTED)
         );
         if (worldIn.getBlockEntity(basePos) instanceof MechanicalPistonBlockEntity baseBE) {
            baseBE.offset = 0.0F;
            baseBE.onLengthBroken();
         }
      }

      return super.playerWillDestroy(worldIn, pos, state, player);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.FOUR_VOXEL_POLE.get(((Direction)state.getValue(FACING)).getAxis());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState FluidState = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite()))
         .setValue(BlockStateProperties.WATERLOGGED, FluidState.getType() == Fluids.WATER);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      return placementHelper.matchesItem(stack) && !player.isShiftKeyDown()
         ? placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
         : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : Fluids.EMPTY.defaultFluidState();
   }

   @Override
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

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @MethodsReturnNonnullByDefault
   public static class PlacementHelper extends PoleHelper<Direction> {
      private static final PistonExtensionPoleBlock.PlacementHelper instance = new PistonExtensionPoleBlock.PlacementHelper();

      public static PistonExtensionPoleBlock.PlacementHelper get() {
         return instance;
      }

      private PlacementHelper() {
         super(AllBlocks.PISTON_EXTENSION_POLE::has, state -> ((Direction)state.getValue(DirectionalBlock.FACING)).getAxis(), DirectionalBlock.FACING);
      }

      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.PISTON_EXTENSION_POLE::isIn;
      }
   }
}
