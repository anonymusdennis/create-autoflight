package com.simibubi.create.content.redstone.diodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PulseRepeaterBlockEntity extends BrassDiodeBlockEntity {
   public PulseRepeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   protected void updateState(boolean powered, boolean powering, boolean atMax, boolean atMin) {
      if (!atMin || powered) {
         if (this.state > this.maxState.getValue() + 1) {
            if (!powered && !powering) {
               this.state = 0;
            }
         } else {
            this.state++;
            if (!this.level.isClientSide) {
               if (this.state == this.maxState.getValue() - 1 && !powering) {
                  this.level.setBlockAndUpdate(this.worldPosition, (BlockState)this.getBlockState().cycle(BrassDiodeBlock.POWERING));
               }

               if (this.state == this.maxState.getValue() + 1 && powering) {
                  this.level.setBlockAndUpdate(this.worldPosition, (BlockState)this.getBlockState().cycle(BrassDiodeBlock.POWERING));
               }
            }
         }
      }
   }
}
