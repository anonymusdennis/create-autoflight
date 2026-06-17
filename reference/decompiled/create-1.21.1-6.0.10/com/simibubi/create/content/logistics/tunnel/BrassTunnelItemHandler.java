package com.simibubi.create.content.logistics.tunnel;

import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class BrassTunnelItemHandler implements IItemHandler {
   private BrassTunnelBlockEntity blockEntity;

   public BrassTunnelItemHandler(BrassTunnelBlockEntity be) {
      this.blockEntity = be;
   }

   public int getSlots() {
      return 1;
   }

   public ItemStack getStackInSlot(int slot) {
      return this.blockEntity.stackToDistribute;
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (!this.blockEntity.hasDistributionBehaviour()) {
         IItemHandler beltCapability = this.blockEntity.getBeltCapability();
         return beltCapability == null ? stack : beltCapability.insertItem(slot, stack, simulate);
      } else if (!this.blockEntity.canTakeItems()) {
         return stack;
      } else {
         ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
         if (!simulate) {
            this.blockEntity.setStackToDistribute(stack, null);
         }

         return remainder;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      IItemHandler beltCapability = this.blockEntity.getBeltCapability();
      return beltCapability == null ? ItemStack.EMPTY : beltCapability.extractItem(slot, amount, simulate);
   }

   public int getSlotLimit(int slot) {
      return this.blockEntity.stackToDistribute.isEmpty() ? 64 : this.blockEntity.stackToDistribute.getMaxStackSize();
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return true;
   }
}
