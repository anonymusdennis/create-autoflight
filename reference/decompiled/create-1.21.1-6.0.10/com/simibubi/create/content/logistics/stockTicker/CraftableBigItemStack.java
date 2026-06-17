package com.simibubi.create.content.logistics.stockTicker;

import com.simibubi.create.content.logistics.BigItemStack;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

public class CraftableBigItemStack extends BigItemStack {
   public Recipe<?> recipe;

   public CraftableBigItemStack(ItemStack stack, Recipe<?> recipe) {
      super(stack);
      this.recipe = recipe;
   }

   public List<Ingredient> getIngredients() {
      return this.recipe.getIngredients();
   }

   public int getOutputCount(Level level) {
      return this.recipe.getResultItem(level.registryAccess()).getCount();
   }
}
