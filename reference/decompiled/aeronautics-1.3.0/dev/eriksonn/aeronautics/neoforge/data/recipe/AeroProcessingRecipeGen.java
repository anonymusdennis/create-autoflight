package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

public class AeroProcessingRecipeGen {
   protected static List<BaseRecipeProvider> GENERATORS = new ArrayList<>();

   public static DataProvider registerAll(PackOutput output, CompletableFuture<Provider> lookupProvider) {
      GENERATORS.add(new AeroMixingRecipes(output, lookupProvider));
      GENERATORS.add(new AeroCrushingRecipes(output, lookupProvider));
      GENERATORS.add(new AeroMechanicalCraftingRecipes(output, lookupProvider));
      GENERATORS.add(new AeroWashingRecipes(output, lookupProvider));
      GENERATORS.add(new AeroDeployingRecipes(output, lookupProvider));
      return new DataProvider() {
         public CompletableFuture<?> run(CachedOutput cachedOutput) {
            return CompletableFuture.allOf(AeroProcessingRecipeGen.GENERATORS.stream().map(gen -> gen.run(cachedOutput)).toArray(CompletableFuture[]::new));
         }

         public String getName() {
            return "Aero's Perfect Processing Recipes";
         }
      };
   }
}
