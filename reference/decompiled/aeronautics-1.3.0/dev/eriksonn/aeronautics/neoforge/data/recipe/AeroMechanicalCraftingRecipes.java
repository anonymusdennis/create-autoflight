package dev.eriksonn.aeronautics.neoforge.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider.GeneratedRecipe;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class AeroMechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
   private final GeneratedRecipe MOUNTED_POTATO_CANNON = this.create(AeroBlocks.MOUNTED_POTATO_CANNON::get)
      .returns(1)
      .recipe(
         b -> b.patternLine("SR  ")
               .patternLine("KCPP")
               .patternLine("SR  ")
               .key('S', AllItems.COPPER_SHEET)
               .key('R', Items.REDSTONE)
               .key('K', Blocks.DRIED_KELP_BLOCK)
               .key('C', AllBlocks.COGWHEEL)
               .key('P', AllBlocks.FLUID_PIPE)
      );

   public AeroMechanicalCraftingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "aeronautics");
   }

   public String getName() {
      return "Aero's Mischievous Mechanical Crafting Recipes";
   }
}
