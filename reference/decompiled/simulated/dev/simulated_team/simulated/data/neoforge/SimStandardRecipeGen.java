package dev.simulated_team.simulated.data.neoforge;

import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import dev.simulated_team.simulated.Simulated;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Recipe;

public class SimStandardRecipeGen extends BaseRecipeProvider {
   GeneratedRecipe PORTABLE_ENGINE_DYEING = this.createSpecial(PortableEngineDyeingRecipe::new, "crafting", "portable_engine_dyeing");

   public SimStandardRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "simulated");
   }

   public String getName() {
      return "Simulated's Surprisingly Standard Recipes";
   }

   private GeneratedRecipe createSpecial(Function<CraftingBookCategory, Recipe<?>> builder, String recipeType, String path) {
      ResourceLocation location = Simulated.path(recipeType + "/" + path);
      return this.register(consumer -> {
         SpecialRecipeBuilder b = SpecialRecipeBuilder.special(builder);
         b.save(consumer, location.toString());
      });
   }
}
