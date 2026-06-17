package com.simibubi.create.foundation.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ItemHandlerWrapper implements IItemHandlerModifiable {
   private IItemHandlerModifiable wrapped;

   public ItemHandlerWrapper(IItemHandlerModifiable wrapped) {
      this.wrapped = wrapped;
   }

   public int getSlots() {
      return this.wrapped.getSlots();
   }

   public ItemStack getStackInSlot(int slot) {
      return this.wrapped.getStackInSlot(slot);
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      return this.wrapped.insertItem(slot, stack, simulate);
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return this.wrapped.extractItem(slot, amount, simulate);
   }

   public int getSlotLimit(int slot) {
      return this.wrapped.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return this.wrapped.isItemValid(slot, stack);
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      this.wrapped.setStackInSlot(slot, stack);
   }
}
