package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.placement.PoleHelper;
import java.util.function.Predicate;
import net.createmod.catnip.math.VoxelShaper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CopycatStepBlock extends WaterloggedCopycatBlock {
   public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
   public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
   private static final int placementHelperId = PlacementHelpers.register(new CopycatStepBlock.PlacementHelper());

   public CopycatStepBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(HALF, Half.BOTTOM)).setValue(FACING, Direction.SOUTH));
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
         if (helper.matchesItem(stack)) {
            return helper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
         }
      }

      return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   @Override
   public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face, @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
      if (fromPos != null && toPos != null) {
         BlockState toState = reader.getBlockState(toPos);
         if (!toState.is(this)) {
            return true;
         } else {
            Direction facing = (Direction)state.getValue(FACING);
            BlockPos diff = fromPos.subtract(toPos);
            int coord = facing.getAxis().choose(diff.getX(), diff.getY(), diff.getZ());
            Half half = (Half)state.getValue(HALF);
            return half != toState.getValue(HALF)
               ? diff.getY() == 0
               : facing == ((Direction)toState.getValue(FACING)).getOpposite() && (coord == 0 || coord == facing.getAxisDirection().getStep());
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
      Direction facing = (Direction)state.getValue(FACING);
      BlockState toState = reader.getBlockState(toPos);
      BlockPos diff = fromPos.subtract(toPos);
      if (fromPos.equals(toPos.relative(facing))) {
         return false;
      } else if (!toState.is(this)) {
         return false;
      } else if (diff.getY() != 0) {
         return isOccluded(toState, state, diff.getY() > 0 ? Direction.UP : Direction.DOWN);
      } else if (isOccluded(state, toState, facing)) {
         return true;
      } else {
         int coord = facing.getAxis().choose(diff.getX(), diff.getY(), diff.getZ());
         return state.setValue(WATERLOGGED, false) == toState.setValue(WATERLOGGED, false) && coord == 0;
      }
   }

   @Override
   public boolean canFaceBeOccluded(BlockState state, Direction face) {
      return face.getAxis() == Axis.Y ? state.getValue(HALF) == Half.TOP == (face == Direction.UP) : state.getValue(FACING) == face;
   }

   @Override
   public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
      return this.canFaceBeOccluded(state, face.getOpposite());
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = (BlockState)super.getStateForPlacement(pContext).setValue(FACING, pContext.getHorizontalDirection());
      Direction direction = pContext.getClickedFace();
      if (direction == Direction.UP) {
         return stateForPlacement;
      } else {
         return direction != Direction.DOWN && !(pContext.getClickLocation().y - (double)pContext.getClickedPos().getY() > 0.5)
            ? stateForPlacement
            : (BlockState)stateForPlacement.setValue(HALF, Half.TOP);
      }
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{HALF, FACING}));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      VoxelShaper voxelShaper = pState.getValue(HALF) == Half.BOTTOM ? AllShapes.STEP_BOTTOM : AllShapes.STEP_TOP;
      return voxelShaper.get((Direction)pState.getValue(FACING));
   }

   public boolean supportsExternalFaceHiding(BlockState state) {
      return true;
   }

   public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
      return state.is(this) == neighborState.is(this) && getMaterial(level, pos).skipRendering(getMaterial(level, pos.relative(dir)), dir.getOpposite())
         ? isOccluded(state, neighborState, dir)
         : false;
   }

   public static boolean isOccluded(BlockState state, BlockState other, Direction pDirection) {
      state = (BlockState)state.setValue(WATERLOGGED, false);
      other = (BlockState)other.setValue(WATERLOGGED, false);
      Half half = (Half)state.getValue(HALF);
      boolean vertical = pDirection.getAxis() == Axis.Y;
      if (half == other.getValue(HALF)) {
         if (vertical) {
            return false;
         } else {
            Direction facing = (Direction)state.getValue(FACING);
            if (facing.getOpposite() == other.getValue(FACING) && pDirection == facing) {
               return true;
            } else {
               return other.getValue(FACING) != facing ? false : pDirection.getAxis() != facing.getAxis();
            }
         }
      } else {
         return vertical && pDirection == Direction.UP == (half == Half.TOP);
      }
   }

   public BlockState rotate(BlockState pState, Rotation pRot) {
      return (BlockState)pState.setValue(FACING, pRot.rotate((Direction)pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation((Direction)pState.getValue(FACING)));
   }

   private static class PlacementHelper extends PoleHelper<Direction> {
      public PlacementHelper() {
         super(AllBlocks.COPYCAT_STEP::has, state -> ((Direction)state.getValue(CopycatStepBlock.FACING)).getClockWise().getAxis(), CopycatStepBlock.FACING);
      }

      @NotNull
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.COPYCAT_STEP::isIn;
      }

      @NotNull
      @Override
      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         PlacementOffset offset = super.getOffset(player, world, state, pos, ray);
         if (offset.isSuccessful()) {
            offset.withTransform(offset.getTransform().andThen(s -> (BlockState)s.setValue(CopycatStepBlock.HALF, (Half)state.getValue(CopycatStepBlock.HALF))));
         }

         return offset;
      }
   }
}
