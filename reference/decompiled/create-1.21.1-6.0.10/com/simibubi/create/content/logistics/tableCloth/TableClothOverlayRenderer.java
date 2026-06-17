package com.simibubi.create.content.logistics.tableCloth;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.content.logistics.stockTicker.StockTickerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.StockTickerInteractionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;

public class TableClothOverlayRenderer {
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         HitResult mouseOver = mc.hitResult;
         if (mouseOver != null) {
            ItemStack heldItem = mc.player.getMainHandItem();
            if (mouseOver.getType() != Type.ENTITY) {
               if (mouseOver instanceof BlockHitResult bhr) {
                  if (mc.level.getBlockEntity(bhr.getBlockPos()) instanceof TableClothBlockEntity dcbe) {
                     if (dcbe.isShop()) {
                        if (!AllBlocks.CLIPBOARD.isIn(heldItem)) {
                           if (!dcbe.targetsPriceTag(mc.player, bhr)) {
                              int alreadyPurchased = 0;
                              ShoppingListItem.ShoppingList list = ShoppingListItem.getList(heldItem);
                              if (list != null) {
                                 alreadyPurchased = list.getPurchases(dcbe.getBlockPos());
                              }

                              BlueprintOverlayRenderer.displayClothShop(dcbe, alreadyPurchased, list);
                           }
                        }
                     }
                  }
               }
            } else {
               EntityHitResult entityRay = (EntityHitResult)mouseOver;
               if (AllItems.SHOPPING_LIST.isIn(heldItem)) {
                  ShoppingListItem.ShoppingList list = ShoppingListItem.getList(heldItem);
                  BlockPos stockTickerPosition = StockTickerInteractionHandler.getStockTickerPosition(entityRay.getEntity());
                  if (list != null && stockTickerPosition != null) {
                     if (mc.level.getBlockEntity(stockTickerPosition) instanceof StockTickerBlockEntity tickerBE) {
                        if (tickerBE.behaviour.freqId.equals(list.shopNetwork())) {
                           BlueprintOverlayRenderer.displayShoppingList(list.bakeEntries(mc.level, null));
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
