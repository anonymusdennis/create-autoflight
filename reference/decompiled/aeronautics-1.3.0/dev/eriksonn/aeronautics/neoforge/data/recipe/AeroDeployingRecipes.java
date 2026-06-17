package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.api.data.recipe.DeployingRecipeGen;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe.Builder;
import com.simibubi.create.foundation.utility.DyeHelper;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

public class AeroDeployingRecipes extends DeployingRecipeGen {
   public AeroDeployingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "aeronautics");

      for (DyeColor color : DyeColor.values()) {
         this.create(
            "deploying_envelope_" + color.getName(),
            b -> (Builder)((Builder)((Builder)b.require(DyeHelper.getWoolOfDye(color))).require(Items.STICK))
                  .output(AeroBlocks.DYED_ENVELOPE_BLOCKS.get(color), 3)
         );
      }
   }

   @NotNull
   public String getName() {
      return "Aero's Devious Deploying Recipes";
   }
}
