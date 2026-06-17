package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.api.data.recipe.CrushingRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import dev.eriksonn.aeronautics.index.AeroItems;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

public class AeroCrushingRecipes extends CrushingRecipeGen {
   GeneratedRecipe END_STONE_POWDER = this.create(
      "end_stone_powder",
      b -> (Builder)((Builder)((Builder)((Builder)b.duration(250)).require(Blocks.END_STONE)).output(0.5F, Blocks.END_STONE)).output(AeroItems.ENDSTONE_POWDER)
   );

   public AeroCrushingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "aeronautics");
   }

   @NotNull
   public String getName() {
      return "Aero's Captivating Crushing Recipes";
   }
}
