package com.simibubi.create.content.contraptions.gantry;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.gantry.GantryShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class GantryCarriageBlock extends DirectionalAxisKineticBlock implements IBE<GantryCarriageBlockEntity> {
   public GantryCarriageBlock(Properties properties) {
      super(properties);
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      Direction direction = (Direction)state.getValue(FACING);
      BlockState shaft = world.getBlockState(pos.relative(direction.getOpposite()));
      return AllBlocks.GANTRY_SHAFT.has(shaft) && ((Direction)shaft.getValue(GantryShaftBlock.FACING)).getAxis() != direction.getAxis();
   }

   @Override
   public void updateIndirectNeighbourShapes(BlockState stateIn, LevelAccessor worldIn, BlockPos pos, int flags, int count) {
      super.updateIndirectNeighbourShapes(stateIn, worldIn, pos, flags, count);
      this.withBlockEntityDo(worldIn, pos, GantryCarriageBlockEntity::checkValidGantryShaft);
   }

   @Override
   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, worldIn, pos, oldState, isMoving);
   }

   @Override
   protected Direction getFacingForPlacement(BlockPlaceContext context) {
      return context.getClickedFace();
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild() || player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (stack.isEmpty()) {
         this.withBlockEntityDo(level, pos, be -> be.checkValidGantryShaft());
         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState stateForPlacement = super.getStateForPlacement(context);
      Direction opposite = ((Direction)stateForPlacement.getValue(FACING)).getOpposite();
      return this.cycleAxisIfNecessary(stateForPlacement, opposite, context.getLevel().getBlockState(context.getClickedPos().relative(opposite)));
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block p_220069_4_, BlockPos updatePos, boolean p_220069_6_) {
      if (updatePos.equals(pos.relative(((Direction)state.getValue(FACING)).getOpposite())) && !this.canSurvive(state, world, pos)) {
         world.destroyBlock(pos, true);
      }
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState otherState, LevelAccessor world, BlockPos pos, BlockPos p_196271_6_) {
      return state.getValue(FACING) != direction.getOpposite() ? state : this.cycleAxisIfNecessary(state, direction, otherState);
   }

   protected BlockState cycleAxisIfNecessary(BlockState state, Direction direction, BlockState otherState) {
      if (!AllBlocks.GANTRY_SHAFT.has(otherState)) {
         return state;
      } else if (((Direction)otherState.getValue(GantryShaftBlock.FACING)).getAxis() == direction.getAxis()) {
         return state;
      } else {
         return isValidGantryShaftAxis(state, otherState) ? state : (BlockState)state.cycle(AXIS_ALONG_FIRST_COORDINATE);
      }
   }

   public static boolean isValidGantryShaftAxis(BlockState pinionState, BlockState gantryState) {
      return getValidGantryShaftAxis(pinionState) == ((Direction)gantryState.getValue(GantryShaftBlock.FACING)).getAxis();
   }

   public static Axis getValidGantryShaftAxis(BlockState state) {
      if (state.getBlock() instanceof GantryCarriageBlock block) {
         Axis var8 = block.getRotationAxis(state);
         Axis facingAxis = ((Direction)state.getValue(FACING)).getAxis();

         for (Axis axis : Iterate.axes) {
            if (axis != var8 && axis != facingAxis) {
               return axis;
            }
         }

         return Axis.Y;
      } else {
         return Axis.Y;
      }
   }

   public static Axis getValidGantryPinionAxis(BlockState state, Axis shaftAxis) {
      Axis facingAxis = ((Direction)state.getValue(FACING)).getAxis();

      for (Axis axis : Iterate.axes) {
         if (axis != shaftAxis && axis != facingAxis) {
            return axis;
         }
      }

      return Axis.Y;
   }

   @Override
   public Class<GantryCarriageBlockEntity> getBlockEntityClass() {
      return GantryCarriageBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends GantryCarriageBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends GantryCarriageBlockEntity>)AllBlockEntityTypes.GANTRY_PINION.get();
   }
}
