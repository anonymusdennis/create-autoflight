package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags.Items;

public final class CreateMechanicalCraftingRecipeGen extends MechanicalCraftingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe CRUSHING_WHEEL = this.create(AllBlocks.CRUSHING_WHEEL::get)
      .returns(2)
      .recipe(
         b -> b.key('P', Ingredient.of(ItemTags.PLANKS))
               .key('S', Ingredient.of(CreateRecipeProvider.I.stone()))
               .key('A', CreateRecipeProvider.I.andesiteAlloy())
               .patternLine(" AAA ")
               .patternLine("AAPAA")
               .patternLine("APSPA")
               .patternLine("AAPAA")
               .patternLine(" AAA ")
               .disallowMirrored()
      );
   BaseRecipeProvider.GeneratedRecipe WAND_OF_SYMMETRY = this.create(AllItems.WAND_OF_SYMMETRY::get)
      .recipe(
         b -> b.key('E', Ingredient.of(Items.ENDER_PEARLS))
               .key('G', Ingredient.of(Items.GLASS_BLOCKS))
               .key('P', CreateRecipeProvider.I.precisionMechanism())
               .key('O', Ingredient.of(Items.OBSIDIANS))
               .key('B', Ingredient.of(CreateRecipeProvider.I.brass()))
               .patternLine(" G ")
               .patternLine("GEG")
               .patternLine(" P ")
               .patternLine(" B ")
               .patternLine(" O ")
      );
   BaseRecipeProvider.GeneratedRecipe EXTENDO_GRIP = this.create(AllItems.EXTENDO_GRIP::get)
      .returns(1)
      .recipe(
         b -> b.key('L', Ingredient.of(CreateRecipeProvider.I.brass()))
               .key('R', CreateRecipeProvider.I.precisionMechanism())
               .key('H', (ItemLike)AllItems.BRASS_HAND.get())
               .key('S', Ingredient.of(Items.RODS_WOODEN))
               .patternLine(" L ")
               .patternLine(" R ")
               .patternLine("SSS")
               .patternLine("SSS")
               .patternLine(" H ")
               .disallowMirrored()
      );
   BaseRecipeProvider.GeneratedRecipe POTATO_CANNON = this.create(AllItems.POTATO_CANNON::get)
      .returns(1)
      .recipe(
         b -> b.key('L', CreateRecipeProvider.I.andesiteAlloy())
               .key('R', CreateRecipeProvider.I.precisionMechanism())
               .key('S', (ItemLike)AllBlocks.FLUID_PIPE.get())
               .key('C', Ingredient.of(CreateRecipeProvider.I.copper()))
               .patternLine("LRSSS")
               .patternLine("CC   ")
      );

   public CreateMechanicalCraftingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
