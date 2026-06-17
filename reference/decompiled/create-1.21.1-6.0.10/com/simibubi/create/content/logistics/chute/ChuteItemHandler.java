package com.simibubi.create.content.logistics.chute;

import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ChuteItemHandler implements IItemHandler {
   private ChuteBlockEntity blockEntity;

   public ChuteItemHandler(ChuteBlockEntity be) {
      this.blockEntity = be;
   }

   public int getSlots() {
      return 1;
   }

   public ItemStack getStackInSlot(int slot) {
      return this.blockEntity.item;
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (!this.blockEntity.canAcceptItem(stack)) {
         return stack;
      } else {
         ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
         if (!simulate) {
            this.blockEntity.setItem(stack);
         }

         return remainder;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      ItemStack remainder = this.blockEntity.item.copy();
      ItemStack split = remainder.split(amount);
      if (!simulate) {
         this.blockEntity.setItem(remainder);
      }

      return split;
   }

   public int getSlotLimit(int slot) {
      return (Integer)this.getStackInSlot(slot).getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return true;
   }
}
