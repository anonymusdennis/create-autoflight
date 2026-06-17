package com.simibubi.create.content.redstone;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.diodes.BrassDiodeBlock;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RoseQuartzLampBlock extends Block implements IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty POWERING = BrassDiodeBlock.POWERING;
   public static final BooleanProperty ACTIVATE = BooleanProperty.create("activate");

   public RoseQuartzLampBlock(Properties p_49795_) {
      super(p_49795_);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(POWERING, false)).setValue(ACTIVATE, false)
      );
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      return (BlockState)stateForPlacement.setValue(POWERED, pContext.getLevel().hasNeighborSignal(pContext.getClickedPos()));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{POWERED, POWERING, ACTIVATE}));
   }

   public boolean shouldCheckWeakPower(BlockState state, SignalGetter level, BlockPos pos, Direction side) {
      return false;
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean isPowered = (Boolean)pState.getValue(POWERED);
         if (isPowered != pLevel.hasNeighborSignal(pPos)) {
            if (isPowered) {
               pLevel.setBlock(pPos, (BlockState)pState.cycle(POWERED), 2);
            } else {
               this.forEachInCluster(pLevel, pPos, (currentPos, currentState) -> {
                  pLevel.setBlock(currentPos, (BlockState)currentState.setValue(POWERING, false), 2);
                  this.scheduleActivation(pLevel, currentPos);
               });
               pLevel.setBlock(
                  pPos, (BlockState)((BlockState)((BlockState)pState.setValue(POWERED, true)).setValue(POWERING, true)).setValue(ACTIVATE, true), 2
               );
               pLevel.updateNeighborsAt(pPos, this);
               this.scheduleActivation(pLevel, pPos);
            }
         }
      }
   }

   private void scheduleActivation(Level pLevel, BlockPos pPos) {
      if (!pLevel.getBlockTicks().hasScheduledTick(pPos, this)) {
         pLevel.scheduleTick(pPos, this, 1);
      }
   }

   private void forEachInCluster(Level pLevel, BlockPos pPos, BiConsumer<BlockPos, BlockState> callback) {
      List<BlockPos> frontier = new LinkedList<>();
      Set<BlockPos> visited = new HashSet<>();
      frontier.add(pPos);
      visited.add(pPos);

      while (!frontier.isEmpty()) {
         BlockPos pos = frontier.remove(0);

         for (Direction d : Iterate.directions) {
            BlockPos currentPos = pos.relative(d);
            if (currentPos.distManhattan(pPos) <= 16 && visited.add(currentPos)) {
               BlockState currentState = pLevel.getBlockState(currentPos);
               if (currentState.is(this)) {
                  callback.accept(currentPos, currentState);
                  frontier.add(currentPos);
               }
            }
         }
      }
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public int getSignal(BlockState pState, BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
      if (pDirection == null) {
         return 0;
      } else {
         BlockState toState = pLevel.getBlockState(pPos.relative(pDirection.getOpposite()));
         if (toState.is(this)) {
            return 0;
         } else if (toState.is(Blocks.COMPARATOR)) {
            return this.getDistanceToPowered(pLevel, pPos, pDirection);
         } else {
            return pState.getValue(POWERING) ? 15 : 0;
         }
      }
   }

   private int getDistanceToPowered(BlockGetter level, BlockPos pos, Direction column) {
      MutableBlockPos currentPos = pos.mutable();

      for (int power = 15; power > 0; power--) {
         BlockState blockState = level.getBlockState(currentPos);
         if (!blockState.is(this)) {
            return 0;
         }

         if ((Boolean)blockState.getValue(POWERING)) {
            return power;
         }

         currentPos.move(column);
      }

      return 0;
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRand) {
      boolean wasPowering = (Boolean)pState.getValue(POWERING);
      boolean shouldBePowering = (Boolean)pState.getValue(ACTIVATE);
      if (wasPowering || shouldBePowering) {
         pLevel.setBlock(pPos, (BlockState)((BlockState)pState.setValue(ACTIVATE, false)).setValue(POWERING, shouldBePowering), 2);
      }

      pLevel.updateNeighborsAt(pPos, this);
   }

   @Override
   public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
      return (BlockState)originalState.cycle(POWERING);
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      InteractionResult onWrenched = IWrenchable.super.onWrenched(state, context);
      if (!onWrenched.consumesAction()) {
         return onWrenched;
      } else {
         this.forEachInCluster(
            context.getLevel(), context.getClickedPos(), (currentPos, currentState) -> context.getLevel().updateNeighborsAt(currentPos, this)
         );
         return onWrenched;
      }
   }
}
