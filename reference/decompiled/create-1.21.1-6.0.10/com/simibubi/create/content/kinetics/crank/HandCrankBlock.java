package com.simibubi.create.content.kinetics.crank;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HandCrankBlock extends DirectionalKineticBlock implements IBE<HandCrankBlockEntity>, ProperWaterloggedBlock {
   public HandCrankBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.CRANK.get((Direction)state.getValue(FACING));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{WATERLOGGED}));
   }

   public int getRotationSpeed() {
      return 32;
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player.isSpectator()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         this.withBlockEntityDo(level, pos, be -> be.turn(player.isShiftKeyDown()));
         if (!stack.is((Item)AllItems.EXTENDO_GRIP.get())) {
            player.causeFoodExhaustion((float)this.getRotationSpeed() * AllConfigs.server().kinetics.crankHungerMultiplier.getF());
         }

         if (player.getFoodData().getFoodLevel() == 0) {
            AllAdvancements.HAND_CRANK.awardTo(player);
         }

         return ItemInteractionResult.SUCCESS;
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      Direction preferred = this.getPreferredFacing(context);
      BlockState defaultBlockState = this.withWater(this.defaultBlockState(), context);
      return preferred != null && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
         ? (BlockState)defaultBlockState.setValue(FACING, preferred.getOpposite())
         : (BlockState)defaultBlockState.setValue(FACING, context.getClickedFace());
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      Direction facing = ((Direction)state.getValue(FACING)).getOpposite();
      BlockPos neighbourPos = pos.relative(facing);
      BlockState neighbour = worldIn.getBlockState(neighbourPos);
      return !neighbour.getCollisionShape(worldIn, neighbourPos).isEmpty();
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         Direction blockFacing = (Direction)state.getValue(FACING);
         if (fromPos.equals(pos.relative(blockFacing.getOpposite())) && !this.canSurvive(state, worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
         }
      }
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == ((Direction)state.getValue(FACING)).getOpposite();
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   public Class<HandCrankBlockEntity> getBlockEntityClass() {
      return HandCrankBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends HandCrankBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends HandCrankBlockEntity>)AllBlockEntityTypes.HAND_CRANK.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }
}
