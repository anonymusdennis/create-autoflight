package com.simibubi.create.foundation.recipe;

import com.simibubi.create.AllRecipeTypes;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class ItemCopyingRecipe extends CustomRecipe {
   public ItemCopyingRecipe(CraftingBookCategory category) {
      super(category);
   }

   public boolean matches(CraftingInput input, Level level) {
      return this.copyCheck(input) != null;
   }

   public ItemStack assemble(CraftingInput input, Provider registries) {
      IntAttached<ItemStack> copyCheck = this.copyCheck(input);
      if (copyCheck == null) {
         return ItemStack.EMPTY;
      } else {
         ItemStack itemToCopy = (ItemStack)copyCheck.getValue();
         return itemToCopy.getItem() instanceof ItemCopyingRecipe.SupportsItemCopying sic
            ? sic.createCopy(itemToCopy, (Integer)copyCheck.getFirst() + 1)
            : ItemStack.EMPTY;
      }
   }

   @Nullable
   private IntAttached<ItemStack> copyCheck(CraftingInput input) {
      ItemStack itemToCopy = ItemStack.EMPTY;
      int copyTargets = 0;

      for (int j = 0; j < input.size(); j++) {
         ItemStack itemInSlot = input.getItem(j);
         if (!itemInSlot.isEmpty()) {
            if (!(itemInSlot.getItem() instanceof ItemCopyingRecipe.SupportsItemCopying sic)) {
               return null;
            }

            if (sic.canCopyFromItem(itemInSlot)) {
               itemToCopy = itemInSlot;
               break;
            }
         }
      }

      if (itemToCopy.isEmpty()) {
         return null;
      } else {
         for (int jx = 0; jx < input.size(); jx++) {
            ItemStack itemInSlot = input.getItem(jx);
            if (!itemInSlot.isEmpty() && itemInSlot != itemToCopy) {
               if (itemToCopy.getItem() != itemInSlot.getItem()) {
                  return null;
               }

               if (!(itemInSlot.getItem() instanceof ItemCopyingRecipe.SupportsItemCopying sic)) {
                  return null;
               }

               if (sic.canCopyFromItem(itemInSlot)) {
                  return null;
               }

               if (!sic.canCopyToItem(itemInSlot)) {
                  return null;
               }

               copyTargets++;
            }
         }

         return copyTargets == 0 ? null : IntAttached.with(copyTargets, itemToCopy);
      }
   }

   public RecipeSerializer<?> getSerializer() {
      return AllRecipeTypes.ITEM_COPYING.getSerializer();
   }

   public boolean canCraftInDimensions(int width, int height) {
      return width >= 2 && height >= 2;
   }

   public interface SupportsItemCopying {
      default ItemStack createCopy(ItemStack original, int count) {
         ItemStack copyWithCount = original.copyWithCount(count);
         copyWithCount.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
         copyWithCount.remove(DataComponents.STORED_ENCHANTMENTS);
         return copyWithCount;
      }

      default boolean canCopyFromItem(ItemStack item) {
         return item.has(this.getComponentType());
      }

      default boolean canCopyToItem(ItemStack item) {
         return !item.has(this.getComponentType());
      }

      DataComponentType<?> getComponentType();
   }
}
