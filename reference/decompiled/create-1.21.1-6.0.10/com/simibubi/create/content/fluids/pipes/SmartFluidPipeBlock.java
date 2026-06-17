package com.simibubi.create.content.fluids.pipes;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VoxelShaper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.NotNull;

public class SmartFluidPipeBlock
   extends FaceAttachedHorizontalDirectionalBlock
   implements IBE<SmartFluidPipeBlockEntity>,
   IAxisPipe,
   IWrenchable,
   ProperWaterloggedBlock {
   public static final MapCodec<SmartFluidPipeBlock> CODEC = simpleCodec(SmartFluidPipeBlock::new);

   public SmartFluidPipeBlock(Properties p_i48339_1_) {
      super(p_i48339_1_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{FACE, FACING, WATERLOGGED});
   }

   public BlockState getStateForPlacement(BlockPlaceContext ctx) {
      BlockState stateForPlacement = super.getStateForPlacement(ctx);
      Axis prefferedAxis = null;
      BlockPos pos = ctx.getClickedPos();
      Level world = ctx.getLevel();

      for (Direction side : Iterate.directions) {
         if (this.prefersConnectionTo(world, pos, side)) {
            if (prefferedAxis != null && prefferedAxis != side.getAxis()) {
               prefferedAxis = null;
               break;
            }

            prefferedAxis = side.getAxis();
         }
      }

      if (prefferedAxis == Axis.Y) {
         stateForPlacement = (BlockState)((BlockState)stateForPlacement.setValue(FACE, AttachFace.WALL))
            .setValue(FACING, ((Direction)stateForPlacement.getValue(FACING)).getOpposite());
      } else if (prefferedAxis != null) {
         if (stateForPlacement.getValue(FACE) == AttachFace.WALL) {
            stateForPlacement = (BlockState)stateForPlacement.setValue(FACE, AttachFace.FLOOR);
         }

         for (Direction direction : ctx.getNearestLookingDirections()) {
            if (direction.getAxis() == prefferedAxis) {
               stateForPlacement = (BlockState)stateForPlacement.setValue(FACING, direction.getOpposite());
            }
         }
      }

      return this.withWater(stateForPlacement, ctx);
   }

   protected boolean prefersConnectionTo(LevelReader reader, BlockPos pos, Direction facing) {
      BlockPos offset = pos.relative(facing);
      BlockState blockState = reader.getBlockState(offset);
      return FluidPipeBlock.canConnectTo(reader, offset, blockState, facing);
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      boolean blockTypeChanged = state.getBlock() != newState.getBlock();
      if (blockTypeChanged && !world.isClientSide) {
         FluidPropagator.propagateChangedPipe(world, pos, state);
      }

      IBE.onRemove(state, world, pos, newState);
   }

   public boolean canSurvive(BlockState p_196260_1_, LevelReader p_196260_2_, BlockPos p_196260_3_) {
      return true;
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean isMoving) {
      if (!world.isClientSide) {
         if (state != oldState) {
            world.scheduleTick(pos, this, 1, TickPriority.HIGH);
         }
      }
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

   public static boolean isOpenAt(BlockState state, Direction d) {
      return d.getAxis() == getPipeAxis(state);
   }

   public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource r) {
      FluidPropagator.propagateChangedPipe(world, pos, state);
   }

   protected static Axis getPipeAxis(BlockState state) {
      return state.getValue(FACE) == AttachFace.WALL ? Axis.Y : ((Direction)state.getValue(FACING)).getAxis();
   }

   public VoxelShape getShape(BlockState state, BlockGetter p_220053_2_, BlockPos p_220053_3_, CollisionContext p_220053_4_) {
      AttachFace face = (AttachFace)state.getValue(FACE);
      VoxelShaper shape = face == AttachFace.FLOOR
         ? AllShapes.SMART_FLUID_PIPE_FLOOR
         : (face == AttachFace.CEILING ? AllShapes.SMART_FLUID_PIPE_CEILING : AllShapes.SMART_FLUID_PIPE_WALL);
      return shape.get((Direction)state.getValue(FACING));
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
   }

   @Override
   public Axis getAxis(BlockState state) {
      return getPipeAxis(state);
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   @Override
   public Class<SmartFluidPipeBlockEntity> getBlockEntityClass() {
      return SmartFluidPipeBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SmartFluidPipeBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SmartFluidPipeBlockEntity>)AllBlockEntityTypes.SMART_FLUID_PIPE.get();
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }
}
