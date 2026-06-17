package com.simibubi.create.content.fluids.transfer;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class GenericItemEmptying {
   public static boolean canItemBeEmptied(Level world, ItemStack stack) {
      if (PotionFluidHandler.isPotionItem(stack)) {
         return true;
      } else if (AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), world).isPresent()) {
         return true;
      } else {
         IFluidHandlerItem capability = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
         if (capability == null) {
            return false;
         } else {
            for (int i = 0; i < capability.getTanks(); i++) {
               if (capability.getFluidInTank(i).getAmount() > 0) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static Pair<FluidStack, ItemStack> emptyItem(Level level, ItemStack stack, boolean simulate) {
      FluidStack resultingFluid = FluidStack.EMPTY;
      ItemStack resultingItem = ItemStack.EMPTY;
      if (PotionFluidHandler.isPotionItem(stack)) {
         return PotionFluidHandler.emptyPotion(stack, simulate);
      } else {
         Optional<RecipeHolder<Recipe<SingleRecipeInput>>> recipe = AllRecipeTypes.EMPTYING.find(new SingleRecipeInput(stack), level);
         if (recipe.isPresent()) {
            EmptyingRecipe emptyingRecipe = (EmptyingRecipe)recipe.get().value();
            List<ItemStack> results = emptyingRecipe.rollResults(level.random);
            if (!simulate) {
               stack.shrink(1);
            }

            resultingItem = results.isEmpty() ? ItemStack.EMPTY : results.get(0);
            resultingFluid = emptyingRecipe.getResultingFluid();
            return Pair.of(resultingFluid, resultingItem);
         } else {
            ItemStack split = stack.copy();
            split.setCount(1);
            IFluidHandlerItem capability = (IFluidHandlerItem)split.getCapability(FluidHandler.ITEM);
            if (capability == null) {
               return Pair.of(resultingFluid, resultingItem);
            } else {
               resultingFluid = capability.drain(1000, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
               resultingItem = capability.getContainer().copy();
               if (!simulate) {
                  stack.shrink(1);
               }

               return Pair.of(resultingFluid, resultingItem);
            }
         }
      }
   }
}
