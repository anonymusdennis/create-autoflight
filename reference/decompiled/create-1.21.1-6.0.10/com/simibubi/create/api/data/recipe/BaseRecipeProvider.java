package com.simibubi.create.api.data.recipe;

import com.simibubi.create.Create;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;

public abstract class BaseRecipeProvider extends RecipeProvider {
   protected final String modid;
   protected final List<BaseRecipeProvider.GeneratedRecipe> all = new ArrayList<>();

   public BaseRecipeProvider(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries);
      this.modid = defaultNamespace;
   }

   protected ResourceLocation asResource(String path) {
      return ResourceLocation.fromNamespaceAndPath(this.modid, path);
   }

   protected BaseRecipeProvider.GeneratedRecipe register(BaseRecipeProvider.GeneratedRecipe recipe) {
      this.all.add(recipe);
      return recipe;
   }

   public void buildRecipes(RecipeOutput recipeOutput) {
      this.all.forEach(c -> c.register(recipeOutput));
      Create.LOGGER.info("{} registered {} recipe{}", new Object[]{this.getName(), this.all.size(), this.all.size() == 1 ? "" : "s"});
   }

   @FunctionalInterface
   public interface GeneratedRecipe {
      void register(RecipeOutput var1);
   }
}
