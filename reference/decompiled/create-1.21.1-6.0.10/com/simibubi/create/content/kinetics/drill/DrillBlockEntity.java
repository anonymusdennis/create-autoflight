package com.simibubi.create.content.kinetics.drill;

import com.simibubi.create.content.kinetics.base.BlockBreakingKineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class DrillBlockEntity extends BlockBreakingKineticBlockEntity {
   private CobbleGenOptimisation.CobbleGenBlockConfiguration currentConfig;
   private BlockState currentOutput = Blocks.AIR.defaultBlockState();

   public DrillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected BlockPos getBreakingPos() {
      return this.getBlockPos().relative((Direction)this.getBlockState().getValue(DrillBlock.FACING));
   }

   @Override
   public void onBlockBroken(BlockState stateToBreak) {
      if (!this.optimiseCobbleGen(stateToBreak)) {
         super.onBlockBroken(stateToBreak);
      }
   }

   public boolean optimiseCobbleGen(BlockState stateToBreak) {
      DirectBeltInputBehaviour inv = BlockEntityBehaviour.get(this.level, this.breakingPos.below(), DirectBeltInputBehaviour.TYPE);
      BlockEntity blockEntityBelow = this.level.getBlockEntity(this.breakingPos.below());
      BlockEntity blockEntityAbove = this.level.getBlockEntity(this.breakingPos.above());
      if (inv != null || blockEntityBelow instanceof HopperBlockEntity || blockEntityAbove instanceof ChuteBlockEntity chute && chute.getItemMotion() > 0.0F) {
         CobbleGenOptimisation.CobbleGenBlockConfiguration config = CobbleGenOptimisation.getConfig(
            this.level, this.worldPosition, (Direction)this.getBlockState().getValue(DrillBlock.FACING)
         );
         if (config == null) {
            return false;
         } else {
            if (this.level instanceof ServerLevel sl) {
               BlockPos var14 = this.getBreakingPos();
               if (!config.equals(this.currentConfig)) {
                  this.currentConfig = config;
                  this.currentOutput = CobbleGenOptimisation.determineOutput(sl, var14, config);
               }

               if (!this.currentOutput.isAir() && this.currentOutput.equals(stateToBreak)) {
                  if (inv != null) {
                     for (ItemStack stack : Block.getDrops(stateToBreak, sl, var14, null)) {
                        inv.handleInsertion(stack, Direction.UP, false);
                     }
                  } else if (blockEntityBelow instanceof HopperBlockEntity hbe) {
                     IItemHandler handler = (IItemHandler)this.level.getCapability(ItemHandler.BLOCK, hbe.getBlockPos(), null);
                     if (handler != null) {
                        for (ItemStack stack : Block.getDrops(stateToBreak, sl, var14, null)) {
                           ItemHandlerHelper.insertItemStacked(handler, stack, false);
                        }
                     }
                  } else if (blockEntityAbove instanceof ChuteBlockEntity chutex && chutex.getItemMotion() > 0.0F) {
                     for (ItemStack stack : Block.getDrops(stateToBreak, sl, var14, null)) {
                        if (chutex.getItem().isEmpty()) {
                           chutex.setItem(stack, 0.0F);
                        }
                     }
                  }

                  this.level.levelEvent(2001, var14, Block.getId(stateToBreak));
                  return true;
               }

               return false;
            }

            return false;
         }
      } else {
         return false;
      }
   }
}
