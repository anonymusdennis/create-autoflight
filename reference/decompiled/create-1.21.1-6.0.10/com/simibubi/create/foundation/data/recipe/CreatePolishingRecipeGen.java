package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.PolishingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;

public final class CreatePolishingRecipeGen extends PolishingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe ROSE_QUARTZ = this.create(
      AllItems.ROSE_QUARTZ::get, b -> (StandardProcessingRecipe.Builder)b.output((ItemLike)AllItems.POLISHED_ROSE_QUARTZ.get())
   );

   public CreatePolishingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
