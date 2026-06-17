package com.simibubi.create.content.logistics.packagePort;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.animatedContainer.AnimatedContainerBehaviour;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class PackagePortMenu extends MenuBase<PackagePortBlockEntity> {
   public PackagePortMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      super(type, id, inv, extraData);
   }

   public PackagePortMenu(MenuType<?> type, int id, Inventory inv, PackagePortBlockEntity be) {
      super(type, id, inv, be);
      BlockEntityBehaviour.get(be, AnimatedContainerBehaviour.TYPE).startOpen(this.player);
   }

   public static PackagePortMenu create(int id, Inventory inv, PackagePortBlockEntity be) {
      return new PackagePortMenu((MenuType<?>)AllMenuTypes.PACKAGE_PORT.get(), id, inv, be);
   }

   protected PackagePortBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
      BlockPos readBlockPos = extraData.readBlockPos();
      ClientLevel world = Minecraft.getInstance().level;
      BlockEntity blockEntity = world.getBlockEntity(readBlockPos);
      return blockEntity instanceof PackagePortBlockEntity ? (PackagePortBlockEntity)blockEntity : null;
   }

   public ItemStack quickMoveStack(Player player, int index) {
      Slot slot = (Slot)this.slots.get(index);
      if (!slot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = slot.getItem().copy();
         ItemStack moved = stack.copy();
         int size = this.contentHolder.inventory.getSlots();
         if (index < size) {
            if (!this.moveItemStackTo(stack, size, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(stack, 0, size, false)) {
            return ItemStack.EMPTY;
         }

         if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
         } else {
            slot.setByPlayer(stack.copy());
         }

         return moved;
      }
   }

   protected void initAndReadInventory(PackagePortBlockEntity contentHolder) {
   }

   @Override
   protected void addSlots() {
      SmartInventory inventory = this.contentHolder.inventory;
      int x = 27;
      int y = 9;

      for (int row = 0; row < 2; row++) {
         for (int col = 0; col < 9; col++) {
            this.addSlot(new SlotItemHandler(inventory, row * 9 + col, x + col * 18, y + row * 18));
         }
      }

      this.addPlayerSlots(38, 108);
   }

   protected void saveData(PackagePortBlockEntity contentHolder) {
   }

   @Override
   public void removed(Player playerIn) {
      super.removed(playerIn);
      if (!playerIn.level().isClientSide) {
         BlockEntityBehaviour.get(this.contentHolder, AnimatedContainerBehaviour.TYPE).stopOpen(playerIn);
      }
   }

   protected boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
      boolean success = false;
      int i = startIndex;
      if (reverseDirection) {
         i = endIndex - 1;
      }

      if (stack.isStackable()) {
         while (!stack.isEmpty() && (reverseDirection ? i >= startIndex : i < endIndex)) {
            Slot slot = (Slot)this.slots.get(i);
            ItemStack stackInSlot = slot.getItem();
            if (!stackInSlot.isEmpty() && ItemStack.isSameItemSameComponents(stack, stackInSlot)) {
               int totalCount = stackInSlot.getCount() + stack.getCount();
               int maxSize = Math.min(slot.getMaxStackSize(), stack.getMaxStackSize());
               if (totalCount <= maxSize) {
                  stack.setCount(0);
                  slot.setByPlayer(stackInSlot.copyWithCount(totalCount));
                  success = true;
               } else if (stackInSlot.getCount() < maxSize) {
                  stack.shrink(maxSize - stackInSlot.getCount());
                  slot.setByPlayer(stackInSlot.copyWithCount(maxSize));
                  success = true;
               }
            }

            if (reverseDirection) {
               i--;
            } else {
               i++;
            }
         }
      }

      if (!stack.isEmpty()) {
         if (reverseDirection) {
            i = endIndex - 1;
         } else {
            i = startIndex;
         }

         while (reverseDirection ? i >= startIndex : i < endIndex) {
            Slot slotx = (Slot)this.slots.get(i);
            ItemStack stackInSlotx = slotx.getItem();
            if (stackInSlotx.isEmpty() && slotx.mayPlace(stack)) {
               if (stack.getCount() > slotx.getMaxStackSize()) {
                  slotx.setByPlayer(stack.split(slotx.getMaxStackSize()));
               } else {
                  slotx.setByPlayer(stack.split(stack.getCount()));
               }

               slotx.setChanged();
               success = true;
               break;
            }

            if (reverseDirection) {
               i--;
            } else {
               i++;
            }
         }
      }

      return success;
   }
}
