package com.simibubi.create.content.kinetics.fan.processing;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

@ParametersAreNonnullByDefault
public class SplashingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
   public SplashingRecipe(ProcessingRecipeParams params) {
      super(AllRecipeTypes.SPLASHING, params);
   }

   public boolean matches(SingleRecipeInput inv, Level worldIn) {
      return inv.isEmpty() ? false : ((Ingredient)this.ingredients.get(0)).test(inv.getItem(0));
   }

   @Override
   protected int getMaxInputCount() {
      return 1;
   }

   @Override
   protected int getMaxOutputCount() {
      return 12;
   }
}
