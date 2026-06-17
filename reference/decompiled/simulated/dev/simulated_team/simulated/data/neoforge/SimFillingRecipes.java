package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.FillingRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import dev.simulated_team.simulated.index.SimItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags.Fluids;

public class SimFillingRecipes extends FillingRecipeGen {
   private final GeneratedRecipe HONEY_GLUE = this.create(
      "honey_glue", b -> (Builder)((Builder)((Builder)b.require(Fluids.HONEY, 500)).require(AllItems.IRON_SHEET)).output(SimItems.HONEY_GLUE)
   );

   public SimFillingRecipes(PackOutput output, CompletableFuture<Provider> lookupProvider) {
      super(output, lookupProvider, "simulated");
   }

   public String getName() {
      return "Simulated's Fantastic Filling Recipes";
   }
}
