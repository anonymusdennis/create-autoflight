package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.processing.AssemblyOperatorUseContext;
import com.simibubi.create.foundation.block.IBE;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.ParametersAreNonnullByDefault;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DeployerBlock extends DirectionalAxisKineticBlock implements IBE<DeployerBlockEntity> {
   private static final int placementHelperId = PlacementHelpers.register(new DeployerBlock.PlacementHelper());

   public DeployerBlock(Properties properties) {
      super(properties);
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.DEPLOYER_INTERACTION.get((Direction)state.getValue(FACING));
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.CASING_12PX.get((Direction)state.getValue(FACING));
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Vec3 normal = Vec3.atLowerCornerOf(((Direction)state.getValue(FACING)).getNormal());
      Vec3 location = context.getClickLocation().subtract(Vec3.atCenterOf(context.getClickedPos()).subtract(normal.scale(0.5))).multiply(normal);
      if (location.length() > 0.75) {
         if (!context.getLevel().isClientSide) {
            this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), DeployerBlockEntity::changeMode);
         }

         return InteractionResult.SUCCESS;
      } else {
         return super.onWrenched(state, context);
      }
   }

   @Override
   public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
      super.setPlacedBy(worldIn, pos, state, placer, stack);
      if (placer instanceof ServerPlayer) {
         this.withBlockEntityDo(worldIn, pos, dbe -> dbe.owner = placer.getUUID());
      }
   }

   @Override
   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!isMoving && !state.is(newState.getBlock())) {
         this.withBlockEntityDo(worldIn, pos, DeployerBlockEntity::discardPlayer);
      }

      super.onRemove(state, worldIn, pos, newState, isMoving);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      ItemStack heldByPlayer = stack.copy();
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      if (!player.isShiftKeyDown()
         && player.mayBuild()
         && placementHelper.matchesItem(heldByPlayer)
         && placementHelper.getOffset(player, level, state, pos, hitResult)
            .placeInWorld(level, (BlockItem)heldByPlayer.getItem(), player, hand, hitResult)
            .consumesAction()) {
         return ItemInteractionResult.SUCCESS;
      } else if (AllItems.WRENCH.isIn(heldByPlayer)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         Vec3 normal = Vec3.atLowerCornerOf(((Direction)state.getValue(FACING)).getNormal());
         Vec3 location = hitResult.getLocation().subtract(Vec3.atCenterOf(pos).subtract(normal.scale(0.5))).multiply(normal);
         if (location.length() < 0.75) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.withBlockEntityDo(level, pos, be -> {
               ItemStack heldByDeployer = be.player.getMainHandItem().copy();
               if (!heldByDeployer.isEmpty() || !heldByPlayer.isEmpty()) {
                  player.setItemInHand(hand, heldByDeployer);
                  be.player.setItemInHand(InteractionHand.MAIN_HAND, heldByPlayer);
                  be.notifyUpdate();
               }
            });
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   @Override
   public Class<DeployerBlockEntity> getBlockEntityClass() {
      return DeployerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends DeployerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends DeployerBlockEntity>)AllBlockEntityTypes.DEPLOYER.get();
   }

   @Override
   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      this.withBlockEntityDo(world, pos, DeployerBlockEntity::redstoneUpdate);
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
      this.withBlockEntityDo(world, pos, DeployerBlockEntity::redstoneUpdate);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context instanceof AssemblyOperatorUseContext ? Direction.DOWN : super.getFacingForPlacement(context);
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.DEPLOYER::isIn;
      }

      public Predicate<BlockState> getStatePredicate() {
         return AllBlocks.DEPLOYER::has;
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
               s -> (BlockState)((BlockState)s.setValue(DirectionalKineticBlock.FACING, (Direction)state.getValue(DirectionalKineticBlock.FACING)))
                     .setValue(
                        DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE,
                        (Boolean)state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
                     )
            );
      }
   }
}
