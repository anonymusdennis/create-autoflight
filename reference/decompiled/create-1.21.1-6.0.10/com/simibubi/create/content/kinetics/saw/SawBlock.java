package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.drill.DrillBlock;
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
import net.minecraft.core.Direction.AxisDirection;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SawBlock extends DirectionalAxisKineticBlock implements IBE<SawBlockEntity> {
   public static final BooleanProperty FLIPPED = BooleanProperty.create("flipped");
   private static final int placementHelperId = PlacementHelpers.register(new SawBlock.PlacementHelper());

   public SawBlock(Properties properties) {
      super(properties);
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{FLIPPED}));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState stateForPlacement = super.getStateForPlacement(context);
      Direction facing = (Direction)stateForPlacement.getValue(FACING);
      return (BlockState)stateForPlacement.setValue(
         FLIPPED, facing.getAxis() == Axis.Y && context.getHorizontalDirection().getAxisDirection() == AxisDirection.POSITIVE
      );
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      BlockState newState = super.getRotatedBlockState(originalState, targetedFace);
      if (((Direction)newState.getValue(FACING)).getAxis() != Axis.Y) {
         return newState;
      } else if (targetedFace.getAxis() != Axis.Y) {
         return newState;
      } else {
         if (!(Boolean)originalState.getValue(AXIS_ALONG_FIRST_COORDINATE)) {
            newState = (BlockState)newState.cycle(FLIPPED);
         }

         return newState;
      }
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      BlockState newState = super.rotate(state, rot);
      if (((Direction)state.getValue(FACING)).getAxis() != Axis.Y) {
         return newState;
      } else {
         if (rot.ordinal() % 2 == 1 && rot == Rotation.CLOCKWISE_90 != (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE)) {
            newState = (BlockState)newState.cycle(FLIPPED);
         }

         if (rot == Rotation.CLOCKWISE_180) {
            newState = (BlockState)newState.cycle(FLIPPED);
         }

         return newState;
      }
   }

   @Override
   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      BlockState newState = super.mirror(state, mirrorIn);
      if (((Direction)state.getValue(FACING)).getAxis() != Axis.Y) {
         return newState;
      } else {
         boolean alongX = (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE);
         if (alongX && mirrorIn == Mirror.FRONT_BACK) {
            newState = (BlockState)newState.cycle(FLIPPED);
         }

         if (!alongX && mirrorIn == Mirror.LEFT_RIGHT) {
            newState = (BlockState)newState.cycle(FLIPPED);
         }

         return newState;
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.CASING_12PX.get((Direction)state.getValue(FACING));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      if (!player.isShiftKeyDown()
         && player.mayBuild()
         && placementHelper.matchesItem(stack)
         && placementHelper.getOffset(player, level, state, pos, hitResult)
            .placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult)
            .consumesAction()) {
         return ItemInteractionResult.SUCCESS;
      } else if (!player.isSpectator() && stack.isEmpty()) {
         return state.getOptionalValue(FACING).orElse(Direction.WEST) != Direction.UP
            ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            : this.onBlockEntityUseItemOn(level, pos, be -> {
               for (int i = 0; i < be.inventory.getSlots(); i++) {
                  ItemStack heldItemStack = be.inventory.getStackInSlot(i);
                  if (!level.isClientSide && !heldItemStack.isEmpty()) {
                     player.getInventory().placeItemBackInInventory(heldItemStack);
                  }
               }

               be.inventory.clear();
               be.notifyUpdate();
               return ItemInteractionResult.SUCCESS;
            });
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn) {
      if (!(entityIn instanceof ItemEntity)) {
         if (new AABB(pos).deflate(0.1F).intersects(entityIn.getBoundingBox())) {
            this.withBlockEntityDo(worldIn, pos, be -> {
               if (be.getSpeed() != 0.0F) {
                  entityIn.hurt(CreateDamageSources.saw(worldIn), (float)DrillBlock.getDamage(be.getSpeed()));
               }
            });
         }
      }
   }

   public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
      super.updateEntityAfterFallOn(worldIn, entityIn);
      if (entityIn instanceof ItemEntity) {
         if (!entityIn.level().isClientSide) {
            BlockPos pos = entityIn.blockPosition();
            this.withBlockEntityDo(entityIn.level(), pos, be -> {
               if (be.getSpeed() != 0.0F) {
                  be.insertItem((ItemEntity)entityIn);
               }
            });
         }
      }
   }

   public PushReaction getPistonPushReaction(BlockState state) {
      return PushReaction.NORMAL;
   }

   public static boolean isHorizontal(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis().isHorizontal();
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return isHorizontal(state) ? ((Direction)state.getValue(FACING)).getAxis() : super.getRotationAxis(state);
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return isHorizontal(state) ? face == ((Direction)state.getValue(FACING)).getOpposite() : super.hasShaftTowards(world, pos, state, face);
   }

   @Override
   public Class<SawBlockEntity> getBlockEntityClass() {
      return SawBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SawBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SawBlockEntity>)AllBlockEntityTypes.SAW.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.MECHANICAL_SAW::isIn;
      }

      public Predicate<BlockState> getStatePredicate() {
         return state -> AllBlocks.MECHANICAL_SAW.has(state);
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
               s -> (BlockState)((BlockState)((BlockState)s.setValue(DirectionalKineticBlock.FACING, (Direction)state.getValue(DirectionalKineticBlock.FACING)))
                        .setValue(
                           DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE,
                           (Boolean)state.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)
                        ))
                     .setValue(SawBlock.FLIPPED, (Boolean)state.getValue(SawBlock.FLIPPED))
            );
      }
   }
}
