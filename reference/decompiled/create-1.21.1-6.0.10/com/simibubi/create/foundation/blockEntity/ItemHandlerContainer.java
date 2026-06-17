package com.simibubi.create.foundation.blockEntity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class ItemHandlerContainer implements Container {
   protected final IItemHandlerModifiable inv;

   public ItemHandlerContainer(IItemHandlerModifiable inv) {
      this.inv = inv;
   }

   public int getContainerSize() {
      return this.inv.getSlots();
   }

   public ItemStack getItem(int slot) {
      return this.inv.getStackInSlot(slot);
   }

   public ItemStack removeItem(int slot, int count) {
      ItemStack stack = this.inv.getStackInSlot(slot);
      return stack.isEmpty() ? ItemStack.EMPTY : stack.split(count);
   }

   public void setItem(int slot, ItemStack stack) {
      this.inv.setStackInSlot(slot, stack);
   }

   public ItemStack removeItemNoUpdate(int index) {
      ItemStack s = this.getItem(index);
      if (s.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.setItem(index, ItemStack.EMPTY);
         return s;
      }
   }

   public boolean isEmpty() {
      for (int i = 0; i < this.inv.getSlots(); i++) {
         if (!this.inv.getStackInSlot(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public boolean canPlaceItem(int slot, ItemStack stack) {
      return this.inv.isItemValid(slot, stack);
   }

   public void clearContent() {
      for (int i = 0; i < this.inv.getSlots(); i++) {
         this.inv.setStackInSlot(i, ItemStack.EMPTY);
      }
   }

   public int getMaxStackSize() {
      return 0;
   }

   public void setChanged() {
   }

   public boolean stillValid(Player player) {
      return false;
   }

   public void startOpen(Player player) {
   }

   public void stopOpen(Player player) {
   }
}
