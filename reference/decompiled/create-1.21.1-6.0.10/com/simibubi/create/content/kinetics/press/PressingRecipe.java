package com.simibubi.create.content.kinetics.press;

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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@ParametersAreNonnullByDefault
public class PressingRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {
   public PressingRecipe(ProcessingRecipeParams params) {
      super(AllRecipeTypes.PRESSING, params);
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
      return 2;
   }

   @Override
   public void addAssemblyIngredients(List<Ingredient> list) {
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Component getDescriptionForAssembly() {
      return CreateLang.translateDirect("recipe.assembly.pressing");
   }

   @Override
   public void addRequiredMachines(Set<ItemLike> list) {
      list.add((ItemLike)AllBlocks.MECHANICAL_PRESS.get());
   }

   @Override
   public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
      return () -> SequencedAssemblySubCategory.AssemblyPressing::new;
   }
}
