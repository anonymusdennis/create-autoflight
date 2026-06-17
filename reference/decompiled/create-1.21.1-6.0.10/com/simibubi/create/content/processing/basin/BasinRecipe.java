package com.simibubi.create.content.processing.basin;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.recipe.DummyCraftingContainer;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class BasinRecipe extends StandardProcessingRecipe<RecipeInput> {
   public static boolean match(BasinBlockEntity basin, Recipe<?> recipe) {
      FilteringBehaviour filter = basin.getFilter();
      if (filter == null) {
         return false;
      } else {
         boolean filterTest = filter.test(recipe.getResultItem(basin.getLevel().registryAccess()));
         if (recipe instanceof BasinRecipe basinRecipe && basinRecipe.getRollableResults().isEmpty() && !basinRecipe.getFluidResults().isEmpty()) {
            filterTest = filter.test((FluidStack)basinRecipe.getFluidResults().get(0));
         }

         return !filterTest ? false : apply(basin, recipe, true);
      }
   }

   public static boolean apply(BasinBlockEntity basin, Recipe<?> recipe) {
      return apply(basin, recipe, false);
   }

   private static boolean apply(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
      boolean isBasinRecipe = recipe instanceof BasinRecipe;
      IItemHandler availableItems = (IItemHandler)basin.getLevel().getCapability(ItemHandler.BLOCK, basin.getBlockPos(), null);
      IFluidHandler availableFluids = (IFluidHandler)basin.getLevel().getCapability(FluidHandler.BLOCK, basin.getBlockPos(), null);
      if (availableItems != null && availableFluids != null) {
         BlazeBurnerBlock.HeatLevel heat = basin.getHeatLevel();
         if (isBasinRecipe && !((BasinRecipe)recipe).getRequiredHeat().testBlazeBurner(heat)) {
            return false;
         } else {
            List<ItemStack> recipeOutputItems = new ArrayList<>();
            List<FluidStack> recipeOutputFluids = new ArrayList<>();
            List<Ingredient> ingredients = new LinkedList<>(recipe.getIngredients());
            List<SizedFluidIngredient> fluidIngredients = (List<SizedFluidIngredient>)(isBasinRecipe
               ? ((BasinRecipe)recipe).getFluidIngredients()
               : Collections.emptyList());

            for (boolean simulate : Iterate.trueAndFalse) {
               if (!simulate && test) {
                  return true;
               }

               int[] extractedItemsFromSlot = new int[availableItems.getSlots()];
               int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];

               label109:
               for (Ingredient ingredient : ingredients) {
                  for (int slot = 0; slot < availableItems.getSlots(); slot++) {
                     if (!simulate || availableItems.getStackInSlot(slot).getCount() > extractedItemsFromSlot[slot]) {
                        ItemStack extracted = availableItems.extractItem(slot, 1, true);
                        if (ingredient.test(extracted)) {
                           if (!simulate) {
                              availableItems.extractItem(slot, 1, false);
                           }

                           extractedItemsFromSlot[slot]++;
                           continue label109;
                        }
                     }
                  }

                  return false;
               }

               boolean fluidsAffected = false;

               label129:
               for (SizedFluidIngredient fluidIngredient : fluidIngredients) {
                  int amountRequired = fluidIngredient.amount();

                  for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                     FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                     if ((!simulate || fluidStack.getAmount() > extractedFluidsFromTank[tank]) && fluidIngredient.test(fluidStack)) {
                        int drainedAmount = Math.min(amountRequired, fluidStack.getAmount());
                        if (!simulate) {
                           fluidStack.shrink(drainedAmount);
                           fluidsAffected = true;
                        }

                        amountRequired -= drainedAmount;
                        if (amountRequired == 0) {
                           extractedFluidsFromTank[tank] += drainedAmount;
                           continue label129;
                        }
                     }
                  }

                  return false;
               }

               if (fluidsAffected) {
                  basin.getBehaviour(SmartFluidTankBehaviour.INPUT).forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
                  basin.getBehaviour(SmartFluidTankBehaviour.OUTPUT).forEach(SmartFluidTankBehaviour.TankSegment::onFluidStackChanged);
               }

               if (simulate) {
                  CraftingInput remainderInput = new DummyCraftingContainer(availableItems, extractedItemsFromSlot).asCraftInput();
                  if (!(recipe instanceof BasinRecipe basinRecipe)) {
                     recipeOutputItems.add(recipe.getResultItem(basin.getLevel().registryAccess()));
                     if (recipe instanceof CraftingRecipe craftingRecipe) {
                        for (ItemStack stack : craftingRecipe.getRemainingItems(remainderInput)) {
                           if (!stack.isEmpty()) {
                              recipeOutputItems.add(stack);
                           }
                        }
                     }
                  } else {
                     recipeOutputItems.addAll(basinRecipe.rollResults(basin.getLevel().random));

                     for (FluidStack fluidStack : basinRecipe.getFluidResults()) {
                        if (!fluidStack.isEmpty()) {
                           recipeOutputFluids.add(fluidStack);
                        }
                     }

                     for (ItemStack stackx : basinRecipe.getRemainingItems(remainderInput)) {
                        if (!stackx.isEmpty()) {
                           recipeOutputItems.add(stackx);
                        }
                     }
                  }
               }

               if (!basin.acceptOutputs(recipeOutputItems, recipeOutputFluids, simulate)) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public static RecipeHolder<BasinRecipe> convertShapeless(RecipeHolder<?> recipe) {
      BasinRecipe basinRecipe = new StandardProcessingRecipe.Builder<>(BasinRecipe::new, recipe.id())
         .withItemIngredients(recipe.value().getIngredients())
         .withSingleItemOutput(recipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()))
         .build();
      return new RecipeHolder(recipe.id(), basinRecipe);
   }

   protected BasinRecipe(IRecipeTypeInfo type, ProcessingRecipeParams params) {
      super(type, params);
   }

   public BasinRecipe(ProcessingRecipeParams params) {
      this(AllRecipeTypes.BASIN, params);
   }

   @Override
   protected int getMaxInputCount() {
      return 64;
   }

   @Override
   protected int getMaxOutputCount() {
      return 4;
   }

   @Override
   protected int getMaxFluidInputCount() {
      return 2;
   }

   @Override
   protected int getMaxFluidOutputCount() {
      return 2;
   }

   @Override
   protected boolean canRequireHeat() {
      return true;
   }

   @Override
   protected boolean canSpecifyDuration() {
      return true;
   }

   public boolean matches(RecipeInput input, @NotNull Level worldIn) {
      return false;
   }
}
