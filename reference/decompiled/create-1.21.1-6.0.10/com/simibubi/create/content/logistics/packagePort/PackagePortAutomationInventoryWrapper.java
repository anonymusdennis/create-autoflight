package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.foundation.item.ItemHandlerWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class PackagePortAutomationInventoryWrapper extends ItemHandlerWrapper {
   private final PackagePortBlockEntity ppbe;

   public PackagePortAutomationInventoryWrapper(IItemHandlerModifiable wrapped, PackagePortBlockEntity ppbe) {
      super(wrapped);
      this.ppbe = ppbe;
   }

   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      ItemStack preview = super.extractItem(slot, 64, true);
      if (!PackageItem.isPackage(preview)) {
         return ItemStack.EMPTY;
      } else {
         String filterString = this.ppbe.getFilterString();
         if (filterString != null && PackageItem.matchAddress(preview, filterString)) {
            return simulate ? preview : super.extractItem(slot, amount, false);
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   @Override
   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (!PackageItem.isPackage(stack)) {
         return stack;
      } else {
         String filterString = this.ppbe.getFilterString();
         return filterString != null && PackageItem.matchAddress(stack, filterString) ? stack : super.insertItem(slot, stack, simulate);
      }
   }
}
