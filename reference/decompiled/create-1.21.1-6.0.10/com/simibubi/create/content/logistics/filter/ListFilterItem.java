package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ListFilterItem extends FilterItem {
   protected ListFilterItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<Component> makeSummary(ItemStack filter) {
      List<Component> list = new ArrayList<>();
      ItemStackHandler filterItems = this.getFilterItemHandler(filter);
      boolean blacklist = (Boolean)filter.getOrDefault(AllDataComponents.FILTER_ITEMS_BLACKLIST, false);
      list.add(
         (blacklist ? CreateLang.translateDirect("gui.filter.deny_list") : CreateLang.translateDirect("gui.filter.allow_list")).withStyle(ChatFormatting.GOLD)
      );
      int count = 0;

      for (int i = 0; i < filterItems.getSlots(); i++) {
         if (count > 3) {
            list.add(Component.literal("- ...").withStyle(ChatFormatting.DARK_GRAY));
            break;
         }

         ItemStack filterStack = filterItems.getStackInSlot(i);
         if (!filterStack.isEmpty()) {
            list.add(Component.literal("- ").append(filterStack.getHoverName()).withStyle(ChatFormatting.GRAY));
            count++;
         }
      }

      return count == 0 ? Collections.emptyList() : list;
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return FilterMenu.create(id, inv, player.getMainHandItem());
   }

   @Override
   public DataComponentType<?> getComponentType() {
      return AllDataComponents.FILTER_ITEMS;
   }

   @Override
   public FilterItemStack makeStackWrapper(ItemStack filter) {
      return new FilterItemStack.ListFilterItemStack(filter);
   }

   public ItemStackHandler getFilterItemHandler(ItemStack stack) {
      ItemStackHandler newInv = new ItemStackHandler(18);
      ItemContainerContents contents = (ItemContainerContents)stack.getOrDefault(AllDataComponents.FILTER_ITEMS, ItemContainerContents.EMPTY);
      ItemHelper.fillItemStackHandler(contents, newInv);
      return newInv;
   }

   @Override
   public ItemStack[] getFilterItems(ItemStack stack) {
      return stack.getOrDefault(AllDataComponents.FILTER_ITEMS_BLACKLIST, false)
         ? new ItemStack[0]
         : ItemHelper.getNonEmptyStacks(this.getFilterItemHandler(stack)).toArray(ItemStack[]::new);
   }
}
