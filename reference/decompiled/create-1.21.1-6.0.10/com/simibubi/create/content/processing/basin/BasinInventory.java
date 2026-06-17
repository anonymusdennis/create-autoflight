package com.simibubi.create.content.processing.basin;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.world.item.ItemStack;

public class BasinInventory extends SmartInventory {
   private BasinBlockEntity blockEntity;
   public boolean packagerMode;

   public BasinInventory(int slots, BasinBlockEntity be) {
      super(slots, be, 64, true);
      this.blockEntity = be;
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (this.packagerMode) {
         return this.inv.insertItem(slot, stack, simulate);
      } else {
         int firstFreeSlot = -1;

         for (int i = 0; i < this.getSlots(); i++) {
            if (i != slot && ItemStack.isSameItemSameComponents(stack, this.inv.getStackInSlot(i))) {
               return stack;
            }

            if (this.inv.getStackInSlot(i).isEmpty() && firstFreeSlot == -1) {
               firstFreeSlot = i;
            }
         }

         return this.inv.getStackInSlot(slot).isEmpty() && firstFreeSlot != slot ? stack : super.insertItem(slot, stack, simulate);
      }
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      ItemStack extractItem = super.extractItem(slot, amount, simulate);
      if (!simulate && !extractItem.isEmpty()) {
         this.blockEntity.notifyChangeOfContents();
      }

      return extractItem;
   }
}
