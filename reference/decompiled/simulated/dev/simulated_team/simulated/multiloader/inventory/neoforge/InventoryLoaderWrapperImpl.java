package dev.simulated_team.simulated.multiloader.inventory.neoforge;

import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import dev.simulated_team.simulated.multiloader.inventory.InventoryLoaderWrapper;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.NotNull;

public class InventoryLoaderWrapperImpl extends InventoryLoaderWrapper {
   private final IItemHandler attachedInventory;

   public InventoryLoaderWrapperImpl(IItemHandler attachedInventory) {
      this.attachedInventory = attachedInventory;
   }

   @Override
   public ItemStack extractAny(int maxAmount, boolean simulate, boolean exact) {
      ItemStack extracted = ItemHelper.extract(
         this.attachedInventory, $ -> true, exact ? ExtractionCountMode.EXACTLY : ExtractionCountMode.UPTO, maxAmount, simulate
      );
      if (this.callback != null && !extracted.isEmpty() && !simulate) {
         this.callback.accept(true);
      }

      return extracted;
   }

   @Override
   public int insertGeneral(ItemInfoWrapper info, int amountToInsert, boolean simulate) {
      ItemStack is = ItemInfoWrapper.generateFromInfo(info);
      is.setCount(amountToInsert);
      int amountInserted = amountToInsert - ItemHandlerHelper.insertItem(this.attachedInventory, is, simulate).getCount();
      if (this.callback != null && amountInserted > 0 && !simulate) {
         this.callback.accept(false);
      }

      return amountInserted;
   }

   @Override
   public ItemStack insertSlot(ItemStack stack, int slot, boolean simulate) {
      ItemStack inserted = this.attachedInventory.insertItem(slot, stack, simulate);
      if (this.callback != null && !stack.equals(inserted) && !simulate) {
         this.callback.accept(false);
      }

      return inserted;
   }

   @Override
   public int extractGeneral(ItemInfoWrapper info, int amountToExtract, boolean simulate) {
      int extractAmount = ItemHelper.extract(this.attachedInventory, $ -> $.getItem() == info.type(), ExtractionCountMode.UPTO, amountToExtract, simulate)
         .getCount();
      if (this.callback != null && extractAmount > 0 && !simulate) {
         this.callback.accept(true);
      }

      return extractAmount;
   }

   @Override
   public ItemStack extractSlot(int index, int amountToExtract, boolean simulate) {
      ItemStack extracted = this.attachedInventory.extractItem(index, amountToExtract, simulate);
      if (this.callback != null && !extracted.isEmpty() && !simulate) {
         this.callback.accept(true);
      }

      return extracted;
   }

   @Override
   public int getContainerSize() {
      return this.attachedInventory.getSlots();
   }

   @Override
   public int getMaxStackSize() {
      return this.attachedInventory.getSlotLimit(0);
   }

   @NotNull
   @Override
   public ItemStack getItem(int slot) {
      return this.attachedInventory.getStackInSlot(slot);
   }
}
