package com.simibubi.create.content.logistics.stockTicker;

import com.simibubi.create.AllMenuTypes;
import com.simibubi.create.foundation.gui.menu.MenuBase;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class StockKeeperRequestMenu extends MenuBase<StockTickerBlockEntity> {
   boolean isAdmin;
   boolean isLocked;
   public Object screenReference;

   public StockKeeperRequestMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf extraData) {
      super(type, id, inv, extraData);
   }

   public StockKeeperRequestMenu(MenuType<?> type, int id, Inventory inv, StockTickerBlockEntity contentHolder) {
      super(type, id, inv, contentHolder);
   }

   public static AbstractContainerMenu create(int pContainerId, Inventory pPlayerInventory, StockTickerBlockEntity stockTickerBlockEntity) {
      return new StockKeeperCategoryMenu((MenuType<?>)AllMenuTypes.STOCK_KEEPER_REQUEST.get(), pContainerId, pPlayerInventory, stockTickerBlockEntity);
   }

   protected StockTickerBlockEntity createOnClient(RegistryFriendlyByteBuf extraData) {
      this.isAdmin = extraData.readBoolean();
      this.isLocked = extraData.readBoolean();
      BlockEntity var3 = Minecraft.getInstance().level.getBlockEntity(extraData.readBlockPos());
      return var3 instanceof StockTickerBlockEntity ? (StockTickerBlockEntity)var3 : null;
   }

   protected void initAndReadInventory(StockTickerBlockEntity contentHolder) {
   }

   public void initializeContents(int pStateId, List<ItemStack> pItems, ItemStack pCarried) {
   }

   @Override
   protected void addSlots() {
      this.addPlayerSlots(-1000, 0);
   }

   protected void saveData(StockTickerBlockEntity contentHolder) {
   }

   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      return ItemStack.EMPTY;
   }
}
