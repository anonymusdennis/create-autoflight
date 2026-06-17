package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public abstract class SimProcessingRecipeGen extends BaseRecipeProvider {
   protected static final List<BaseRecipeProvider> GENERATORS = new ArrayList<>();

   public static DataProvider registerAll(PackOutput output, CompletableFuture<Provider> lookupProvider) {
      GENERATORS.add(new SimFillingRecipes(output, lookupProvider));
      GENERATORS.add(new SimMechanicalCraftingRecipes(output, lookupProvider));
      GENERATORS.add(new SimSequencedAssemblyRecipes(output, lookupProvider));
      GENERATORS.add(new SimStandardRecipeGen(output, lookupProvider));
      return new DataProvider() {
         public CompletableFuture<?> run(CachedOutput arg) {
            return CompletableFuture.allOf(SimProcessingRecipeGen.GENERATORS.stream().map(gen -> gen.run(arg)).toArray(CompletableFuture[]::new));
         }

         public String getName() {
            return "Simulated's Peculiar Processing Recipes";
         }
      };
   }

   public SimProcessingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "simulated");
   }
}
