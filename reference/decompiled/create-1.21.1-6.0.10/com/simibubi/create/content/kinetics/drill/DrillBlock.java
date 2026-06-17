package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DrillBlock extends DirectionalKineticBlock implements IBE<DrillBlockEntity>, SimpleWaterloggedBlock {
   private static final int placementHelperId = PlacementHelpers.register(new DrillBlock.PlacementHelper());

   public DrillBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
   }

   public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn) {
      if (!(entityIn instanceof ItemEntity)) {
         if (new AABB(pos).deflate(0.1F).intersects(entityIn.getBoundingBox())) {
            this.withBlockEntityDo(worldIn, pos, be -> {
               if (be.getSpeed() != 0.0F) {
                  entityIn.hurt(CreateDamageSources.drill(worldIn), (float)getDamage(be.getSpeed()));
               }
            });
         }
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.CASING_12PX.get((Direction)state.getValue(FACING));
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      this.withBlockEntityDo(worldIn, pos, BlockBreakingKineticBlockEntity::destroyNextTick);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face == ((Direction)state.getValue(FACING)).getOpposite();
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
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

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState FluidState = context.getLevel().getFluidState(context.getClickedPos());
      return (BlockState)super.getStateForPlacement(context).setValue(BlockStateProperties.WATERLOGGED, FluidState.getType() == Fluids.WATER);
   }

   public static double getDamage(float speed) {
      float speedAbs = Math.abs(speed);
      double sub1 = (double)Math.min(speedAbs / 16.0F, 2.0F);
      double sub2 = (double)Math.min(speedAbs / 32.0F, 4.0F);
      double sub3 = (double)Math.min(speedAbs / 64.0F, 4.0F);
      return Mth.clamp(sub1 + sub2 + sub3, 1.0, 10.0);
   }

   @Override
   public Class<DrillBlockEntity> getBlockEntityClass() {
      return DrillBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends DrillBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends DrillBlockEntity>)AllBlockEntityTypes.DRILL.get();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      if (!player.isShiftKeyDown() && player.mayBuild() && placementHelper.matchesItem(stack)) {
         placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.MECHANICAL_DRILL::isIn;
      }

      public Predicate<BlockState> getStatePredicate() {
         return AllBlocks.MECHANICAL_DRILL::has;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(
            pos,
            ray.getLocation(),
            ((Direction)state.getValue(DirectionalKineticBlock.FACING)).getAxis(),
            dir -> world.getBlockState(pos.relative(dir)).canBeReplaced()
         );
         return directions.isEmpty()
            ? PlacementOffset.fail()
            : PlacementOffset.success(
               pos.relative(directions.get(0)),
               s -> (BlockState)s.setValue(DirectionalKineticBlock.FACING, (Direction)state.getValue(DirectionalKineticBlock.FACING))
            );
      }
   }
}
