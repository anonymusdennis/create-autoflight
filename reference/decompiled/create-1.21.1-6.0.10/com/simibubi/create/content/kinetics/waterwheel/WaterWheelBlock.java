package com.simibubi.create.content.kinetics.waterwheel;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class WaterWheelBlock extends DirectionalKineticBlock implements IBE<WaterWheelBlockEntity> {
   public WaterWheelBlock(Properties properties) {
      super(properties);
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      for (Direction direction : Iterate.directions) {
         BlockPos neighbourPos = pos.relative(direction);
         BlockState neighbourState = worldIn.getBlockState(neighbourPos);
         if (AllBlocks.WATER_WHEEL.has(neighbourState)) {
            Axis axis = ((Direction)state.getValue(FACING)).getAxis();
            if (((Direction)neighbourState.getValue(FACING)).getAxis() != axis || axis != direction.getAxis()) {
               return false;
            }
         }
      }

      return true;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      return this.onBlockEntityUseItemOn(level, pos, wwt -> wwt.applyMaterialIfValid(stack));
   }

   public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      if (worldIn instanceof WrappedLevel) {
         return stateIn;
      } else if (worldIn.isClientSide()) {
         return stateIn;
      } else {
         if (!worldIn.getBlockTicks().hasScheduledTick(currentPos, this)) {
            worldIn.scheduleTick(currentPos, this, 1);
         }

         return stateIn;
      }
   }

   @Override
   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, worldIn, pos, oldState, isMoving);
      if (!worldIn.isClientSide()) {
         if (!worldIn.getBlockTicks().hasScheduledTick(pos, this)) {
            worldIn.scheduleTick(pos, this, 1);
         }
      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      this.withBlockEntityDo(pLevel, pPos, WaterWheelBlockEntity::determineAndApplyFlowScore);
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = super.getStateForPlacement(context);
      state.setValue(FACING, Direction.get(AxisDirection.POSITIVE, ((Direction)state.getValue(FACING)).getAxis()));
      return state;
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return ((Direction)state.getValue(FACING)).getAxis() == face.getAxis();
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return ((Direction)state.getValue(FACING)).getAxis();
   }

   @Override
   public float getParticleTargetRadius() {
      return 1.125F;
   }

   @Override
   public float getParticleInitialRadius() {
      return 1.0F;
   }

   @Override
   public boolean hideStressImpact() {
      return true;
   }

   @Override
   public Class<WaterWheelBlockEntity> getBlockEntityClass() {
      return WaterWheelBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends WaterWheelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends WaterWheelBlockEntity>)AllBlockEntityTypes.WATER_WHEEL.get();
   }

   public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
      return false;
   }
}
