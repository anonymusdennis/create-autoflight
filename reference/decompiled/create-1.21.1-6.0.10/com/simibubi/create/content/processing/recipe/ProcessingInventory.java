package com.simibubi.create.content.processing.recipe;

import java.util.function.Consumer;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ProcessingInventory extends ItemStackHandler {
   public float remainingTime;
   public float recipeDuration;
   public boolean appliedRecipe;
   public Consumer<ItemStack> callback;
   private boolean limit;

   public ProcessingInventory(Consumer<ItemStack> callback) {
      super(32);
      this.callback = callback;
   }

   public ProcessingInventory withSlotLimit(boolean limit) {
      this.limit = limit;
      return this;
   }

   public int getSlotLimit(int slot) {
      return !this.limit ? super.getSlotLimit(slot) : 1;
   }

   public void clear() {
      for (int i = 0; i < this.getSlots(); i++) {
         this.setStackInSlot(i, ItemStack.EMPTY);
      }

      this.remainingTime = 0.0F;
      this.recipeDuration = 0.0F;
      this.appliedRecipe = false;
   }

   public boolean isEmpty() {
      for (int i = 0; i < this.getSlots(); i++) {
         if (!this.getStackInSlot(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      ItemStack insertItem = super.insertItem(slot, stack, simulate);
      if (slot == 0 && (insertItem.getCount() != stack.getCount() || !ItemStack.isSameItem(insertItem, stack))) {
         this.callback.accept(this.getStackInSlot(slot));
      }

      return insertItem;
   }

   @NotNull
   public CompoundTag serializeNBT(@NotNull Provider registries) {
      CompoundTag nbt = super.serializeNBT(registries);
      nbt.putFloat("ProcessingTime", this.remainingTime);
      nbt.putFloat("RecipeTime", this.recipeDuration);
      nbt.putBoolean("AppliedRecipe", this.appliedRecipe);
      return nbt;
   }

   public void deserializeNBT(@NotNull Provider registries, CompoundTag nbt) {
      this.remainingTime = nbt.getFloat("ProcessingTime");
      this.recipeDuration = nbt.getFloat("RecipeTime");
      this.appliedRecipe = nbt.getBoolean("AppliedRecipe");
      super.deserializeNBT(registries, nbt);
      if (this.isEmpty()) {
         this.appliedRecipe = false;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return ItemStack.EMPTY;
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return slot == 0 && this.isEmpty();
   }
}
