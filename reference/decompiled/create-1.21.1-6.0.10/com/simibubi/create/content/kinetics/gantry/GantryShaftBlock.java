package com.simibubi.create.content.kinetics.gantry;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class GantryShaftBlock extends DirectionalKineticBlock implements IBE<GantryShaftBlockEntity> {
   public static final Property<GantryShaftBlock.Part> PART = EnumProperty.create("part", GantryShaftBlock.Part.class);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int placementHelperId = PlacementHelpers.register(new GantryShaftBlock.PlacementHelper());

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{PART, POWERED}));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
      return !placementHelper.matchesItem(stack)
         ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
         : placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
   }

   public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
      return AllShapes.EIGHT_VOXEL_POLE.get(((Direction)state.getValue(FACING)).getAxis());
   }

   public RenderShape getRenderShape(BlockState state) {
      return RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighbour, LevelAccessor world, BlockPos pos, BlockPos neighbourPos) {
      Direction facing = (Direction)state.getValue(FACING);
      Axis axis = facing.getAxis();
      if (direction.getAxis() != axis) {
         return state;
      } else {
         boolean connect = AllBlocks.GANTRY_SHAFT.has(neighbour) && neighbour.getValue(FACING) == facing;
         GantryShaftBlock.Part part = (GantryShaftBlock.Part)state.getValue(PART);
         if (direction.getAxisDirection() == facing.getAxisDirection()) {
            if (connect) {
               if (part == GantryShaftBlock.Part.END) {
                  part = GantryShaftBlock.Part.MIDDLE;
               }

               if (part == GantryShaftBlock.Part.SINGLE) {
                  part = GantryShaftBlock.Part.START;
               }
            } else {
               if (part == GantryShaftBlock.Part.MIDDLE) {
                  part = GantryShaftBlock.Part.END;
               }

               if (part == GantryShaftBlock.Part.START) {
                  part = GantryShaftBlock.Part.SINGLE;
               }
            }
         } else if (connect) {
            if (part == GantryShaftBlock.Part.START) {
               part = GantryShaftBlock.Part.MIDDLE;
            }

            if (part == GantryShaftBlock.Part.SINGLE) {
               part = GantryShaftBlock.Part.END;
            }
         } else {
            if (part == GantryShaftBlock.Part.MIDDLE) {
               part = GantryShaftBlock.Part.START;
            }

            if (part == GantryShaftBlock.Part.END) {
               part = GantryShaftBlock.Part.SINGLE;
            }
         }

         return (BlockState)state.setValue(PART, part);
      }
   }

   public GantryShaftBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(PART, GantryShaftBlock.Part.SINGLE));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      BlockPos pos = context.getClickedPos();
      Level world = context.getLevel();
      Direction face = context.getClickedFace();
      BlockState neighbour = world.getBlockState(pos.relative(((Direction)state.getValue(FACING)).getOpposite()));
      BlockState clickedState = AllBlocks.GANTRY_SHAFT.has(neighbour) ? neighbour : world.getBlockState(pos.relative(face.getOpposite()));
      if (AllBlocks.GANTRY_SHAFT.has(clickedState) && ((Direction)clickedState.getValue(FACING)).getAxis() == ((Direction)state.getValue(FACING)).getAxis()) {
         Direction facing = (Direction)clickedState.getValue(FACING);
         state = (BlockState)state.setValue(FACING, context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? facing.getOpposite() : facing);
      }

      return (BlockState)state.setValue(POWERED, this.shouldBePowered(state, world, pos));
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult onWrenched = super.onWrenched(state, context);
      if (onWrenched.consumesAction()) {
         BlockPos pos = context.getClickedPos();
         Level world = context.getLevel();
         this.neighborChanged(world.getBlockState(pos), world, pos, state.getBlock(), pos, false);
      }

      return onWrenched;
   }

   @Override
   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, worldIn, pos, oldState, isMoving);
      if (!worldIn.isClientSide() && oldState.is((Block)AllBlocks.GANTRY_SHAFT.get())) {
         GantryShaftBlock.Part oldPart = (GantryShaftBlock.Part)oldState.getValue(PART);
         GantryShaftBlock.Part part = (GantryShaftBlock.Part)state.getValue(PART);
         if (oldPart != GantryShaftBlock.Part.MIDDLE && part == GantryShaftBlock.Part.MIDDLE
            || oldPart == GantryShaftBlock.Part.SINGLE && part != GantryShaftBlock.Part.SINGLE) {
            BlockEntity be = worldIn.getBlockEntity(pos);
            if (be instanceof GantryShaftBlockEntity) {
               ((GantryShaftBlockEntity)be).checkAttachedCarriageBlocks();
            }
         }
      }
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block p_220069_4_, BlockPos p_220069_5_, boolean p_220069_6_) {
      if (!worldIn.isClientSide) {
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         boolean shouldPower = worldIn.hasNeighborSignal(pos);
         if (!previouslyPowered && !shouldPower && this.shouldBePowered(state, worldIn, pos)) {
            worldIn.setBlock(pos, (BlockState)state.setValue(POWERED, true), 3);
         } else if (previouslyPowered != shouldPower) {
            List<BlockPos> toUpdate = new ArrayList<>();
            Direction facing = (Direction)state.getValue(FACING);
            Axis axis = facing.getAxis();

            for (Direction d : Iterate.directionsInAxis(axis)) {
               for (BlockPos currentPos = pos.relative(d); worldIn.isLoaded(currentPos); currentPos = currentPos.relative(d)) {
                  BlockState currentState = worldIn.getBlockState(currentPos);
                  if (!(currentState.getBlock() instanceof GantryShaftBlock) || currentState.getValue(FACING) != facing) {
                     break;
                  }

                  if (!shouldPower && (Boolean)currentState.getValue(POWERED) && worldIn.hasNeighborSignal(currentPos)) {
                     return;
                  }

                  if ((Boolean)currentState.getValue(POWERED) == shouldPower) {
                     break;
                  }

                  toUpdate.add(currentPos);
               }
            }

            toUpdate.add(pos);

            for (BlockPos blockPos : toUpdate) {
               BlockState blockState = worldIn.getBlockState(blockPos);
               BlockEntity be = worldIn.getBlockEntity(blockPos);
               if (be instanceof KineticBlockEntity) {
                  ((KineticBlockEntity)be).detachKinetics();
               }

               if (blockState.getBlock() instanceof GantryShaftBlock) {
                  worldIn.setBlock(blockPos, (BlockState)blockState.setValue(POWERED, shouldPower), 2);
               }
            }
         }
      }
   }

   protected boolean shouldBePowered(BlockState state, Level worldIn, BlockPos pos) {
      boolean shouldPower = worldIn.hasNeighborSignal(pos);
      Direction facing = (Direction)state.getValue(FACING);

      for (Direction d : Iterate.directionsInAxis(facing.getAxis())) {
         BlockPos neighbourPos = pos.relative(d);
         if (worldIn.isLoaded(neighbourPos)) {
            BlockState neighbourState = worldIn.getBlockState(neighbourPos);
            if (neighbourState.getBlock() instanceof GantryShaftBlock && neighbourState.getValue(FACING) == facing) {
               shouldPower |= neighbourState.getValue(POWERED);
            }
         }
      }

      return shouldPower;
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() == ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      return super.areStatesKineticallyEquivalent(oldState, newState) && oldState.getValue(POWERED) == newState.getValue(POWERED);
   }

   @Override
   public float getParticleTargetRadius() {
      return 0.35F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 0.25F;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<GantryShaftBlockEntity> getBlockEntityClass() {
      return GantryShaftBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends GantryShaftBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends GantryShaftBlockEntity>)AllBlockEntityTypes.GANTRY_SHAFT.get();
   }

   public static enum Part implements StringRepresentable {
      START,
      MIDDLE,
      END,
      SINGLE;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }

   public static class PlacementHelper extends PoleHelper<Direction> {
      public PlacementHelper() {
         super(AllBlocks.GANTRY_SHAFT::has, s -> ((Direction)s.getValue(DirectionalKineticBlock.FACING)).getAxis(), DirectionalKineticBlock.FACING);
      }

      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.GANTRY_SHAFT::isIn;
      }

      @Override
      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
         offset.withTransform(
            offset.getTransform().andThen(s -> (BlockState)s.setValue(GantryShaftBlock.POWERED, (Boolean)state.getValue(GantryShaftBlock.POWERED)))
         );
         return offset;
      }
   }
}
