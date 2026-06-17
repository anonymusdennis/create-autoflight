package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

public class AeroWashingRecipes extends WashingRecipeGen {
   GeneratedRecipe ENVELOPE_WASHING = this.create(
      "envelope_washing", b -> (Builder)((Builder)b.require(AeroTags.ItemTags.SHAFTLESS_ENVELOPE)).output(AeroBlocks.WHITE_ENVELOPE_BLOCK.asItem())
   );

   public AeroWashingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "aeronautics");
   }

   @NotNull
   public String getName() {
      return "Aero's Whimsical Washing Recipes";
   }
}
