package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;

public abstract class FillingRecipeGen extends StandardProcessingRecipeGen<FillingRecipe> {
   protected BaseRecipeProvider.GeneratedRecipe moddedGrass(DatagenMod mod, String name) {
      String grass = name + "_grass_block";
      return this.create(
         mod.recipeId(grass),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Fluids.WATER, 500
                     ))
                     .require(mod, name + "_dirt"))
                  .output(mod, grass))
               .whenModLoaded(mod.getId())
      );
   }

   public FillingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.FILLING;
   }
}
