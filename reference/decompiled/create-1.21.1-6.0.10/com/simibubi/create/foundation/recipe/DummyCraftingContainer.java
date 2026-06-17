package com.simibubi.create.foundation.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class DummyCraftingContainer extends TransientCraftingContainer {
   private final NonNullList<ItemStack> inv;

   public DummyCraftingContainer(IItemHandler itemHandler, int[] extractedItemsFromSlot) {
      super(null, 0, 0);
      this.inv = createInventory(itemHandler, extractedItemsFromSlot);
   }

   public int getContainerSize() {
      return this.inv.size();
   }

   public boolean isEmpty() {
      for (int slot = 0; slot < this.getContainerSize(); slot++) {
         if (!this.getItem(slot).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @NotNull
   public ItemStack getItem(int slot) {
      return slot >= this.getContainerSize() ? ItemStack.EMPTY : (ItemStack)this.inv.get(slot);
   }

   @NotNull
   public ItemStack removeItemNoUpdate(int slot) {
      return ItemStack.EMPTY;
   }

   @NotNull
   public ItemStack removeItem(int slot, int count) {
      return ItemStack.EMPTY;
   }

   public void setItem(int slot, @NotNull ItemStack stack) {
   }

   public void clearContent() {
   }

   public void fillStackedContents(@NotNull StackedContents helper) {
   }

   private static NonNullList<ItemStack> createInventory(IItemHandler itemHandler, int[] extractedItemsFromSlot) {
      NonNullList<ItemStack> inv = NonNullList.create();

      for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
         ItemStack stack = itemHandler.getStackInSlot(slot);
         if (!stack.isEmpty()) {
            for (int i = 0; i < extractedItemsFromSlot[slot]; i++) {
               ItemStack stackCopy = stack.copy();
               stackCopy.setCount(1);
               inv.add(stackCopy);
            }
         }
      }

      return inv;
   }
}
