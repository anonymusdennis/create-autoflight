package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.DeployingRecipeGen;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;

public final class CreateDeployingRecipeGen extends DeployingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe COPPER_TILES = this.copperChain(AllBlocks.COPPER_TILES);
   BaseRecipeProvider.GeneratedRecipe COPPER_SHINGLES = this.copperChain(AllBlocks.COPPER_SHINGLES);
   BaseRecipeProvider.GeneratedRecipe COGWHEEL = this.create(
      "cogwheel",
      b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(CreateRecipeProvider.I.shaft()))
               .require(CreateRecipeProvider.I.planks()))
            .output(CreateRecipeProvider.I.cog())
   );
   BaseRecipeProvider.GeneratedRecipe LARGE_COGWHEEL = this.create(
      "large_cogwheel",
      b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(CreateRecipeProvider.I.cog()))
               .require(CreateRecipeProvider.I.planks()))
            .output(CreateRecipeProvider.I.largeCog())
   );
   BaseRecipeProvider.GeneratedRecipe COPPER_BLOCK = this.oxidizationChain(
      List.of(() -> Blocks.COPPER_BLOCK, () -> Blocks.EXPOSED_COPPER, () -> Blocks.WEATHERED_COPPER, () -> Blocks.OXIDIZED_COPPER),
      List.of(() -> Blocks.WAXED_COPPER_BLOCK, () -> Blocks.WAXED_EXPOSED_COPPER, () -> Blocks.WAXED_WEATHERED_COPPER, () -> Blocks.WAXED_OXIDIZED_COPPER)
   );
   BaseRecipeProvider.GeneratedRecipe COPPER_BULB = this.oxidizationChain(
      List.of(() -> Blocks.COPPER_BULB, () -> Blocks.EXPOSED_COPPER_BULB, () -> Blocks.WEATHERED_COPPER_BULB, () -> Blocks.OXIDIZED_COPPER_BULB),
      List.of(
         () -> Blocks.WAXED_COPPER_BULB,
         () -> Blocks.WAXED_EXPOSED_COPPER_BULB,
         () -> Blocks.WAXED_WEATHERED_COPPER_BULB,
         () -> Blocks.WAXED_OXIDIZED_COPPER_BULB
      )
   );
   BaseRecipeProvider.GeneratedRecipe CHISELED_COPPER = this.oxidizationChain(
      List.of(() -> Blocks.CHISELED_COPPER, () -> Blocks.EXPOSED_CHISELED_COPPER, () -> Blocks.WEATHERED_CHISELED_COPPER, () -> Blocks.OXIDIZED_CHISELED_COPPER),
      List.of(
         () -> Blocks.WAXED_CHISELED_COPPER,
         () -> Blocks.WAXED_EXPOSED_CHISELED_COPPER,
         () -> Blocks.WAXED_WEATHERED_CHISELED_COPPER,
         () -> Blocks.WAXED_OXIDIZED_CHISELED_COPPER
      )
   );
   BaseRecipeProvider.GeneratedRecipe COPPER_GRATE = this.oxidizationChain(
      List.of(() -> Blocks.COPPER_GRATE, () -> Blocks.EXPOSED_COPPER_GRATE, () -> Blocks.WEATHERED_COPPER_GRATE, () -> Blocks.OXIDIZED_COPPER_GRATE),
      List.of(
         () -> Blocks.WAXED_COPPER_GRATE,
         () -> Blocks.WAXED_EXPOSED_COPPER_GRATE,
         () -> Blocks.WAXED_WEATHERED_COPPER_GRATE,
         () -> Blocks.WAXED_OXIDIZED_COPPER_GRATE
      )
   );
   BaseRecipeProvider.GeneratedRecipe COPPER_DOOR = this.oxidizationChain(
      List.of(() -> Blocks.COPPER_DOOR, () -> Blocks.EXPOSED_COPPER_DOOR, () -> Blocks.WEATHERED_COPPER_DOOR, () -> Blocks.OXIDIZED_COPPER_DOOR),
      List.of(
         () -> Blocks.WAXED_COPPER_DOOR,
         () -> Blocks.WAXED_EXPOSED_COPPER_DOOR,
         () -> Blocks.WAXED_WEATHERED_COPPER_DOOR,
         () -> Blocks.WAXED_OXIDIZED_COPPER_DOOR
      )
   );
   BaseRecipeProvider.GeneratedRecipe COPPER_TRAPDOOR = this.oxidizationChain(
      List.of(() -> Blocks.COPPER_TRAPDOOR, () -> Blocks.EXPOSED_COPPER_TRAPDOOR, () -> Blocks.WEATHERED_COPPER_TRAPDOOR, () -> Blocks.OXIDIZED_COPPER_TRAPDOOR),
      List.of(
         () -> Blocks.WAXED_COPPER_TRAPDOOR,
         () -> Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR,
         () -> Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR,
         () -> Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR
      )
   );
   BaseRecipeProvider.GeneratedRecipe CUT_COPPER = this.oxidizationChain(
      List.of(() -> Blocks.CUT_COPPER, () -> Blocks.EXPOSED_CUT_COPPER, () -> Blocks.WEATHERED_CUT_COPPER, () -> Blocks.OXIDIZED_CUT_COPPER),
      List.of(
         () -> Blocks.WAXED_CUT_COPPER, () -> Blocks.WAXED_EXPOSED_CUT_COPPER, () -> Blocks.WAXED_WEATHERED_CUT_COPPER, () -> Blocks.WAXED_OXIDIZED_CUT_COPPER
      )
   );
   BaseRecipeProvider.GeneratedRecipe CUT_COPPER_STAIRS = this.oxidizationChain(
      List.of(
         () -> Blocks.CUT_COPPER_STAIRS,
         () -> Blocks.EXPOSED_CUT_COPPER_STAIRS,
         () -> Blocks.WEATHERED_CUT_COPPER_STAIRS,
         () -> Blocks.OXIDIZED_CUT_COPPER_STAIRS
      ),
      List.of(
         () -> Blocks.WAXED_CUT_COPPER_STAIRS,
         () -> Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS,
         () -> Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS,
         () -> Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS
      )
   );
   BaseRecipeProvider.GeneratedRecipe CUT_COPPER_SLAB = this.oxidizationChain(
      List.of(() -> Blocks.CUT_COPPER_SLAB, () -> Blocks.EXPOSED_CUT_COPPER_SLAB, () -> Blocks.WEATHERED_CUT_COPPER_SLAB, () -> Blocks.OXIDIZED_CUT_COPPER_SLAB),
      List.of(
         () -> Blocks.WAXED_CUT_COPPER_SLAB,
         () -> Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB,
         () -> Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
         () -> Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB
      )
   );

   public CreateDeployingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
