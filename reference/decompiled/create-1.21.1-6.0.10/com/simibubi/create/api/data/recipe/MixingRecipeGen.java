package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.mixer.MixingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;

public abstract class MixingRecipeGen extends StandardProcessingRecipeGen<MixingRecipe> {
   protected BaseRecipeProvider.GeneratedRecipe moddedMud(DatagenMod mod, String name) {
      String mud = name + "_mud";
      return this.create(
         mod.recipeId(mud),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Fluids.WATER, 250
                     ))
                     .require(mod, name + "_dirt"))
                  .output(mod, mud))
               .whenModLoaded(mod.getId())
      );
   }

   public MixingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.MIXING;
   }
}
