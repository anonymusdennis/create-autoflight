package dev.simulated_team.simulated.service;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;

public interface SimItemService {
   SimItemService INSTANCE = ServiceUtil.load(SimItemService.class);

   static DyeColor getDyeColor(ItemStack itemStack) {
      return itemStack.getItem() instanceof DyeItem dyeItem ? dyeItem.getDyeColor() : null;
   }

   int getBurnTime(ItemStack var1);

   int getSuperheatedBurnTime(ItemStack var1);
}
