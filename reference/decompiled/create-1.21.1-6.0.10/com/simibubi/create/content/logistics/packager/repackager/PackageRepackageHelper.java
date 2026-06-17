package com.simibubi.create.content.logistics.packager.repackager;

import com.google.common.collect.Lists;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PackageRepackageHelper {
   protected Map<Integer, List<ItemStack>> collectedPackages = new HashMap<>();

   public void clear() {
      this.collectedPackages.clear();
   }

   public boolean isFragmented(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA);
   }

   public int addPackageFragment(ItemStack box) {
      int collectedOrderId = PackageItem.getOrderId(box);
      if (collectedOrderId == -1) {
         return -1;
      } else {
         List<ItemStack> collectedOrder = this.collectedPackages.computeIfAbsent(collectedOrderId, $ -> Lists.newArrayList());
         collectedOrder.add(box);
         return !this.isOrderComplete(collectedOrderId) ? -1 : collectedOrderId;
      }
   }

   public List<BigItemStack> repack(int orderId, RandomSource r) {
      List<BigItemStack> exportingPackages = new ArrayList<>();
      String address = "";
      PackageOrderWithCrafts orderContext = null;
      InventorySummary summary = new InventorySummary();

      for (ItemStack box : this.collectedPackages.get(orderId)) {
         address = PackageItem.getAddress(box);
         if (box.has(AllDataComponents.PACKAGE_ORDER_DATA)) {
            PackageOrderWithCrafts context = ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).orderContext();
            if (context != null && !context.isEmpty()) {
               orderContext = context;
            }
         }

         ItemStackHandler contents = PackageItem.getContents(box);

         for (int slot = 0; slot < contents.getSlots(); slot++) {
            summary.add(contents.getStackInSlot(slot));
         }
      }

      List<BigItemStack> orderedStacks = new ArrayList<>();
      if (orderContext != null) {
         List<BigItemStack> packagesSplitByRecipe = this.repackBasedOnRecipes(summary, orderContext, address, r);
         exportingPackages.addAll(packagesSplitByRecipe);
         if (packagesSplitByRecipe.isEmpty()) {
            for (BigItemStack stack : orderContext.stacks()) {
               orderedStacks.add(new BigItemStack(stack.stack, stack.count));
            }
         }
      }

      List<BigItemStack> allItems = summary.getStacks();
      List<ItemStack> outputSlots = new ArrayList<>();

      label130:
      while (true) {
         allItems.removeIf(e -> e.count == 0);
         if (allItems.isEmpty()) {
            int currentSlot = 0;
            ItemStackHandler target = new ItemStackHandler(9);

            for (ItemStack item : outputSlots) {
               target.setStackInSlot(currentSlot++, item);
               if (currentSlot >= 9) {
                  exportingPackages.add(new BigItemStack(PackageItem.containing(target), 1));
                  target = new ItemStackHandler(9);
                  currentSlot = 0;
               }
            }

            for (int slot = 0; slot < target.getSlots(); slot++) {
               if (!target.getStackInSlot(slot).isEmpty()) {
                  exportingPackages.add(new BigItemStack(PackageItem.containing(target), 1));
                  break;
               }
            }

            for (BigItemStack box : exportingPackages) {
               PackageItem.addAddress(box.stack, address);
            }

            for (int i = 0; i < exportingPackages.size(); i++) {
               BigItemStack box = exportingPackages.get(i);
               boolean isfinal = i == exportingPackages.size() - 1;
               PackageOrderWithCrafts outboundOrderContext = isfinal && orderContext != null ? orderContext : null;
               if (PackageItem.getOrderId(box.stack) == -1) {
                  PackageItem.setOrder(box.stack, orderId, 0, true, 0, true, outboundOrderContext);
               }
            }

            return exportingPackages;
         }

         BigItemStack targetedEntry = null;
         if (!orderedStacks.isEmpty()) {
            targetedEntry = orderedStacks.remove(0);
         }

         Iterator target = allItems.iterator();

         while (true) {
            BigItemStack entry;
            int targetAmount;
            while (true) {
               if (!target.hasNext()) {
                  continue label130;
               }

               entry = (BigItemStack)target.next();
               targetAmount = entry.count;
               if (targetAmount != 0) {
                  if (targetedEntry == null) {
                     break;
                  }

                  targetAmount = targetedEntry.count;
                  if (ItemStack.isSameItemSameComponents(entry.stack, targetedEntry.stack)) {
                     break;
                  }
               }
            }

            while (true) {
               if (targetAmount <= 0) {
                  continue label130;
               }

               int removedAmount = Math.min(Math.min(targetAmount, entry.stack.getMaxStackSize()), entry.count);
               if (removedAmount == 0) {
                  break;
               }

               ItemStack output = entry.stack.copyWithCount(removedAmount);
               targetAmount -= removedAmount;
               if (targetedEntry != null) {
                  targetedEntry.count = targetAmount;
               }

               entry.count -= removedAmount;
               outputSlots.add(output);
            }
         }
      }
   }

   private boolean isOrderComplete(int orderId) {
      boolean finalLinkReached = false;

      label44:
      for (int linkCounter = 0; linkCounter < 1000 && !finalLinkReached; linkCounter++) {
         label42:
         for (int packageCounter = 0; packageCounter < 1000; packageCounter++) {
            for (ItemStack box : this.collectedPackages.get(orderId)) {
               PackageItem.PackageOrderData data = (PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA);
               if (linkCounter == data.linkIndex() && packageCounter == data.fragmentIndex()) {
                  finalLinkReached = data.isFinalLink();
                  if (data.isFinal()) {
                     continue label44;
                  }
                  continue label42;
               }
            }

            return false;
         }
      }

      return true;
   }

   protected List<BigItemStack> repackBasedOnRecipes(InventorySummary summary, PackageOrderWithCrafts order, String address, RandomSource r) {
      if (order.orderedCrafts().isEmpty()) {
         return List.of();
      } else {
         List<BigItemStack> packages = new ArrayList<>();

         for (PackageOrderWithCrafts.CraftingEntry craftingEntry : order.orderedCrafts()) {
            int packagesToCreate = 0;

            label39:
            for (int i = 0; i < craftingEntry.count(); i++) {
               for (BigItemStack required : craftingEntry.pattern().stacks()) {
                  if (!required.stack.isEmpty()) {
                     if (summary.getCountOf(required.stack) <= 0) {
                        break label39;
                     }

                     summary.add(required.stack, -1);
                  }
               }

               packagesToCreate++;
            }

            ItemStackHandler target = new ItemStackHandler(9);
            List<BigItemStack> stacks = craftingEntry.pattern().stacks();

            for (int currentSlot = 0; currentSlot < Math.min(stacks.size(), target.getSlots()); currentSlot++) {
               target.setStackInSlot(currentSlot, stacks.get(currentSlot).stack.copyWithCount(1));
            }

            ItemStack box = PackageItem.containing(target);
            PackageItem.setOrder(box, r.nextInt(), 0, true, 0, true, PackageOrderWithCrafts.singleRecipe(craftingEntry.pattern().stacks()));
            packages.add(new BigItemStack(box, packagesToCreate));
         }

         return packages;
      }
   }
}
