package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.neoforge.index.AeroFluidsNeoForge;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.Tags.Fluids;
import org.jetbrains.annotations.NotNull;

public class AeroMixingRecipes extends MixingRecipeGen {
   GeneratedRecipe LEVITITE_BLEND = this.create(
      "levitite_blend",
      b -> (Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)b.require(AeroItems.ENDSTONE_POWDER))
                                 .require(AeroItems.ENDSTONE_POWDER))
                              .require(AeroItems.ENDSTONE_POWDER))
                           .require(AeroItems.ENDSTONE_POWDER))
                        .require(AllItems.ZINC_NUGGET))
                     .require(AllItems.ZINC_NUGGET))
                  .require(Fluids.WATER, 500))
               .output((Fluid)AeroFluidsNeoForge.LEVITITE_BLEND.get(), 500))
            .requiresHeat(HeatCondition.HEATED)
   );

   public AeroMixingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "aeronautics");
   }

   @NotNull
   public String getName() {
      return "Aero's Miraculous Mixing Recipes";
   }
}
