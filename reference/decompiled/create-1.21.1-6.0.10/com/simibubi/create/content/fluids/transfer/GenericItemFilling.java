package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;

public class GenericItemFilling {
   public static boolean isFluidHandlerValid(ItemStack stack, IFluidHandlerItem fluidHandler) {
      if (fluidHandler.getClass() == FluidBucketWrapper.class) {
         Item item = stack.getItem();
         if (item.getClass() != BucketItem.class && !(item instanceof MilkBucketItem)) {
            return false;
         }
      }

      return true;
   }

   public static boolean canItemBeFilled(Level world, ItemStack stack) {
      if (stack.getItem() == Items.GLASS_BOTTLE) {
         return true;
      } else if (stack.getItem() == Items.MILK_BUCKET) {
         return false;
      } else {
         IFluidHandlerItem capability = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
         if (capability == null) {
            return false;
         } else if (!isFluidHandlerValid(stack, capability)) {
            return false;
         } else {
            for (int i = 0; i < capability.getTanks(); i++) {
               if (capability.getFluidInTank(i).getAmount() < capability.getTankCapacity(i)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static int getRequiredAmountForItem(Level world, ItemStack stack, FluidStack availableFluid) {
      if (stack.getItem() == Items.GLASS_BOTTLE && canFillGlassBottleInternally(availableFluid)) {
         return PotionFluidHandler.getRequiredAmountForFilledBottle(stack, availableFluid);
      } else if (stack.getItem() == Items.BUCKET && canFillBucketInternally(availableFluid)) {
         return 1000;
      } else {
         IFluidHandlerItem capability = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
         if (capability == null) {
            return -1;
         } else if (capability instanceof FluidBucketWrapper) {
            Item filledBucket = availableFluid.getFluid().getBucket();
            if (filledBucket == null || filledBucket == Items.AIR) {
               return -1;
            } else {
               return !((FluidBucketWrapper)capability).getFluid().isEmpty() ? -1 : 1000;
            }
         } else {
            int filled = capability.fill(availableFluid, FluidAction.SIMULATE);
            return filled == 0 ? -1 : filled;
         }
      }
   }

   private static boolean canFillGlassBottleInternally(FluidStack availableFluid) {
      Fluid fluid = availableFluid.getFluid();
      if (fluid.isSame(Fluids.WATER)) {
         return true;
      } else {
         return fluid.isSame((Fluid)AllFluids.POTION.get()) ? true : fluid.isSame((Fluid)AllFluids.TEA.get());
      }
   }

   private static boolean canFillBucketInternally(FluidStack availableFluid) {
      return false;
   }

   public static ItemStack fillItem(Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid) {
      FluidStack toFill = availableFluid.copy();
      toFill.setAmount(requiredAmount);
      availableFluid.shrink(requiredAmount);
      if (stack.getItem() == Items.GLASS_BOTTLE && canFillGlassBottleInternally(toFill)) {
         Fluid fluid = toFill.getFluid();
         ItemStack fillBottle;
         if (FluidHelper.isWater(fluid)) {
            fillBottle = PotionContents.createItemStack(Items.POTION, Potions.WATER);
         } else if (fluid.isSame((Fluid)AllFluids.TEA.get())) {
            fillBottle = AllItems.BUILDERS_TEA.asStack();
         } else {
            fillBottle = PotionFluidHandler.fillBottle(stack, toFill);
         }

         stack.shrink(1);
         return fillBottle;
      } else {
         ItemStack split = stack.copy();
         split.setCount(1);
         IFluidHandlerItem capability = (IFluidHandlerItem)split.getCapability(FluidHandler.ITEM);
         if (capability == null) {
            return ItemStack.EMPTY;
         } else {
            capability.fill(toFill, FluidAction.EXECUTE);
            ItemStack container = capability.getContainer().copy();
            stack.shrink(1);
            return container;
         }
      }
   }
}
