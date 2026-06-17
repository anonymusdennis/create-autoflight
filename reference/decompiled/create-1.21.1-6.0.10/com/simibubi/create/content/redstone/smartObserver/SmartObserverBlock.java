package com.simibubi.create.content.redstone.smartObserver;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class SmartObserverBlock extends DirectedDirectionalBlock implements IBE<SmartObserverBlockEntity> {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public SmartObserverBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(POWERED, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{POWERED}));
   }

   @Override
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      BlockState state = this.defaultBlockState();
      Direction preferredFacing = null;

      for (Direction face : context.getNearestLookingDirections()) {
         BlockPos offsetPos = context.getClickedPos().relative(face);
         Level world = context.getLevel();
         boolean canDetect = false;
         BlockEntity blockEntity = world.getBlockEntity(offsetPos);
         if (BlockEntityBehaviour.get(blockEntity, TransportedItemStackHandlerBehaviour.TYPE) != null) {
            canDetect = true;
         } else if (BlockEntityBehaviour.get(blockEntity, FluidTransportBehaviour.TYPE) != null) {
            canDetect = true;
         } else if (blockEntity == null
            || context.getLevel().getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), null) == null
               && context.getLevel().getCapability(FluidHandler.BLOCK, blockEntity.getBlockPos(), null) == null) {
            if (blockEntity instanceof FunnelBlockEntity) {
               canDetect = true;
            }
         } else {
            canDetect = true;
         }

         if (canDetect) {
            preferredFacing = face;
            break;
         }
      }

      if (preferredFacing == null) {
         Direction facing = context.getNearestLookingDirection();
         preferredFacing = context.getPlayer() != null && context.getPlayer().isShiftKeyDown() ? facing : facing.getOpposite();
      }

      if (preferredFacing.getAxis() == Axis.Y) {
         state = (BlockState)state.setValue(TARGET, preferredFacing == Direction.UP ? AttachFace.CEILING : AttachFace.FLOOR);
         preferredFacing = context.getHorizontalDirection();
      }

      return (BlockState)state.setValue(FACING, preferredFacing);
   }

   public boolean isSignalSource(BlockState state) {
      return (Boolean)state.getValue(POWERED);
   }

   public int getSignal(BlockState blockState, BlockGetter blockAccess, BlockPos pos, Direction side) {
      return !this.isSignalSource(blockState) || side != null && side == getTargetDirection(blockState).getOpposite() ? 0 : 15;
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
      worldIn.setBlock(pos, (BlockState)state.setValue(POWERED, false), 2);
      worldIn.updateNeighborsAt(pos, this);
   }

   public boolean canConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side != ((Direction)state.getValue(FACING)).getOpposite();
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      IBE.onRemove(state, worldIn, pos, newState);
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      InvManipulationBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, InvManipulationBehaviour.TYPE);
      if (behaviour != null) {
         behaviour.onNeighborChanged(fromPos);
      }
   }

   public void onFunnelTransfer(Level world, BlockPos funnelPos, ItemStack transferred) {
      for (Direction direction : Iterate.directions) {
         BlockPos detectorPos = funnelPos.relative(direction);
         BlockState detectorState = world.getBlockState(detectorPos);
         if (AllBlocks.SMART_OBSERVER.has(detectorState) && getTargetDirection(detectorState) == direction.getOpposite()) {
            this.withBlockEntityDo(world, detectorPos, be -> {
               FilteringBehaviour filteringBehaviour = BlockEntityBehaviour.get(be, FilteringBehaviour.TYPE);
               if (filteringBehaviour != null) {
                  if (filteringBehaviour.test(transferred)) {
                     be.activate(4);
                  }
               }
            });
         }
      }
   }

   @Override
   public Class<SmartObserverBlockEntity> getBlockEntityClass() {
      return SmartObserverBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SmartObserverBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SmartObserverBlockEntity>)AllBlockEntityTypes.SMART_OBSERVER.get();
   }
}
