package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllTags;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class DeployerApplicationRecipe extends ItemApplicationRecipe implements IAssemblyRecipe {
   public DeployerApplicationRecipe(ItemApplicationRecipeParams params) {
      super(AllRecipeTypes.DEPLOYING, params);
   }

   @Override
   protected int getMaxOutputCount() {
      return 4;
   }

   public static RecipeHolder<DeployerApplicationRecipe> convert(RecipeHolder<?> sandpaperRecipe) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(sandpaperRecipe.id().getNamespace(), sandpaperRecipe.id().getPath() + "_using_deployer");
      DeployerApplicationRecipe recipe = new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, id)
         .require((Ingredient)sandpaperRecipe.value().getIngredients().get(0))
         .require(AllTags.AllItemTags.SANDPAPER.tag)
         .output(sandpaperRecipe.value().getResultItem(Minecraft.getInstance().level.registryAccess()))
         .build();
      return new RecipeHolder(id, recipe);
   }

   @Override
   public void addAssemblyIngredients(List<Ingredient> list) {
      list.add((Ingredient)this.ingredients.get(1));
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public Component getDescriptionForAssembly() {
      ItemStack[] matchingStacks = ((Ingredient)this.ingredients.get(1)).getItems();
      return matchingStacks.length == 0
         ? Component.literal("Invalid")
         : CreateLang.translateDirect("recipe.assembly.deploying_item", Component.translatable(matchingStacks[0].getDescriptionId()).getString());
   }

   @Override
   public void addRequiredMachines(Set<ItemLike> list) {
      list.add((ItemLike)AllBlocks.DEPLOYER.get());
   }

   @Override
   public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
      return () -> SequencedAssemblySubCategory.AssemblyDeploying::new;
   }
}
