package com.simibubi.create.foundation.item.render;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CustomRenderedItems {
   private static final Set<Item> ITEMS = new ReferenceOpenHashSet();
   private static boolean itemsFiltered = false;

   public static void register(Item item) {
      ITEMS.add(item);
   }

   public static void forEach(Consumer<Item> consumer) {
      if (!itemsFiltered) {
         Iterator<Item> iterator = ITEMS.iterator();

         while (iterator.hasNext()) {
            Item item = iterator.next();
            if (!BuiltInRegistries.ITEM.containsValue(item) || !(IClientItemExtensions.of(item).getCustomRenderer() instanceof CustomRenderedItemModelRenderer)
               )
             {
               iterator.remove();
            }
         }

         itemsFiltered = true;
      }

      ITEMS.forEach(consumer);
   }
}
