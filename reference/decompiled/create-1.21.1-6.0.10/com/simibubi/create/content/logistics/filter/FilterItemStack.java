package com.simibubi.create.content.logistics.filter;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemStackHandler;

public class FilterItemStack {
   private final ItemStack filterItemStack;
   private boolean fluidExtracted;
   private FluidStack filterFluidStack;

   public static FilterItemStack of(ItemStack filter) {
      if (!filter.isComponentsPatchEmpty() && filter.getItem() instanceof FilterItem item) {
         trimFilterComponents(filter);
         return item.makeStackWrapper(filter);
      } else {
         return new FilterItemStack(filter);
      }
   }

   public static FilterItemStack of(Provider registries, CompoundTag tag) {
      return of(ItemStack.parseOptional(registries, tag));
   }

   public static FilterItemStack empty() {
      return of(ItemStack.EMPTY);
   }

   private static void trimFilterComponents(ItemStack filter) {
      filter.remove(DataComponents.ENCHANTMENTS);
      filter.remove(DataComponents.ATTRIBUTE_MODIFIERS);
   }

   public boolean isEmpty() {
      return this.filterItemStack.isEmpty();
   }

   public CompoundTag serializeNBT(Provider registries) {
      return (CompoundTag)this.filterItemStack.saveOptional(registries);
   }

   public ItemStack item() {
      return this.filterItemStack;
   }

   public FluidStack fluid(Level level) {
      this.resolveFluid(level);
      return this.filterFluidStack;
   }

   public boolean isFilterItem() {
      return this.filterItemStack.getItem() instanceof FilterItem;
   }

   public boolean test(Level world, ItemStack stack) {
      return this.test(world, stack, false);
   }

   public boolean test(Level world, FluidStack stack) {
      return this.test(world, stack, true);
   }

   public boolean test(Level world, ItemStack stack, boolean matchNBT) {
      return this.isEmpty() ? true : FilterItem.testDirect(this.filterItemStack, stack, matchNBT);
   }

   public boolean test(Level world, FluidStack stack, boolean matchNBT) {
      if (this.isEmpty()) {
         return true;
      } else if (stack.isEmpty()) {
         return false;
      } else {
         this.resolveFluid(world);
         if (this.filterFluidStack.isEmpty()) {
            return false;
         } else {
            return !matchNBT ? this.filterFluidStack.getFluid().isSame(stack.getFluid()) : FluidStack.isSameFluidSameComponents(this.filterFluidStack, stack);
         }
      }
   }

   private void resolveFluid(Level world) {
      if (!this.fluidExtracted) {
         this.fluidExtracted = true;
         if (GenericItemEmptying.canItemBeEmptied(world, this.filterItemStack)) {
            this.filterFluidStack = (FluidStack)GenericItemEmptying.emptyItem(world, this.filterItemStack, true).getFirst();
         }
      }
   }

   protected FilterItemStack(ItemStack filter) {
      this.filterItemStack = filter;
      this.filterFluidStack = FluidStack.EMPTY;
      this.fluidExtracted = false;
   }

   public static class AttributeFilterItemStack extends FilterItemStack {
      public AttributeFilterWhitelistMode whitelistMode;
      public List<Pair<ItemAttribute, Boolean>> attributeTests;

      public AttributeFilterItemStack(ItemStack filter) {
         super(filter);
         boolean defaults = !filter.has(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES);
         this.attributeTests = new ArrayList<>();
         this.whitelistMode = (AttributeFilterWhitelistMode)filter.getOrDefault(
            AllDataComponents.ATTRIBUTE_FILTER_WHITELIST_MODE, AttributeFilterWhitelistMode.WHITELIST_DISJ
         );

         for (ItemAttribute.ItemAttributeEntry attributeEntry : defaults
            ? new ArrayList()
            : (List)filter.get(AllDataComponents.ATTRIBUTE_FILTER_MATCHED_ATTRIBUTES)) {
            ItemAttribute attribute = attributeEntry.attribute();
            if (attribute != null) {
               this.attributeTests.add(Pair.of(attribute, attributeEntry.inverted()));
            }
         }
      }

      @Override
      public boolean test(Level world, FluidStack stack, boolean matchNBT) {
         return false;
      }

      @Override
      public boolean test(Level world, ItemStack stack, boolean matchNBT) {
         if (this.attributeTests.isEmpty()) {
            return super.test(world, stack, matchNBT);
         } else {
            for (Pair<ItemAttribute, Boolean> test : this.attributeTests) {
               ItemAttribute attribute = (ItemAttribute)test.getFirst();
               boolean inverted = (Boolean)test.getSecond();
               boolean matches = attribute.appliesTo(stack, world) != inverted;
               if (matches) {
                  switch (this.whitelistMode) {
                     case BLACKLIST:
                        return false;
                     case WHITELIST_CONJ:
                     default:
                        break;
                     case WHITELIST_DISJ:
                        return true;
                  }
               } else {
                  switch (this.whitelistMode) {
                     case BLACKLIST:
                     case WHITELIST_DISJ:
                     default:
                        break;
                     case WHITELIST_CONJ:
                        return false;
                  }
               }
            }
            return switch (this.whitelistMode) {
               case BLACKLIST, WHITELIST_CONJ -> true;
               case WHITELIST_DISJ -> false;
            };
         }
      }
   }

   public static class ListFilterItemStack extends FilterItemStack {
      public List<FilterItemStack> containedItems;
      public boolean shouldRespectNBT;
      public boolean isBlacklist;

      public ListFilterItemStack(ItemStack filter) {
         super(filter);
         boolean hasFilterItems = filter.has(AllDataComponents.FILTER_ITEMS);
         this.containedItems = new ArrayList<>();
         ItemStackHandler items = ((ListFilterItem)filter.getItem()).getFilterItemHandler(filter);

         for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stackInSlot = items.getStackInSlot(i);
            if (!stackInSlot.isEmpty()) {
               this.containedItems.add(FilterItemStack.of(stackInSlot));
            }
         }

         this.shouldRespectNBT = hasFilterItems && (Boolean)filter.getOrDefault(AllDataComponents.FILTER_ITEMS_RESPECT_NBT, false);
         this.isBlacklist = hasFilterItems && (Boolean)filter.getOrDefault(AllDataComponents.FILTER_ITEMS_BLACKLIST, false);
      }

      @Override
      public boolean test(Level world, ItemStack stack, boolean matchNBT) {
         for (FilterItemStack filterItemStack : this.containedItems) {
            if (filterItemStack.test(world, stack, this.shouldRespectNBT)) {
               return !this.isBlacklist;
            }
         }

         return this.isBlacklist;
      }

      @Override
      public boolean test(Level world, FluidStack stack, boolean matchNBT) {
         for (FilterItemStack filterItemStack : this.containedItems) {
            if (filterItemStack.test(world, stack, this.shouldRespectNBT)) {
               return !this.isBlacklist;
            }
         }

         return this.isBlacklist;
      }
   }

   public static class PackageFilterItemStack extends FilterItemStack {
      public String filterString;

      public PackageFilterItemStack(ItemStack filter) {
         super(filter);
         this.filterString = PackageItem.getAddress(filter);
      }

      @Override
      public boolean test(Level world, ItemStack stack, boolean matchNBT) {
         return this.filterString.isBlank() && super.test(world, stack, matchNBT)
            || PackageItem.isPackage(stack) && PackageItem.matchAddress(stack, this.filterString);
      }

      @Override
      public boolean test(Level world, FluidStack stack, boolean matchNBT) {
         return false;
      }
   }
}
