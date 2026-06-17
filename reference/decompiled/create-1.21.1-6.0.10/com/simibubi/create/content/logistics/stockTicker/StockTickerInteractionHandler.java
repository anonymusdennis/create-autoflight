package com.simibubi.create.content.logistics.stockTicker;

import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.tableCloth.ShoppingListItem;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.items.ItemHandlerHelper;

@EventBusSubscriber
public class StockTickerInteractionHandler {
   @SubscribeEvent
   public static void interactWithLogisticsManager(EntityInteractSpecific event) {
      Entity entity = event.getTarget();
      Player player = event.getEntity();
      if (player != null && entity != null) {
         if (!player.isSpectator()) {
            Level level = event.getLevel();
            BlockPos targetPos = getStockTickerPosition(entity);
            if (targetPos != null) {
               if (interactWithLogisticsManagerAt(player, level, targetPos)) {
                  event.setCancellationResult(InteractionResult.SUCCESS);
                  event.setCanceled(true);
               }
            }
         }
      }
   }

   public static boolean interactWithLogisticsManagerAt(Player player, Level level, BlockPos targetPos) {
      ItemStack mainHandItem = player.getMainHandItem();
      if (AllItems.SHOPPING_LIST.isIn(mainHandItem)) {
         interactWithShop(player, level, targetPos, mainHandItem);
         return true;
      } else if (level.isClientSide()) {
         return true;
      } else if (level.getBlockEntity(targetPos) instanceof StockTickerBlockEntity stbe) {
         if (!stbe.behaviour.mayInteract(player)) {
            player.displayClientMessage(CreateLang.translate("stock_keeper.locked").style(ChatFormatting.RED).component(), true);
            return true;
         } else {
            if (player instanceof ServerPlayer sp) {
               boolean showLockOption = stbe.behaviour.mayAdministrate(player) && Create.LOGISTICS.isLockable(stbe.behaviour.freqId);
               boolean isCurrentlyLocked = Create.LOGISTICS.isLocked(stbe.behaviour.freqId);
               sp.openMenu(stbe.new RequestMenuProvider(), buf -> {
                  buf.writeBoolean(showLockOption);
                  buf.writeBoolean(isCurrentlyLocked);
                  buf.writeBlockPos(targetPos);
               });
               stbe.getRecentSummary().divideAndSendTo(sp, targetPos);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static void interactWithShop(Player player, Level level, BlockPos targetPos, ItemStack mainHandItem) {
      if (!level.isClientSide()) {
         if (level.getBlockEntity(targetPos) instanceof StockTickerBlockEntity tickerBE) {
            ShoppingListItem.ShoppingList list = ShoppingListItem.getList(mainHandItem);
            if (list != null) {
               if (!tickerBE.behaviour.freqId.equals(list.shopNetwork())) {
                  AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                  CreateLang.translate("stock_keeper.wrong_network").style(ChatFormatting.RED).sendStatus(player);
               } else {
                  Couple<InventorySummary> bakeEntries = list.bakeEntries(level, null);
                  InventorySummary paymentEntries = (InventorySummary)bakeEntries.getSecond();
                  InventorySummary orderEntries = (InventorySummary)bakeEntries.getFirst();
                  PackageOrder order = new PackageOrder(orderEntries.getStacksByCount());
                  tickerBE.getAccurateSummary();
                  InventorySummary recentSummary = tickerBE.getRecentSummary();

                  for (BigItemStack entry : order.stacks()) {
                     if (recentSummary.getCountOf(entry.stack) < entry.count) {
                        AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                        CreateLang.translate("stock_keeper.stock_level_too_low").style(ChatFormatting.RED).sendStatus(player);
                        return;
                     }
                  }

                  int occupiedSlots = 0;

                  for (BigItemStack entryx : paymentEntries.getStacksByCount()) {
                     occupiedSlots += Mth.ceil((float)entryx.count / (float)entryx.stack.getMaxStackSize());
                  }

                  for (int i = 0; i < tickerBE.receivedPayments.getSlots(); i++) {
                     if (tickerBE.receivedPayments.getStackInSlot(i).isEmpty()) {
                        occupiedSlots--;
                     }
                  }

                  if (occupiedSlots > 0) {
                     AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                     CreateLang.translate("stock_keeper.cash_register_full").style(ChatFormatting.RED).sendStatus(player);
                  } else {
                     for (boolean simulate : Iterate.trueAndFalse) {
                        InventorySummary tally = paymentEntries.copy();
                        List<ItemStack> toTransfer = new ArrayList<>();

                        for (int ix = 0; ix < player.getInventory().items.size(); ix++) {
                           ItemStack item = player.getInventory().getItem(ix);
                           if (!item.isEmpty()) {
                              int countOf = tally.getCountOf(item);
                              if (countOf != 0) {
                                 int toRemove = Math.min(item.getCount(), countOf);
                                 tally.add(item, -toRemove);
                                 if (!simulate) {
                                    int newStackSize = item.getCount() - toRemove;
                                    player.getInventory().setItem(ix, newStackSize == 0 ? ItemStack.EMPTY : item.copyWithCount(newStackSize));
                                    toTransfer.add(item.copyWithCount(toRemove));
                                 }
                              }
                           }
                        }

                        if (simulate && tally.getTotalCount() != 0) {
                           AllSoundEvents.DENY.playOnServer(level, player.blockPosition());
                           CreateLang.translate("stock_keeper.too_broke").style(ChatFormatting.RED).sendStatus(player);
                           return;
                        }

                        if (!simulate) {
                           toTransfer.forEach(s -> ItemHandlerHelper.insertItemStacked(tickerBE.receivedPayments, s, false));
                        }
                     }

                     tickerBE.broadcastPackageRequest(LogisticallyLinkedBehaviour.RequestType.PLAYER, order, null, ShoppingListItem.getAddress(mainHandItem));
                     player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                     if (!order.isEmpty()) {
                        AllSoundEvents.STOCK_TICKER_TRADE.playOnServer(level, tickerBE.getBlockPos());
                     }
                  }
               }
            }
         }
      }
   }

   public static BlockPos getStockTickerPosition(Entity entity) {
      Entity rootVehicle = entity.getRootVehicle();
      if (!(rootVehicle instanceof SeatEntity)) {
         return null;
      } else if (!(entity instanceof LivingEntity)) {
         return null;
      } else if (AllEntityTypes.PACKAGE.is(entity)) {
         return null;
      } else {
         BlockPos pos = entity.blockPosition();
         int stations = 0;
         BlockPos targetPos = null;

         for (Direction d : Iterate.horizontalDirections) {
            for (int y : Iterate.zeroAndOne) {
               BlockPos workstationPos = pos.relative(d).above(y);
               if (entity.level().getBlockState(workstationPos).getBlock() instanceof StockTickerBlock) {
                  targetPos = workstationPos;
                  stations++;
               }
            }
         }

         return stations != 1 ? null : targetPos;
      }
   }
}
