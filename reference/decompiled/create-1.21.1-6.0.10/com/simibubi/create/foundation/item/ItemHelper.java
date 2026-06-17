package com.simibubi.create.foundation.item;

import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

public class ItemHelper {
   public static boolean sameItem(ItemStack stack, ItemStack otherStack) {
      return !otherStack.isEmpty() && stack.is(otherStack.getItem());
   }

   public static Predicate<ItemStack> sameItemPredicate(ItemStack stack) {
      return s -> sameItem(stack, s);
   }

   public static void dropContents(Level world, BlockPos pos, IItemHandler inv) {
      for (int slot = 0; slot < inv.getSlots(); slot++) {
         Containers.dropItemStack(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), inv.getStackInSlot(slot));
      }
   }

   public static List<ItemStack> multipliedOutput(ItemStack in, ItemStack out) {
      List<ItemStack> stacks = new ArrayList<>();
      ItemStack result = out.copy();
      result.setCount(in.getCount() * out.getCount());

      while (result.getCount() > result.getMaxStackSize()) {
         stacks.add(result.split(result.getMaxStackSize()));
      }

      stacks.add(result);
      return stacks;
   }

   public static void addToList(ItemStack stack, List<ItemStack> stacks) {
      for (ItemStack s : stacks) {
         if (ItemStack.isSameItemSameComponents(stack, s)) {
            int transferred = Math.min(s.getMaxStackSize() - s.getCount(), stack.getCount());
            s.grow(transferred);
            stack.shrink(transferred);
         }
      }

      if (stack.getCount() > 0) {
         stacks.add(stack);
      }
   }

   public static boolean isSameInventory(IItemHandler h1, IItemHandler h2) {
      if (h1 != null && h2 != null) {
         if (h1.getSlots() != h2.getSlots()) {
            return false;
         } else {
            for (int slot = 0; slot < h1.getSlots(); slot++) {
               if (h1.getStackInSlot(slot) != h2.getStackInSlot(slot)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static <T extends IBE<? extends BlockEntity>> int calcRedstoneFromBlockEntity(T ibe, Level level, BlockPos pos) {
      return ibe.getBlockEntityOptional(level, pos)
         .map(be -> (IItemHandler)level.getCapability(ItemHandler.BLOCK, pos, null))
         .map(ItemHelper::calcRedstoneFromInventory)
         .orElse(0);
   }

   public static int calcRedstoneFromInventory(@Nullable IItemHandler inv) {
      if (inv == null) {
         return 0;
      } else {
         int i = 0;
         float f = 0.0F;
         int totalSlots = inv.getSlots();

         for (int j = 0; j < inv.getSlots(); j++) {
            int slotLimit = inv.getSlotLimit(j);
            if (slotLimit == 0) {
               totalSlots--;
            } else {
               ItemStack itemstack = inv.getStackInSlot(j);
               if (!itemstack.isEmpty()) {
                  f += (float)itemstack.getCount() / (float)Math.min(slotLimit, itemstack.getMaxStackSize());
                  i++;
               }
            }
         }

         if (totalSlots == 0) {
            return 0;
         } else {
            f /= (float)totalSlots;
            return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
         }
      }
   }

   public static List<Pair<Ingredient, MutableInt>> condenseIngredients(NonNullList<Ingredient> recipeIngredients) {
      List<Pair<Ingredient, MutableInt>> actualIngredients = new ArrayList<>();

      label42:
      for (Ingredient igd : recipeIngredients) {
         for (Pair<Ingredient, MutableInt> pair : actualIngredients) {
            ItemStack[] stacks1 = ((Ingredient)pair.getFirst()).getItems();
            ItemStack[] stacks2 = igd.getItems();
            if (stacks1.length == stacks2.length) {
               for (int i = 0; i <= stacks1.length; i++) {
                  if (i == stacks1.length) {
                     ((MutableInt)pair.getSecond()).increment();
                     continue label42;
                  }

                  if (!ItemStack.matches(stacks1[i], stacks2[i])) {
                     break;
                  }
               }
            }
         }

         actualIngredients.add(Pair.of(igd, new MutableInt(1)));
      }

      return actualIngredients;
   }

   public static boolean matchIngredients(Ingredient i1, Ingredient i2) {
      if (i1 == i2) {
         return true;
      } else {
         ItemStack[] stacks1 = i1.getItems();
         ItemStack[] stacks2 = i2.getItems();
         if (stacks1 == stacks2) {
            return true;
         } else if (stacks1.length == stacks2.length) {
            for (int i = 0; i < stacks1.length; i++) {
               if (!ItemStack.isSameItem(stacks1[i], stacks2[i])) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public static boolean matchAllIngredients(NonNullList<Ingredient> ingredients) {
      if (ingredients.size() <= 1) {
         return true;
      } else {
         Ingredient firstIngredient = (Ingredient)ingredients.get(0);

         for (int i = 1; i < ingredients.size(); i++) {
            if (!matchIngredients(firstIngredient, (Ingredient)ingredients.get(i))) {
               return false;
            }
         }

         return true;
      }
   }

   public static ItemStack extract(IItemHandler inv, Predicate<ItemStack> test, boolean simulate) {
      return extract(inv, test, ItemHelper.ExtractionCountMode.UPTO, 64, simulate);
   }

   public static ItemStack extract(IItemHandler inv, Predicate<ItemStack> test, int exactAmount, boolean simulate) {
      return extract(inv, test, ItemHelper.ExtractionCountMode.EXACTLY, exactAmount, simulate);
   }

   public static ItemStack extract(IItemHandler inv, Predicate<ItemStack> test, ItemHelper.ExtractionCountMode mode, int amount, boolean simulate) {
      ItemStack extracting = ItemStack.EMPTY;
      boolean amountRequired = mode == ItemHelper.ExtractionCountMode.EXACTLY;
      boolean checkHasEnoughItems = amountRequired;
      boolean hasEnoughItems = !amountRequired;
      boolean potentialOtherMatch = false;
      int maxExtractionCount = amount;

      label77:
      while (true) {
         extracting = ItemStack.EMPTY;

         for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack slotStack = inv.getStackInSlot(slot);
            if (!slotStack.isEmpty()) {
               int amountToExtractFromThisSlot = Math.min(maxExtractionCount - extracting.getCount(), slotStack.getMaxStackSize());
               ItemStack stack = inv.extractItem(slot, amountToExtractFromThisSlot, true);
               if (!stack.isEmpty() && test.test(stack)) {
                  if (!extracting.isEmpty() && !canItemStackAmountsStack(stack, extracting)) {
                     potentialOtherMatch = true;
                  } else {
                     if (extracting.isEmpty()) {
                        extracting = stack.copy();
                     } else {
                        extracting.grow(stack.getCount());
                     }

                     if (!simulate && hasEnoughItems) {
                        inv.extractItem(slot, stack.getCount(), false);
                     }

                     if (extracting.getCount() >= maxExtractionCount) {
                        if (!checkHasEnoughItems) {
                           return amountRequired && extracting.getCount() < amount ? ItemStack.EMPTY : extracting;
                        }

                        hasEnoughItems = true;
                        checkHasEnoughItems = false;
                        continue label77;
                     }
                  }
               }
            }
         }

         if (!extracting.isEmpty() && !hasEnoughItems && potentialOtherMatch) {
            ItemStack blackListed = extracting.copy();
            test = test.and(i -> !ItemStack.isSameItemSameComponents(i, blackListed));
         } else {
            if (!checkHasEnoughItems) {
               return amountRequired && extracting.getCount() < amount ? ItemStack.EMPTY : extracting;
            }

            checkHasEnoughItems = false;
         }
      }
   }

   public static ItemStack extract(IItemHandler inv, Predicate<ItemStack> test, Function<ItemStack, Integer> amountFunction, boolean simulate) {
      ItemStack extracting = ItemStack.EMPTY;
      int maxExtractionCount = 64;

      for (int slot = 0; slot < inv.getSlots(); slot++) {
         if (extracting.isEmpty()) {
            ItemStack stackInSlot = inv.getStackInSlot(slot);
            if (stackInSlot.isEmpty() || !test.test(stackInSlot)) {
               continue;
            }

            int maxExtractionCountForItem = amountFunction.apply(stackInSlot);
            if (maxExtractionCountForItem == 0) {
               continue;
            }

            maxExtractionCount = Math.min(maxExtractionCount, maxExtractionCountForItem);
         }

         ItemStack stack = inv.extractItem(slot, maxExtractionCount - extracting.getCount(), true);
         if (test.test(stack) && (extracting.isEmpty() || canItemStackAmountsStack(stack, extracting))) {
            if (extracting.isEmpty()) {
               extracting = stack.copy();
            } else {
               extracting.grow(stack.getCount());
            }

            if (!simulate) {
               inv.extractItem(slot, stack.getCount(), false);
            }

            if (extracting.getCount() >= maxExtractionCount) {
               break;
            }
         }
      }

      return extracting;
   }

   public static boolean canItemStackAmountsStack(ItemStack a, ItemStack b) {
      return ItemStack.isSameItemSameComponents(a, b) && a.getCount() + b.getCount() <= a.getMaxStackSize();
   }

   public static ItemStack findFirstMatch(IItemHandler inv, Predicate<ItemStack> test) {
      int slot = findFirstMatchingSlotIndex(inv, test);
      return slot == -1 ? ItemStack.EMPTY : inv.getStackInSlot(slot);
   }

   public static int findFirstMatchingSlotIndex(IItemHandler inv, Predicate<ItemStack> test) {
      for (int slot = 0; slot < inv.getSlots(); slot++) {
         ItemStack toTest = inv.getStackInSlot(slot);
         if (test.test(toTest)) {
            return slot;
         }
      }

      return -1;
   }

   public static ItemStack fromItemEntity(Entity entityIn) {
      if (!entityIn.isAlive()) {
         return ItemStack.EMPTY;
      } else if (entityIn instanceof PackageEntity packageEntity) {
         return packageEntity.getBox();
      } else {
         return entityIn instanceof ItemEntity itemEntity ? itemEntity.getItem() : ItemStack.EMPTY;
      }
   }

   public static void fillItemStackHandler(ItemContainerContents contents, ItemStackHandler inv) {
      List<ItemStack> itemStacks = contents.stream().toList();

      for (int i = 0; i < itemStacks.size(); i++) {
         inv.setStackInSlot(i, itemStacks.get(i));
      }
   }

   public static ItemContainerContents containerContentsFromHandler(ItemStackHandler handler) {
      return ItemContainerContents.fromItems(((ItemStackHandlerAccessor)handler).create$getStacks());
   }

   public static ItemStack limitCountToMaxStackSize(ItemStack stack, boolean simulate) {
      int count = stack.getCount();
      int max = stack.getMaxStackSize();
      if (count <= max) {
         return ItemStack.EMPTY;
      } else {
         ItemStack remainder = stack.copyWithCount(count - max);
         if (!simulate) {
            stack.setCount(max);
         }

         return remainder;
      }
   }

   public static void copyContents(IItemHandler from, IItemHandlerModifiable to) {
      if (from.getSlots() != to.getSlots()) {
         throw new IllegalArgumentException("Slot count mismatch");
      } else {
         for (int slot = to.getSlots() - 1; slot >= 0; slot--) {
            to.setStackInSlot(slot, ItemStack.EMPTY);
         }

         for (int i = 0; i < from.getSlots(); i++) {
            to.setStackInSlot(i, from.getStackInSlot(i).copy());
         }
      }
   }

   public static List<ItemStack> getNonEmptyStacks(ItemStackHandler handler) {
      List<ItemStack> stacks = new ArrayList<>();

      for (int i = 0; i < handler.getSlots(); i++) {
         ItemStack stack = handler.getStackInSlot(i);
         if (!stack.isEmpty()) {
            stacks.add(stack);
         }
      }

      return stacks;
   }

   public static enum ExtractionCountMode {
      EXACTLY,
      UPTO;
   }
}
