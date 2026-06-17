package com.simibubi.create.content.fluids.pump;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public class PumpBlock extends DirectionalKineticBlock implements SimpleWaterloggedBlock, ICogWheel, IBE<PumpBlockEntity> {
   public PumpBlock(Properties p_i48415_1_) {
      super(p_i48415_1_);
      this.registerDefaultState((BlockState)super.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      return (BlockState)originalState.setValue(FACING, ((Direction)originalState.getValue(FACING)).getOpposite());
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
      return AllShapes.PUMP.get((Direction)state.getValue(FACING));
   }

   public void neighborChanged(BlockState state, Level world, BlockPos pos, Block otherBlock, BlockPos neighborPos, boolean isMoving) {
      DebugPackets.sendNeighborsUpdatePacket(world, pos);
      Direction d = FluidPropagator.validateNeighbourChange(state, world, pos, otherBlock, neighborPos, isMoving);
      if (d != null) {
         if (isOpenAt(state, d)) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
         }
      }
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
      BlockState toPlace = super.getStateForPlacement(context);
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      boolean isShiftKeyDown = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
      toPlace = ProperWaterloggedBlock.withWater(level, toPlace, pos);
      Direction nearestLookingDirection = context.getNearestLookingDirection();
      Direction targetDirection = isShiftKeyDown ? nearestLookingDirection : nearestLookingDirection.getOpposite();
      Direction bestConnectedDirection = null;
      double bestDistance = Double.MAX_VALUE;

      for (Direction d : Iterate.directions) {
         BlockPos adjPos = pos.relative(d);
         BlockState adjState = level.getBlockState(adjPos);
         if (FluidPipeBlock.canConnectTo(level, adjPos, adjState, d)) {
            double distance = Vec3.atLowerCornerOf(d.getNormal()).distanceTo(Vec3.atLowerCornerOf(targetDirection.getNormal()));
            if (!(distance > bestDistance)) {
               bestDistance = distance;
               bestConnectedDirection = d;
            }
         }
      }

      return bestConnectedDirection != null && bestConnectedDirection.getAxis() != targetDirection.getAxis() && !isShiftKeyDown
         ? (BlockState)toPlace.setValue(FACING, bestConnectedDirection)
         : toPlace;
   }

   public static boolean isPump(BlockState state) {
      return state.getBlock() instanceof PumpBlock;
   }

   @Override
   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, world, pos, oldState, isMoving);
      if (!world.isClientSide) {
         if (state != oldState) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
         }

         if (isPump(state) && isPump(oldState) && state.getValue(FACING) == ((Direction)oldState.getValue(FACING)).getOpposite()) {
            if (!(world.getBlockEntity(pos) instanceof PumpBlockEntity pump)) {
               return;
            }

            pump.pressureUpdate = true;
         }
      }
   }

   public static boolean isOpenAt(BlockState state, Direction d) {
      return d.getAxis() == ((Direction)state.getValue(FACING)).getAxis();
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
      FluidPropagator.propagateChangedPipe(world, pos, state);
   }

   @Override
   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      boolean blockTypeChanged = !state.is(newState.getBlock());
      if (blockTypeChanged && !world.isClientSide) {
         FluidPropagator.propagateChangedPipe(world, pos, state);
      }

      super.onRemove(state, world, pos, newState, isMoving);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public Class<PumpBlockEntity> getBlockEntityClass() {
      return PumpBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends PumpBlockEntity>)AllBlockEntityTypes.MECHANICAL_PUMP.get();
   }
}
