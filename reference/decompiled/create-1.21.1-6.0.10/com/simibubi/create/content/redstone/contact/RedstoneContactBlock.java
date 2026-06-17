package com.simibubi.create.content.redstone.contact;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.elevator.ElevatorColumn;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RedstoneContactBlock extends WrenchableDirectionalBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public RedstoneContactBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(FACING, Direction.UP));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED});
      super.createBlockStateDefinition(builder);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = (BlockState)this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
      Direction placeDirection = context.getClickedFace().getOpposite();
      if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown() || hasValidContact(context.getLevel(), context.getClickedPos(), placeDirection)) {
         state = (BlockState)state.setValue(FACING, placeDirection);
      }

      if (hasValidContact(context.getLevel(), context.getClickedPos(), (Direction)state.getValue(FACING))) {
         state = (BlockState)state.setValue(POWERED, true);
      }

      return state;
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult onWrenched = super.onWrenched(state, context);
      if (onWrenched != InteractionResult.SUCCESS) {
         return onWrenched;
      } else {
         Level level = context.getLevel();
         if (level.isClientSide()) {
            return onWrenched;
         } else {
            BlockPos pos = context.getClickedPos();
            state = level.getBlockState(pos);
            Direction facing = (Direction)state.getValue(FACING);
            if (facing.getAxis() == Axis.Y) {
               return onWrenched;
            } else if (ElevatorColumn.get(level, new ElevatorColumn.ColumnCoords(pos.getX(), pos.getZ(), facing)) == null) {
               return onWrenched;
            } else {
               level.setBlockAndUpdate(pos, BlockHelper.copyProperties(state, AllBlocks.ELEVATOR_CONTACT.getDefaultState()));
               return onWrenched;
            }
         }
      }
   }

   public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      if (facing != stateIn.getValue(FACING)) {
         return stateIn;
      } else {
         boolean hasValidContact = hasValidContact(worldIn, currentPos, facing);
         return stateIn.getValue(POWERED) != hasValidContact ? (BlockState)stateIn.setValue(POWERED, hasValidContact) : stateIn;
      }
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.getBlock() == this && newState.getBlock() == this && state == newState.cycle(POWERED)) {
         worldIn.updateNeighborsAt(pos, this);
      }

      super.onRemove(state, worldIn, pos, newState, isMoving);
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
      boolean hasValidContact = hasValidContact(worldIn, pos, (Direction)state.getValue(FACING));
      if ((Boolean)state.getValue(POWERED) != hasValidContact) {
         worldIn.setBlockAndUpdate(pos, (BlockState)state.setValue(POWERED, hasValidContact));
      }
   }

   public static boolean hasValidContact(LevelAccessor world, BlockPos pos, Direction direction) {
      BlockState blockState = world.getBlockState(pos.relative(direction));
      return (AllBlocks.REDSTONE_CONTACT.has(blockState) || AllBlocks.ELEVATOR_CONTACT.has(blockState))
         && blockState.getValue(FACING) == direction.getOpposite();
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERED);
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, @Nullable Direction side) {
      return side != null && state.getValue(FACING) != side.getOpposite();
   }

   public int getSignal(BlockState state, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return state.getValue(POWERED) && side != ((Direction)state.getValue(FACING)).getOpposite() ? 15 : 0;
   }
}
