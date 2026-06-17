package com.simibubi.create.content.trains.observer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class TrackObserverBlock extends Block implements IBE<TrackObserverBlockEntity>, IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public TrackObserverBlock(Properties p_49795_) {
      super(p_49795_);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{POWERED}));
   }

   public boolean isSignalSource(BlockState state) {
      return true;
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return blockState.getValue(POWERED) ? 15 : 0;
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return true;
   }

   @Override
   public Class<TrackObserverBlockEntity> getBlockEntityClass() {
      return TrackObserverBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends TrackObserverBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends TrackObserverBlockEntity>)AllBlockEntityTypes.TRACK_OBSERVER.get();
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      IBE.onRemove(state, worldIn, pos, newState);
   }
}
