package com.simibubi.create.api.data.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

public abstract class StandardProcessingRecipeGen<R extends StandardProcessingRecipe<?>>
   extends ProcessingRecipeGen<ProcessingRecipeParams, R, StandardProcessingRecipe.Builder<R>> {
   public StandardProcessingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected StandardProcessingRecipe.Serializer<R> getSerializer() {
      return this.getRecipeType().getSerializer();
   }

   protected StandardProcessingRecipe.Builder<R> getBuilder(ResourceLocation id) {
      return new StandardProcessingRecipe.Builder<>(this.getSerializer().factory(), id);
   }
}
