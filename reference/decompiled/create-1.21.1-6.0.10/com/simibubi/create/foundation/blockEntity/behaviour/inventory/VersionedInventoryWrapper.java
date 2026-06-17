package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class VersionedInventoryWrapper implements IItemHandlerModifiable {
   public static final AtomicInteger idGenerator = new AtomicInteger();
   private IItemHandlerModifiable inventory;
   private int version;
   private int id = idGenerator.getAndIncrement();

   public VersionedInventoryWrapper(IItemHandlerModifiable inventory) {
      this.inventory = inventory;
      this.version = 0;
   }

   public void incrementVersion() {
      this.version++;
   }

   public int getVersion() {
      return this.version;
   }

   public int getId() {
      return this.id;
   }

   public int getSlots() {
      return this.inventory.getSlots();
   }

   public int getSlotLimit(int slot) {
      return this.inventory.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return this.inventory.isItemValid(slot, stack);
   }

   public ItemStack getStackInSlot(int slot) {
      return this.inventory.getStackInSlot(slot);
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      int count = stack.getCount();
      ItemStack result = this.inventory.insertItem(slot, stack, simulate);
      if (!simulate && count != result.getCount()) {
         this.incrementVersion();
      }

      return result;
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      ItemStack result = this.inventory.extractItem(slot, amount, simulate);
      if (!simulate && !result.isEmpty()) {
         this.incrementVersion();
      }

      return result;
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      ItemStack previousItem = this.inventory.getStackInSlot(slot);
      this.inventory.setStackInSlot(slot, stack);
      if (stack.isEmpty() == previousItem.isEmpty()) {
         if (stack.isEmpty()) {
            return;
         }

         if (ItemStack.isSameItemSameComponents(stack, previousItem) && stack.getCount() == previousItem.getCount()) {
            return;
         }
      }

      this.incrementVersion();
   }
}
