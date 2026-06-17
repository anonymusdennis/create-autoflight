package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

public class DeployerItemHandler implements IItemHandlerModifiable {
   private DeployerBlockEntity be;
   private DeployerFakePlayer player;

   public DeployerItemHandler(DeployerBlockEntity be) {
      this.be = be;
      this.player = be.player;
   }

   public int getSlots() {
      return 1 + this.be.overflowItems.size();
   }

   public ItemStack getStackInSlot(int slot) {
      return slot >= this.be.overflowItems.size() ? this.getHeld() : this.be.overflowItems.get(slot);
   }

   public ItemStack getHeld() {
      return this.player == null ? ItemStack.EMPTY : this.player.getMainHandItem();
   }

   public void set(ItemStack stack) {
      if (this.player != null) {
         if (!this.be.getLevel().isClientSide) {
            this.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            this.be.setChanged();
            this.be.sendData();
         }
      }
   }

   public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
      if (slot < this.be.overflowItems.size()) {
         return stack;
      } else if (!this.isItemValid(slot, stack)) {
         return stack;
      } else {
         ItemStack held = this.getHeld();
         if (held.isEmpty()) {
            ItemStack remainder = ItemHelper.limitCountToMaxStackSize(stack, simulate);
            if (!simulate) {
               this.set(stack);
            }

            return remainder;
         } else if (!ItemStack.isSameItemSameComponents(held, stack)) {
            return stack;
         } else {
            int space = held.getMaxStackSize() - held.getCount();
            ItemStack remainder = stack.copy();
            ItemStack split = remainder.split(space);
            if (space == 0) {
               return stack;
            } else {
               if (!simulate) {
                  held = held.copy();
                  held.setCount(held.getCount() + split.getCount());
                  this.set(held);
               }

               return remainder;
            }
         }
      }
   }

   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (amount == 0) {
         return ItemStack.EMPTY;
      } else if (slot < this.be.overflowItems.size()) {
         ItemStack itemStack = this.be.overflowItems.get(slot);
         int toExtract = Math.min(amount, itemStack.getCount());
         ItemStack extracted = simulate ? itemStack.copy() : itemStack.split(toExtract);
         extracted.setCount(toExtract);
         if (!simulate && itemStack.isEmpty()) {
            this.be.overflowItems.remove(slot);
         }

         if (!simulate && !extracted.isEmpty()) {
            this.be.setChanged();
         }

         return extracted;
      } else {
         ItemStack held = this.getHeld();
         if (amount == 0 || held.isEmpty()) {
            return ItemStack.EMPTY;
         } else if (!this.be.filtering.getFilter().isEmpty() && this.be.filtering.test(held)) {
            return ItemStack.EMPTY;
         } else if (simulate) {
            return held.copy().split(amount);
         } else {
            ItemStack toReturn = held.split(amount);
            this.be.setChanged();
            this.be.sendData();
            return toReturn;
         }
      }
   }

   public int getSlotLimit(int slot) {
      return (Integer)this.getStackInSlot(slot).getOrDefault(DataComponents.MAX_STACK_SIZE, 64);
   }

   public boolean isItemValid(int slot, ItemStack stack) {
      FilteringBehaviour filteringBehaviour = this.be.getBehaviour(FilteringBehaviour.TYPE);
      return filteringBehaviour == null || filteringBehaviour.test(stack);
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      if (slot < this.be.overflowItems.size()) {
         this.be.overflowItems.set(slot, stack);
      } else {
         this.set(stack);
      }
   }
}
