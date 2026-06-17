package com.simibubi.create.content.redstone.diodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PulseTimerBlockEntity extends BrassDiodeBlockEntity {
   public PulseTimerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected int defaultValue() {
      return 20;
   }

   @Override
   protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin) {
      if (!powered && this.state < this.maxState.getValue() - 1) {
         this.state++;
      } else {
         this.state = 0;
      }

      if (!this.level.isClientSide) {
         boolean shouldPower = !powered && (this.maxState.getValue() == 2 ? this.state == 0 : this.state <= 1);
         BlockState blockState = this.getBlockState();
         if ((Boolean)blockState.getValue(BrassDiodeBlock.POWERING) != shouldPower) {
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.setValue(BrassDiodeBlock.POWERING, shouldPower));
         }
      }
   }
}
