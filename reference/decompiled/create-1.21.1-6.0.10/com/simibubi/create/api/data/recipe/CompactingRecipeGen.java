package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.mixer.CompactingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;

public abstract class CompactingRecipeGen extends StandardProcessingRecipeGen<CompactingRecipe> {
   public CompactingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.COMPACTING;
   }
}
