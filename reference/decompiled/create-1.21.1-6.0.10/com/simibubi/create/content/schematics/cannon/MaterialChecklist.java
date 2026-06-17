package com.simibubi.create.content.schematics.cannon;

import com.google.common.collect.Sets;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.ClipboardContent;
import com.simibubi.create.content.equipment.clipboard.ClipboardEntry;
import com.simibubi.create.content.equipment.clipboard.ClipboardOverrides;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.utility.CreateLang;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.HoverEvent.Action;
import net.minecraft.network.chat.HoverEvent.ItemStackInfo;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

public class MaterialChecklist {
   public static final int MAX_ENTRIES_PER_PAGE = 5;
   public static final int MAX_ENTRIES_PER_CLIPBOARD_PAGE = 7;
   public Object2IntMap<Item> gathered = new Object2IntArrayMap();
   public Object2IntMap<Item> required = new Object2IntArrayMap();
   public Object2IntMap<Item> damageRequired = new Object2IntArrayMap();
   public boolean blocksNotLoaded;

   public void warnBlockNotLoaded() {
      this.blocksNotLoaded = true;
   }

   public void require(ItemRequirement requirement) {
      if (!requirement.isEmpty()) {
         if (!requirement.isInvalid()) {
            for (ItemRequirement.StackRequirement stack : requirement.getRequiredItems()) {
               if (stack.usage == ItemRequirement.ItemUseType.DAMAGE) {
                  this.putOrIncrement(this.damageRequired, stack.stack);
               }

               if (stack.usage == ItemRequirement.ItemUseType.CONSUME) {
                  this.putOrIncrement(this.required, stack.stack);
               }
            }
         }
      }
   }

   private void putOrIncrement(Object2IntMap<Item> map, ItemStack stack) {
      Item item = stack.getItem();
      if (item != Items.AIR) {
         if (map.containsKey(item)) {
            map.put(item, map.getInt(item) + stack.getCount());
         } else {
            map.put(item, stack.getCount());
         }
      }
   }

   public void collect(ItemStack stack) {
      Item item = stack.getItem();
      if (this.required.containsKey(item) || this.damageRequired.containsKey(item)) {
         if (this.gathered.containsKey(item)) {
            this.gathered.put(item, this.gathered.getInt(item) + stack.getCount());
         } else {
            this.gathered.put(item, stack.getCount());
         }
      }
   }

   public ItemStack createWrittenBook() {
      ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
      List<Filterable<Component>> pages = new ArrayList<>();
      int itemsWritten = 0;
      if (this.blocksNotLoaded) {
         MutableComponent textComponent = Component.literal("\n" + ChatFormatting.RED);
         textComponent = textComponent.append(CreateLang.translateDirect("materialChecklist.blocksNotLoaded"));
         pages.add(Filterable.passThrough(textComponent));
      }

      List<Item> keys = new ArrayList<>(Sets.union(this.required.keySet(), this.damageRequired.keySet()));
      Collections.sort(keys, (item1, item2) -> {
         Locale locale = Locale.ENGLISH;
         String name1 = item1.getDescription().getString().toLowerCase(locale);
         String name2 = item2.getDescription().getString().toLowerCase(locale);
         return name1.compareTo(name2);
      });
      MutableComponent textComponent = Component.empty();
      List<Item> completed = new ArrayList<>();

      for (Item item : keys) {
         int amount = this.getRequiredAmount(item);
         if (this.gathered.containsKey(item)) {
            amount -= this.gathered.getInt(item);
         }

         if (amount <= 0) {
            completed.add(item);
         } else {
            if (itemsWritten == 5) {
               itemsWritten = 0;
               textComponent.append(Component.literal("\n >>>").withStyle(ChatFormatting.BLUE));
               pages.add(Filterable.passThrough(textComponent));
               textComponent = Component.empty();
            }

            itemsWritten++;
            textComponent.append(this.entry(new ItemStack(item), amount, true, true));
         }
      }

      for (Item item : completed) {
         if (itemsWritten == 5) {
            itemsWritten = 0;
            textComponent.append(Component.literal("\n >>>").withStyle(ChatFormatting.DARK_GREEN));
            pages.add(Filterable.passThrough(textComponent));
            textComponent = Component.empty();
         }

         itemsWritten++;
         textComponent.append(this.entry(new ItemStack(item), this.getRequiredAmount(item), false, true));
      }

      pages.add(Filterable.passThrough(textComponent));
      WrittenBookContent contents = new WrittenBookContent(Filterable.passThrough(ChatFormatting.BLUE + "Material Checklist"), "Schematicannon", 0, pages, true);
      book.set(DataComponents.WRITTEN_BOOK_CONTENT, contents);
      textComponent = CreateLang.translateDirect("materialChecklist").setStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withItalic(Boolean.FALSE));
      book.set(DataComponents.CUSTOM_NAME, textComponent);
      return book;
   }

   public ItemStack createWrittenClipboard() {
      int itemsWritten = 0;
      List<List<ClipboardEntry>> pages = new ArrayList<>();
      List<ClipboardEntry> currentPage = new ArrayList<>();
      if (this.blocksNotLoaded) {
         currentPage.add(new ClipboardEntry(false, CreateLang.translateDirect("materialChecklist.blocksNotLoaded").withStyle(ChatFormatting.RED)));
      }

      List<Item> keys = new ArrayList<>(Sets.union(this.required.keySet(), this.damageRequired.keySet()));
      Collections.sort(keys, (item1, item2) -> {
         Locale locale = Locale.ENGLISH;
         String name1 = item1.getDescription().getString().toLowerCase(locale);
         String name2 = item2.getDescription().getString().toLowerCase(locale);
         return name1.compareTo(name2);
      });
      List<Item> completed = new ArrayList<>();

      for (Item item : keys) {
         int amount = this.getRequiredAmount(item);
         if (this.gathered.containsKey(item)) {
            amount -= this.gathered.getInt(item);
         }

         if (amount <= 0) {
            completed.add(item);
         } else {
            if (itemsWritten == 7) {
               itemsWritten = 0;
               currentPage.add(new ClipboardEntry(false, Component.literal(">>>").withStyle(ChatFormatting.DARK_GRAY)));
               pages.add(currentPage);
               currentPage = new ArrayList<>();
            }

            itemsWritten++;
            currentPage.add(new ClipboardEntry(false, this.entry(new ItemStack(item), amount, true, false)).displayItem(new ItemStack(item), amount));
         }
      }

      for (Item item : completed) {
         if (itemsWritten == 7) {
            itemsWritten = 0;
            currentPage.add(new ClipboardEntry(true, Component.literal(">>>").withStyle(ChatFormatting.DARK_GREEN)));
            pages.add(currentPage);
            currentPage = new ArrayList<>();
         }

         itemsWritten++;
         currentPage.add(
            new ClipboardEntry(true, this.entry(new ItemStack(item), this.getRequiredAmount(item), false, false)).displayItem(new ItemStack(item), 0)
         );
      }

      pages.add(currentPage);
      ItemStack clipboard = AllBlocks.CLIPBOARD.asStack();
      clipboard.set(AllDataComponents.CLIPBOARD_CONTENT, new ClipboardContent(ClipboardOverrides.ClipboardType.WRITTEN, pages, true));
      clipboard.set(DataComponents.CUSTOM_NAME, CreateLang.translateDirect("materialChecklist").setStyle(Style.EMPTY.withItalic(false)));
      return clipboard;
   }

   public int getRequiredAmount(Item item) {
      int amount = this.required.getOrDefault(item, 0);
      if (this.damageRequired.containsKey(item)) {
         amount += (int)Math.ceil((double)((float)this.damageRequired.getInt(item) / (float)new ItemStack(item).getMaxDamage()));
      }

      return amount;
   }

   private MutableComponent entry(ItemStack item, int amount, boolean unfinished, boolean forBook) {
      int stacks = amount / 64;
      int remainder = amount % 64;
      MutableComponent tc = Component.empty();
      tc.append(Component.translatable(item.getDescriptionId()).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_ITEM, new ItemStackInfo(item)))));
      if (!unfinished && forBook) {
         tc.append(" ✔");
      }

      if (!unfinished || forBook) {
         tc.withStyle(unfinished ? ChatFormatting.BLUE : ChatFormatting.DARK_GREEN);
      }

      return tc.append(Component.literal("\n x" + amount).withStyle(ChatFormatting.BLACK))
         .append(Component.literal(" | " + stacks + "▤ +" + remainder + (forBook ? "\n" : "")).withStyle(ChatFormatting.GRAY));
   }
}
