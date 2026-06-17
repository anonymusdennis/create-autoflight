package com.simibubi.create.api.contraption.storage.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public abstract class WrapperMountedItemStorage<T extends IItemHandlerModifiable> extends MountedItemStorage {
   protected final T wrapped;

   protected WrapperMountedItemStorage(MountedItemStorageType<?> type, T wrapped) {
      super(type);
      this.wrapped = wrapped;
   }

   public void setStackInSlot(int slot, @NotNull ItemStack stack) {
      this.wrapped.setStackInSlot(slot, stack);
   }

   public int getSlots() {
      return this.wrapped.getSlots();
   }

   @NotNull
   public ItemStack getStackInSlot(int slot) {
      return this.wrapped.getStackInSlot(slot);
   }

   @NotNull
   public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      return this.wrapped.insertItem(slot, stack, simulate);
   }

   @NotNull
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return this.wrapped.extractItem(slot, amount, simulate);
   }

   public int getSlotLimit(int slot) {
      return this.wrapped.getSlotLimit(slot);
   }

   public boolean isItemValid(int slot, @NotNull ItemStack stack) {
      return this.wrapped.isItemValid(slot, stack);
   }

   public static ItemStackHandler copyToItemStackHandler(IItemHandler handler) {
      ItemStackHandler copy = new ItemStackHandler(handler.getSlots());

      for (int i = 0; i < handler.getSlots(); i++) {
         copy.setStackInSlot(i, handler.getStackInSlot(i).copy());
      }

      return copy;
   }
}
