package com.simibubi.create.content.logistics.stockTicker;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class StockKeeperCategoryMenu extends MenuBase<StockTickerBlockEntity> {
   public boolean slotsActive = true;
   public ItemStackHandler proxyInventory;

   public StockKeeperCategoryMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      super(type, id, inv, extraData);
   }

   public static AbstractContainerMenu create(int pContainerId, Inventory pPlayerInventory, StockTickerBlockEntity stockTickerBlockEntity) {
      return new StockKeeperCategoryMenu((MenuType<?>)AllMenuTypes.STOCK_KEEPER_CATEGORY.get(), pContainerId, pPlayerInventory, stockTickerBlockEntity);
   }

   public StockKeeperCategoryMenu(MenuType<?> type, int id, Inventory inv, StockTickerBlockEntity contentHolder) {
      super(type, id, inv, contentHolder);
   }

   protected void initAndReadInventory(StockTickerBlockEntity contentHolder) {
      this.proxyInventory = new ItemStackHandler(1);
   }

   protected StockTickerBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
      BlockPos blockPos = extraData.readBlockPos();
      return ((StockTickerBlock)AllBlocks.STOCK_TICKER.get()).getBlockEntity(Minecraft.getInstance().level, blockPos);
   }

   @Override
   protected void addSlots() {
      this.addSlot(new StockKeeperCategoryMenu.InactiveItemHandlerSlot(this.proxyInventory, 0, 16, 24));
      this.addPlayerSlots(18, 106);
   }

   @Override
   protected Slot createPlayerSlot(Inventory inventory, int index, int x, int y) {
      return new StockKeeperCategoryMenu.InactiveSlot(inventory, index, x, y);
   }

   protected void saveData(StockTickerBlockEntity contentHolder) {
   }

   @Override
   public boolean stillValid(Player player) {
      return !this.contentHolder.isRemoved() && player.canInteractWithBlock(this.contentHolder.getBlockPos(), 4.0);
   }

   public ItemStack quickMoveStack(Player pPlayer, int index) {
      Slot clickedSlot = this.getSlot(index);
      if (!clickedSlot.hasItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack stack = clickedSlot.getItem();
         int size = 1;
         boolean success = false;
         if (index < size) {
            success = !this.moveItemStackTo(stack, size, this.slots.size(), true);
         } else {
            success = !this.moveItemStackTo(stack, 0, size, false);
         }

         return success ? ItemStack.EMPTY : stack;
      }
   }

   class InactiveItemHandlerSlot extends SlotItemHandler {
      public InactiveItemHandlerSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
         super(itemHandler, index, xPosition, yPosition);
      }

      public boolean mayPlace(@NotNull ItemStack stack) {
         return super.mayPlace(stack) && (stack.isEmpty() || stack.getItem() instanceof FilterItem);
      }

      public boolean isActive() {
         return StockKeeperCategoryMenu.this.slotsActive;
      }
   }

   class InactiveSlot extends Slot {
      public InactiveSlot(Container pContainer, int pIndex, int pX, int pY) {
         super(pContainer, pIndex, pX, pY);
      }

      public boolean isActive() {
         return StockKeeperCategoryMenu.this.slotsActive;
      }
   }
}
