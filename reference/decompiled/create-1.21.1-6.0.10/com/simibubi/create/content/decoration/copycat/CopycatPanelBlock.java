package com.simibubi.create.content.decoration.copycat;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import java.util.List;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class CopycatPanelBlock extends WaterloggedCopycatBlock {
   public static final DirectionProperty FACING = BlockStateProperties.FACING;
   private static final int placementHelperId = PlacementHelpers.register(new CopycatPanelBlock.PlacementHelper());

   public CopycatPanelBlock(Properties pProperties) {
      super(pProperties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(FACING, Direction.UP));
   }

   @Override
   public boolean isAcceptedRegardless(BlockState material) {
      return CopycatSpecialCases.isBarsMaterial(material) || CopycatSpecialCases.isTrapdoorMaterial(material);
   }

   @Override
   public BlockState prepareMaterial(
      Level pLevel, BlockPos pPos, BlockState pState, Player pPlayer, InteractionHand pHand, BlockHitResult pHit, BlockState material
   ) {
      if (!CopycatSpecialCases.isTrapdoorMaterial(material)) {
         return super.prepareMaterial(pLevel, pPos, pState, pPlayer, pHand, pHit, material);
      } else {
         Direction panelFacing = (Direction)pState.getValue(FACING);
         if (panelFacing == Direction.DOWN) {
            material = (BlockState)material.setValue(TrapDoorBlock.HALF, Half.TOP);
         }

         if (panelFacing.getAxis() == Axis.Y) {
            return (BlockState)((BlockState)material.setValue(TrapDoorBlock.FACING, pPlayer.getDirection())).setValue(TrapDoorBlock.OPEN, false);
         } else {
            boolean clickedNearTop = pHit.getLocation().y - 0.5 > (double)pPos.getY();
            return (BlockState)((BlockState)((BlockState)material.setValue(TrapDoorBlock.OPEN, true))
                  .setValue(TrapDoorBlock.HALF, clickedNearTop ? Half.TOP : Half.BOTTOM))
               .setValue(TrapDoorBlock.FACING, panelFacing);
         }
      }
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
         if (placementHelper.matchesItem(stack)) {
            placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem)stack.getItem(), player, hand, hitResult);
            return ItemInteractionResult.SUCCESS;
         }
      }

      return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
   }

   @Override
   public boolean isIgnoredConnectivitySide(BlockAndTintGetter reader, BlockState state, Direction face, @Nullable BlockPos fromPos, @Nullable BlockPos toPos) {
      if (fromPos != null && toPos != null) {
         Direction facing = (Direction)state.getValue(FACING);
         BlockState toState = reader.getBlockState(toPos);
         if (!toState.is(this)) {
            return facing != face.getOpposite();
         } else {
            BlockPos diff = fromPos.subtract(toPos);
            int coord = facing.getAxis().choose(diff.getX(), diff.getY(), diff.getZ());
            return facing == ((Direction)toState.getValue(FACING)).getOpposite() && (coord == 0 || coord != facing.getAxisDirection().getStep());
         }
      } else {
         return true;
      }
   }

   @Override
   public boolean canConnectTexturesToward(BlockAndTintGetter reader, BlockPos fromPos, BlockPos toPos, BlockState state) {
      Direction facing = (Direction)state.getValue(FACING);
      BlockState toState = reader.getBlockState(toPos);
      if (toPos.equals(fromPos.relative(facing))) {
         return false;
      } else {
         BlockPos diff = fromPos.subtract(toPos);
         int coord = facing.getAxis().choose(diff.getX(), diff.getY(), diff.getZ());
         if (!toState.is(this)) {
            return coord != -facing.getAxisDirection().getStep();
         } else {
            return isOccluded(state, toState, facing) ? true : toState.setValue(WATERLOGGED, false) == state.setValue(WATERLOGGED, false) && coord == 0;
         }
      }
   }

   @Override
   public boolean canFaceBeOccluded(BlockState state, Direction face) {
      return ((Direction)state.getValue(FACING)).getOpposite() == face;
   }

   @Override
   public boolean shouldFaceAlwaysRender(BlockState state, Direction face) {
      return this.canFaceBeOccluded(state, face.getOpposite());
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      return (BlockState)stateForPlacement.setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACING}));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AllShapes.CASING_3PX.get((Direction)pState.getValue(FACING));
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public boolean supportsExternalFaceHiding(BlockState state) {
      return true;
   }

   public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
      if (state.is(this) == neighborState.is(this)) {
         if (CopycatSpecialCases.isBarsMaterial(getMaterial(level, pos)) && CopycatSpecialCases.isBarsMaterial(getMaterial(level, pos.relative(dir)))) {
            return state.getValue(FACING) == neighborState.getValue(FACING);
         }

         if (getMaterial(level, pos).skipRendering(getMaterial(level, pos.relative(dir)), dir.getOpposite())) {
            return isOccluded(state, neighborState, dir.getOpposite());
         }
      }

      return state.getValue(FACING) == dir.getOpposite() && getMaterial(level, pos).skipRendering(neighborState, dir.getOpposite());
   }

   public static boolean isOccluded(BlockState state, BlockState other, Direction pDirection) {
      state = (BlockState)state.setValue(WATERLOGGED, false);
      other = (BlockState)other.setValue(WATERLOGGED, false);
      Direction facing = (Direction)state.getValue(FACING);
      if (facing.getOpposite() == other.getValue(FACING) && pDirection == facing) {
         return true;
      } else {
         return other.getValue(FACING) != facing ? false : pDirection.getAxis() != facing.getAxis();
      }
   }

   public BlockState rotate(BlockState state, Rotation rot) {
      return (BlockState)state.setValue(FACING, rot.rotate((Direction)state.getValue(FACING)));
   }

   public BlockState mirror(BlockState state, Mirror mirrorIn) {
      return state.rotate(mirrorIn.getRotation((Direction)state.getValue(FACING)));
   }

   @MethodsReturnNonnullByDefault
   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return AllBlocks.COPYCAT_PANEL::isIn;
      }

      public Predicate<BlockState> getStatePredicate() {
         return AllBlocks.COPYCAT_PANEL::has;
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray) {
         List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(
            pos,
            ray.getLocation(),
            ((Direction)state.getValue(CopycatPanelBlock.FACING)).getAxis(),
            dir -> world.getBlockState(pos.relative(dir)).canBeReplaced()
         );
         return directions.isEmpty()
            ? PlacementOffset.fail()
            : PlacementOffset.success(
               pos.relative(directions.get(0)), s -> (BlockState)s.setValue(CopycatPanelBlock.FACING, (Direction)state.getValue(CopycatPanelBlock.FACING))
            );
      }
   }
}
