package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class PackagerItemHandler implements IItemHandlerModifiable {
   private PackagerBlockEntity blockEntity;

   public PackagerItemHandler(PackagerBlockEntity blockEntity) {
      this.blockEntity = blockEntity;
   }

   public int getSlots() {
      return 1;
   }

   public ItemStack getStackInSlot(int slot) {
      return this.blockEntity.heldBox;
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      if (slot == 0) {
         this.blockEntity.heldBox = stack;
         this.blockEntity.notifyUpdate();
      }
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (!this.blockEntity.heldBox.isEmpty() || !this.blockEntity.queuedExitingPackages.isEmpty()) {
         return stack;
      } else if (!this.isItemValid(slot, stack)) {
         return stack;
      } else if (!this.blockEntity.unwrapBox(stack, true)) {
         return stack;
      } else {
         if (!simulate) {
            this.blockEntity.unwrapBox(stack, false);
            this.blockEntity.triggerStockCheck();
         }

         return stack.copyWithCount(stack.getCount() - 1);
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (this.blockEntity.animationTicks != 0) {
         return ItemStack.EMPTY;
      } else {
         ItemStack box = this.blockEntity.heldBox;
         if (!simulate) {
            this.setStackInSlot(slot, ItemStack.EMPTY);
         }

         return box;
      }
   }

   public int getSlotLimit(int slot) {
      return 1;
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return PackageItem.isPackage(stack);
   }
}
