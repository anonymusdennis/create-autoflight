package com.simibubi.create.content.logistics.packagerLink;

import com.google.common.cache.Cache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.utility.TickBasedCache;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public class LogisticsManager {
   private static Random r = new Random();
   public static final Cache<UUID, InventorySummary> ACCURATE_SUMMARIES = new TickBasedCache<>(1, false);
   public static final Cache<UUID, InventorySummary> SUMMARIES = new TickBasedCache<>(20, false);

   public static InventorySummary getSummaryOfNetwork(UUID freqId, boolean accurate) {
      try {
         Cache<UUID, InventorySummary> cacheToUse = accurate ? ACCURATE_SUMMARIES : SUMMARIES;
         return (InventorySummary)cacheToUse.get(freqId, () -> createSummaryOfNetwork(freqId));
      } catch (ExecutionException var3) {
         var3.printStackTrace();
         return InventorySummary.EMPTY;
      }
   }

   private static InventorySummary createSummaryOfNetwork(UUID freqId) {
      InventorySummary summaryOfLinks = new InventorySummary();
      Set<InventoryIdentifier> processedInventories = new HashSet<>();

      for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(freqId, false)) {
         InventoryIdentifier currentInventoryId = getInventoryIdentifierFromLink(link);
         if (currentInventoryId == null || processedInventories.add(currentInventoryId)) {
            InventorySummary summary = link.getSummary(null);
            if (summary != InventorySummary.EMPTY) {
               summaryOfLinks.contributingLinks++;
               summaryOfLinks.add(summary);
            }
         }
      }

      return summaryOfLinks;
   }

   public static int getStockOf(UUID freqId, ItemStack stack, @Nullable IdentifiedInventory ignoredHandler) {
      int sum = 0;

      for (LogisticallyLinkedBehaviour link : LogisticallyLinkedBehaviour.getAllPresent(freqId, false)) {
         sum += link.getSummary(ignoredHandler).getCountOf(stack);
      }

      return sum;
   }

   public static boolean broadcastPackageRequest(
      UUID freqId, LogisticallyLinkedBehaviour.RequestType type, PackageOrderWithCrafts order, @Nullable IdentifiedInventory ignoredHandler, String address
   ) {
      if (order.isEmpty()) {
         return false;
      } else {
         Multimap<PackagerBlockEntity, PackagingRequest> requests = findPackagersForRequest(freqId, order, ignoredHandler, address);

         for (PackagerBlockEntity packager : requests.keySet()) {
            if (packager.isTooBusyFor(type)) {
               return false;
            }
         }

         performPackageRequests(requests);
         return true;
      }
   }

   public static Multimap<PackagerBlockEntity, PackagingRequest> findPackagersForRequest(
      UUID freqId, PackageOrderWithCrafts order, @Nullable IdentifiedInventory ignoredHandler, String address
   ) {
      List<BigItemStack> stacks = new ArrayList<>();

      for (BigItemStack stack : order.stacks()) {
         if (!stack.stack.isEmpty() && stack.count > 0) {
            stacks.add(stack);
         }
      }

      Multimap<PackagerBlockEntity, PackagingRequest> requests = HashMultimap.create();
      Iterable<LogisticallyLinkedBehaviour> allAvailableLinks = LogisticallyLinkedBehaviour.getAllPresent(freqId, true);
      Map<InventoryIdentifier, List<LogisticallyLinkedBehaviour>> linksByInventory = new HashMap<>();
      List<LogisticallyLinkedBehaviour> availableLinks = new ArrayList<>();

      for (LogisticallyLinkedBehaviour link : allAvailableLinks) {
         InventoryIdentifier inventoryId = getInventoryIdentifierFromLink(link);
         if (inventoryId != null) {
            linksByInventory.computeIfAbsent(inventoryId, k -> new ArrayList<>()).add(link);
         } else {
            availableLinks.add(link);
         }
      }

      for (List<LogisticallyLinkedBehaviour> linkGroup : linksByInventory.values()) {
         if (!linkGroup.isEmpty()) {
            LogisticallyLinkedBehaviour selectedLink = linkGroup.get(r.nextInt(linkGroup.size()));
            availableLinks.add(selectedLink);
         }
      }

      List<LogisticallyLinkedBehaviour> usedLinks = new ArrayList<>();
      MutableBoolean finalLinkTracker = new MutableBoolean(false);
      PackageOrderWithCrafts context = order;
      int orderId = r.nextInt();

      for (int i = 0; i < stacks.size(); i++) {
         BigItemStack entry = stacks.get(i);
         int remainingCount = entry.count;
         boolean finalEntry = i == stacks.size() - 1;
         ItemStack requestedItem = entry.stack;

         for (LogisticallyLinkedBehaviour linkx : availableLinks) {
            int usedIndex = usedLinks.indexOf(linkx);
            int linkIndex = usedIndex == -1 ? usedLinks.size() : usedIndex;
            MutableBoolean isFinalLink = new MutableBoolean(false);
            if (linkIndex == usedLinks.size() - 1) {
               isFinalLink = finalLinkTracker;
            }

            Pair<PackagerBlockEntity, PackagingRequest> request = linkx.processRequest(
               requestedItem, remainingCount, address, linkIndex, isFinalLink, orderId, context, ignoredHandler
            );
            if (request != null) {
               requests.put((PackagerBlockEntity)request.getFirst(), (PackagingRequest)request.getSecond());
               int processedCount = ((PackagingRequest)request.getSecond()).getCount();
               if (processedCount > 0 && usedIndex == -1) {
                  context = null;
                  usedLinks.add(linkx);
                  finalLinkTracker = isFinalLink;
               }

               remainingCount -= processedCount;
               if (remainingCount <= 0) {
                  if (finalEntry) {
                     finalLinkTracker.setTrue();
                  }
                  break;
               }
            }
         }
      }

      return requests;
   }

   @Nullable
   private static InventoryIdentifier getInventoryIdentifierFromLink(LogisticallyLinkedBehaviour link) {
      if (link.blockEntity instanceof PackagerLinkBlockEntity plbe) {
         PackagerBlockEntity packager = plbe.getPackager();
         if (packager != null && packager.targetInventory.hasInventory()) {
            IdentifiedInventory identifiedInventory = packager.targetInventory.getIdentifiedInventory();
            return identifiedInventory != null ? identifiedInventory.identifier() : null;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void performPackageRequests(Multimap<PackagerBlockEntity, PackagingRequest> requests) {
      Map<PackagerBlockEntity, Collection<PackagingRequest>> asMap = requests.asMap();

      for (Entry<PackagerBlockEntity, Collection<PackagingRequest>> entry : asMap.entrySet()) {
         ArrayList<PackagingRequest> queuedRequests = new ArrayList<>(entry.getValue());
         PackagerBlockEntity packager = entry.getKey();
         if (!queuedRequests.isEmpty()) {
            packager.flashLink();
         }

         for (int i = 0; i < 100 && !queuedRequests.isEmpty(); i++) {
            packager.attemptToSend(queuedRequests);
         }

         packager.triggerStockCheck();
         packager.notifyUpdate();
      }
   }
}
