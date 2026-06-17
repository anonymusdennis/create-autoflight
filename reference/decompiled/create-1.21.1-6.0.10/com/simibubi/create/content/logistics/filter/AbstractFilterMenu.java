package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.gui.menu.HeldItemGhostItemMenu;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractFilterMenu extends HeldItemGhostItemMenu {
   protected AbstractFilterMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      super(type, id, inv, extraData);
   }

   protected AbstractFilterMenu(MenuType<?> type, int id, Inventory inv, ItemStack contentHolder) {
      super(type, id, inv, contentHolder);
   }

   @Override
   protected boolean allowRepeats() {
      return false;
   }

   protected abstract int getPlayerInventoryXOffset();

   protected abstract int getPlayerInventoryYOffset();

   protected abstract void addFilterSlots();

   @Override
   protected void addSlots() {
      this.addPlayerSlots(this.getPlayerInventoryXOffset(), this.getPlayerInventoryYOffset());
      this.addFilterSlots();
   }

   protected void saveData(ItemStack contentHolder) {
      for (int i = 0; i < this.ghostInventory.getSlots(); i++) {
         if (!this.ghostInventory.getStackInSlot(i).isEmpty()) {
            contentHolder.set(AllDataComponents.FILTER_ITEMS, ItemHelper.containerContentsFromHandler(this.ghostInventory));
            return;
         }
      }

      contentHolder.remove(AllDataComponents.FILTER_ITEMS);
   }
}
