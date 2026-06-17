package com.simibubi.create.foundation.item;

import java.util.Iterator;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

public class TagDependentIngredientItem extends Item {
   private TagKey<Item> tag;

   public TagDependentIngredientItem(Properties properties, TagKey<Item> tag) {
      super(properties);
      this.tag = tag;
   }

   public boolean shouldHide() {
      Iterator var1 = BuiltInRegistries.ITEM.getTagOrEmpty(this.tag).iterator();
      if (var1.hasNext()) {
         Holder<Item> ignored = (Holder<Item>)var1.next();
         return false;
      } else {
         return true;
      }
   }
}
