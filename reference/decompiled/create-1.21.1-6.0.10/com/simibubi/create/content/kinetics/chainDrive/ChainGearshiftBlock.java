package com.simibubi.create.content.kinetics.chainDrive;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class ChainGearshiftBlock extends ChainDriveBlock {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public ChainGearshiftBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED}));
   }

   @Override
   public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
      super.onPlace(state, worldIn, pos, oldState, isMoving);
      if (oldState.getBlock() != state.getBlock()) {
         this.withBlockEntityDo(worldIn, pos, kbe -> ((ChainGearshiftBlockEntity)kbe).neighbourChanged());
      }
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      return super.areStatesKineticallyEquivalent(oldState, newState) && oldState.getValue(POWERED) == newState.getValue(POWERED);
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         this.withBlockEntityDo(worldIn, pos, kbe -> ((ChainGearshiftBlockEntity)kbe).neighbourChanged());
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
            worldIn.setBlock(pos, (BlockState)state.cycle(POWERED), 18);
         }
      }
   }

   @Override
   public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends KineticBlockEntity>)AllBlockEntityTypes.ADJUSTABLE_CHAIN_GEARSHIFT.get();
   }
}
