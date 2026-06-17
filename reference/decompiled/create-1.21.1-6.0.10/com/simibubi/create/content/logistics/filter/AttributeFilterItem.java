package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.attributes.InTagAttribute;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;

public class AttributeFilterItem extends FilterItem {
   protected AttributeFilterItem(Properties properties) {
      super(properties);
   }

   @Override
   public List<Component> makeSummary(ItemStack filter) {
      List<Component> list = new ArrayList<>();
      AttributeFilterWhitelistMode whitelistMode = (AttributeFilterWhitelistMode)filter.get(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE);
      list.add(
         (whitelistMode == AttributeFilterWhitelistMode.WHITELIST_CONJ
               ? CreateLang.translateDirect("gui.attribute_filter.allow_list_conjunctive")
               : (
                  whitelistMode == AttributeFilterWhitelistMode.WHITELIST_DISJ
                     ? CreateLang.translateDirect("gui.attribute_filter.allow_list_disjunctive")
                     : CreateLang.translateDirect("gui.attribute_filter.deny_list")
               ))
            .withStyle(ChatFormatting.GOLD)
      );
      int count = 0;

      for (ItemAttribute.ItemAttributeEntry attributeEntry : (List)filter.getOrDefault(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES, List.of())) {
         ItemAttribute attribute = attributeEntry.attribute();
         if (attribute != null) {
            boolean inverted = attributeEntry.inverted();
            if (count > 3) {
               list.add(Component.literal("- ...").withStyle(ChatFormatting.DARK_GRAY));
               break;
            }

            list.add(Component.literal("- ").append(attribute.format(inverted)));
            count++;
         }
      }

      return count == 0 ? Collections.emptyList() : list;
   }

   @Override
   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return AttributeFilterMenu.create(id, inv, player.getMainHandItem());
   }

   @Override
   public DataComponentType<?> getComponentType() {
      return AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES;
   }

   @Override
   public FilterItemStack makeStackWrapper(ItemStack filter) {
      return new FilterItemStack.AttributeFilterItemStack(filter);
   }

   @Override
   public ItemStack[] getFilterItems(ItemStack stack) {
      AttributeFilterWhitelistMode whitelistMode = (AttributeFilterWhitelistMode)stack.get(AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE);
      List<ItemAttribute.ItemAttributeEntry> attributes = (List<ItemAttribute.ItemAttributeEntry>)stack.getOrDefault(
         AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES, List.of()
      );
      if (whitelistMode == AttributeFilterWhitelistMode.WHITELIST_DISJ
         && attributes.size() == 1
         && attributes.getFirst().attribute() instanceof InTagAttribute var5) {
         InTagAttribute var10000 = var5;

         try {
            var12 = var10000.tag();
         } catch (Throwable var10) {
            throw new MatchException(var10.toString(), var10);
         }

         TagKey stacks = var12;
         List<ItemStack> stacksx = new ArrayList<>();

         for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(stacks)) {
            stacksx.add(new ItemStack((ItemLike)holder.value()));
         }

         return stacksx.toArray(ItemStack[]::new);
      } else {
         return new ItemStack[0];
      }
   }
}
