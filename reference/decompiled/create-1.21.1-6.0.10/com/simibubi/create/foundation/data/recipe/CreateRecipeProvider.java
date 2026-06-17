package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.ProcessingRecipeGen;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags.Items;

public final class CreateRecipeProvider extends RecipeProvider {
   static final List<ProcessingRecipeGen<?, ?, ?>> GENERATORS = new ArrayList<>();
   static final int BUCKET = 1000;
   static final int BOTTLE = 250;

   public CreateRecipeProvider(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries);
   }

   protected void buildRecipes(RecipeOutput recipeOutput) {
   }

   public static void registerAllProcessing(DataGenerator gen, PackOutput output, CompletableFuture<Provider> registries) {
      GENERATORS.add(new CreateCrushingRecipeGen(output, registries));
      GENERATORS.add(new CreateMillingRecipeGen(output, registries));
      GENERATORS.add(new CreateCuttingRecipeGen(output, registries));
      GENERATORS.add(new CreateWashingRecipeGen(output, registries));
      GENERATORS.add(new CreatePolishingRecipeGen(output, registries));
      GENERATORS.add(new CreateDeployingRecipeGen(output, registries));
      GENERATORS.add(new CreateMixingRecipeGen(output, registries));
      GENERATORS.add(new CreateCompactingRecipeGen(output, registries));
      GENERATORS.add(new CreatePressingRecipeGen(output, registries));
      GENERATORS.add(new CreateFillingRecipeGen(output, registries));
      GENERATORS.add(new CreateEmptyingRecipeGen(output, registries));
      GENERATORS.add(new CreateHauntingRecipeGen(output, registries));
      GENERATORS.add(new CreateItemApplicationRecipeGen(output, registries));
      gen.addProvider(true, new DataProvider() {
         public String getName() {
            return "Create's Processing Recipes";
         }

         public CompletableFuture<?> run(CachedOutput dc) {
            return CompletableFuture.allOf(CreateRecipeProvider.GENERATORS.stream().map(gen -> gen.run(dc)).toArray(CompletableFuture[]::new));
         }
      });
   }

   protected static class I {
      static TagKey<Item> redstone() {
         return Items.DUSTS_REDSTONE;
      }

      static TagKey<Item> planks() {
         return ItemTags.PLANKS;
      }

      static TagKey<Item> woodSlab() {
         return ItemTags.WOODEN_SLABS;
      }

      static TagKey<Item> gold() {
         return Items.INGOTS_GOLD;
      }

      static TagKey<Item> goldSheet() {
         return CommonMetal.GOLD.plates;
      }

      static TagKey<Item> stone() {
         return Items.STONES;
      }

      static ItemLike andesiteAlloy() {
         return (ItemLike)AllItems.ANDESITE_ALLOY.get();
      }

      static ItemLike shaft() {
         return (ItemLike)AllBlocks.SHAFT.get();
      }

      static ItemLike cog() {
         return (ItemLike)AllBlocks.COGWHEEL.get();
      }

      static ItemLike largeCog() {
         return (ItemLike)AllBlocks.LARGE_COGWHEEL.get();
      }

      static ItemLike andesiteCasing() {
         return (ItemLike)AllBlocks.ANDESITE_CASING.get();
      }

      static ItemLike vault() {
         return (ItemLike)AllBlocks.ITEM_VAULT.get();
      }

      static ItemLike stockLink() {
         return (ItemLike)AllBlocks.STOCK_LINK.get();
      }

      static TagKey<Item> brass() {
         return CommonMetal.BRASS.ingots;
      }

      static TagKey<Item> brassSheet() {
         return CommonMetal.BRASS.plates;
      }

      static TagKey<Item> iron() {
         return Items.INGOTS_IRON;
      }

      static TagKey<Item> ironNugget() {
         return Items.NUGGETS_IRON;
      }

      static TagKey<Item> zinc() {
         return CommonMetal.ZINC.ingots;
      }

      static TagKey<Item> ironSheet() {
         return CommonMetal.IRON.plates;
      }

      static TagKey<Item> sturdySheet() {
         return AllTags.AllItemTags.OBSIDIAN_PLATES.tag;
      }

      static ItemLike brassCasing() {
         return (ItemLike)AllBlocks.BRASS_CASING.get();
      }

      static ItemLike cardboard() {
         return (ItemLike)AllItems.CARDBOARD.get();
      }

      static ItemLike railwayCasing() {
         return (ItemLike)AllBlocks.RAILWAY_CASING.get();
      }

      static ItemLike electronTube() {
         return (ItemLike)AllItems.ELECTRON_TUBE.get();
      }

      static ItemLike precisionMechanism() {
         return (ItemLike)AllItems.PRECISION_MECHANISM.get();
      }

      static TagKey<Item> brassBlock() {
         return CommonMetal.BRASS.storageBlocks.items();
      }

      static TagKey<Item> zincBlock() {
         return CommonMetal.ZINC.storageBlocks.items();
      }

      static TagKey<Item> wheatFlour() {
         return AllTags.AllItemTags.WHEAT_FLOURS.tag;
      }

      static TagKey<Item> copper() {
         return Items.INGOTS_COPPER;
      }

      static TagKey<Item> copperNugget() {
         return CommonMetal.COPPER.nuggets;
      }

      static TagKey<Item> copperBlock() {
         return Items.STORAGE_BLOCKS_COPPER;
      }

      static TagKey<Item> copperSheet() {
         return CommonMetal.COPPER.plates;
      }

      static TagKey<Item> brassNugget() {
         return CommonMetal.BRASS.nuggets;
      }

      static TagKey<Item> zincNugget() {
         return CommonMetal.ZINC.nuggets;
      }

      static ItemLike copperCasing() {
         return (ItemLike)AllBlocks.COPPER_CASING.get();
      }

      static ItemLike refinedRadiance() {
         return (ItemLike)AllItems.REFINED_RADIANCE.get();
      }

      static ItemLike shadowSteel() {
         return (ItemLike)AllItems.SHADOW_STEEL.get();
      }

      static Ingredient netherite() {
         return Ingredient.of(Items.INGOTS_NETHERITE);
      }
   }
}
