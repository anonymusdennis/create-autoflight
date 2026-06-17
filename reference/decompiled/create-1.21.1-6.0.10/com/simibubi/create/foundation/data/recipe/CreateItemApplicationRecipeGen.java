package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.ItemApplicationRecipeGen;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.Tags.Items;

public final class CreateItemApplicationRecipeGen extends ItemApplicationRecipeGen {
   BaseRecipeProvider.GeneratedRecipe BOUND_CARDBOARD_BLOCK = this.create(
      "bound_cardboard_inworld",
      b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(AllBlocks.CARDBOARD_BLOCK.asItem()))
               .require(Items.STRINGS))
            .output(AllBlocks.BOUND_CARDBOARD_BLOCK.asStack())
   );
   BaseRecipeProvider.GeneratedRecipe ANDESITE = this.woodCasing("andesite", CreateRecipeProvider.I::andesiteAlloy, CreateRecipeProvider.I::andesiteCasing);
   BaseRecipeProvider.GeneratedRecipe COPPER = this.woodCasingTag("copper", CreateRecipeProvider.I::copper, CreateRecipeProvider.I::copperCasing);
   BaseRecipeProvider.GeneratedRecipe BRASS = this.woodCasingTag("brass", CreateRecipeProvider.I::brass, CreateRecipeProvider.I::brassCasing);
   BaseRecipeProvider.GeneratedRecipe RAILWAY = this.create(
      "railway_casing",
      b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(CreateRecipeProvider.I.brassCasing()))
               .require(CreateRecipeProvider.I.sturdySheet()))
            .output(CreateRecipeProvider.I.railwayCasing())
   );

   public CreateItemApplicationRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
