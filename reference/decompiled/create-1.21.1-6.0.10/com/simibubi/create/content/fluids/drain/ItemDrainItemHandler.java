package com.simibubi.create.content.fluids.drain;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class ItemDrainItemHandler implements IItemHandler {
   private ItemDrainBlockEntity blockEntity;
   private Direction side;

   public ItemDrainItemHandler(ItemDrainBlockEntity be, Direction side) {
      this.blockEntity = be;
      this.side = side;
   }

   public int getSlots() {
      return 1;
   }

   public ItemStack getStackInSlot(int slot) {
      return this.blockEntity.getHeldItemStack();
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (!this.blockEntity.getHeldItemStack().isEmpty()) {
         return stack;
      } else {
         ItemStack returned = ItemStack.EMPTY;
         if (stack.getCount() > 1 && GenericItemEmptying.canItemBeEmptied(this.blockEntity.getLevel(), stack)) {
            returned = stack.copyWithCount(stack.getCount() - 1);
            stack = stack.copyWithCount(1);
         } else {
            returned = ItemHelper.limitCountToMaxStackSize(stack, simulate);
         }

         if (!simulate) {
            TransportedItemStack heldItem = new TransportedItemStack(stack);
            heldItem.prevBeltPosition = 0.0F;
            this.blockEntity.setHeldItem(heldItem, this.side.getOpposite());
            this.blockEntity.notifyUpdate();
         }

         return returned;
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      TransportedItemStack held = this.blockEntity.heldItem;
      if (held == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = held.stack.copy();
         ItemStack extracted = stack.split(amount);
         if (!simulate) {
            this.blockEntity.heldItem.stack = stack;
            if (stack.isEmpty()) {
               this.blockEntity.heldItem = null;
            }

            this.blockEntity.notifyUpdate();
         }

         return extracted;
      }
   }

   public int getSlotLimit(int slot) {
      return 64;
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      return true;
   }
}
