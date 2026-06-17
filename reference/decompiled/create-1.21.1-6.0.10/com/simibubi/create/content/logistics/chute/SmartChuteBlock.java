package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.AllBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SmartChuteBlock extends AbstractChuteBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public SmartChuteBlock(Properties p_i48440_1_) {
      super(p_i48440_1_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, true));
   }

   @Override
   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      super.neighborChanged(state, level, pos, block, fromPos, isMoving);
      if (!level.isClientSide) {
         if (!level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
         }
      }
   }

   @Override
   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
      boolean previouslyPowered = (Boolean)state.getValue(POWERED);
      if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
         worldIn.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
      }
   }

   public BlockState getStateForPlacement(BlockPlaceContext p_196258_1_) {
      return (BlockState)super.getStateForPlacement(p_196258_1_).setValue(POWERED, p_196258_1_.getLevel().hasNeighborSignal(p_196258_1_.getClickedPos()));
   }

   public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
      return true;
   }

   @Override
   public BlockEntityType<? extends ChuteBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends ChuteBlockEntity>)AllBlockEntityTypes.SMART_CHUTE.get();
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
      super.createBlockStateDefinition(p_206840_1_.add(new Property[]{POWERED}));
   }

   @Override
   public BlockState updateChuteState(BlockState state, BlockState above, BlockGetter world, BlockPos pos) {
      return state;
   }
}
