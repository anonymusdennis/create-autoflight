package com.simibubi.create.content.kinetics.crusher;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;

@ParametersAreNonnullByDefault
public class CrushingRecipe extends AbstractCrushingRecipe {
   public CrushingRecipe(ProcessingRecipeParams params) {
      super(AllRecipeTypes.CRUSHING, params);
   }

   public boolean matches(RecipeInput inv, Level worldIn) {
      return inv.isEmpty() ? false : ((Ingredient)this.ingredients.get(0)).test(inv.getItem(0));
   }

   @Override
   protected int getMaxOutputCount() {
      return 7;
   }
}
