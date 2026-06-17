package com.simibubi.create.content.kinetics.saw;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

@ParametersAreNonnullByDefault
public class CuttingRecipe extends StandardProcessingRecipe<RecipeWrapper> implements IAssemblyRecipe {
   public CuttingRecipe(ProcessingRecipeParams params) {
      super(AllRecipeTypes.CUTTING, params);
   }

   public boolean matches(RecipeWrapper inv, Level worldIn) {
      return inv.isEmpty() ? false : ((Ingredient)this.ingredients.get(0)).test(inv.getItem(0));
   }

   @Override
   protected int getMaxInputCount() {
      return 1;
   }

   @Override
   protected int getMaxOutputCount() {
      return 4;
   }

   @Override
   protected boolean canSpecifyDuration() {
      return true;
   }

   @Override
   public void addAssemblyIngredients(List<Ingredient> list) {
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Component getDescriptionForAssembly() {
      return CreateLang.translateDirect("recipe.assembly.cutting");
   }

   @Override
   public void addRequiredMachines(Set<ItemLike> list) {
      list.add((ItemLike)AllBlocks.MECHANICAL_SAW.get());
   }

   @Override
   public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
      return () -> SequencedAssemblySubCategory.AssemblyCutting::new;
   }
}
