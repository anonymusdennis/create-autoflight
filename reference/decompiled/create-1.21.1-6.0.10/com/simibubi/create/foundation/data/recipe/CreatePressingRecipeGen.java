package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.PressingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreatePressingRecipeGen extends PressingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe SUGAR_CANE = this.create(() -> Items.SUGAR_CANE, b -> (StandardProcessingRecipe.Builder)b.output(Items.PAPER));
   BaseRecipeProvider.GeneratedRecipe PATH = this.create(
      "path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  Ingredient.of(new ItemLike[]{Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.MYCELIUM, Items.PODZOL})
               ))
               .output(Items.DIRT_PATH))
            .whenModMissing(Mods.ENV.getId())
   );
   BaseRecipeProvider.GeneratedRecipe GRASS_PATH = this.create(
      "path_from_grass", b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Items.GRASS_BLOCK)).output(Items.DIRT_PATH)
   );
   BaseRecipeProvider.GeneratedRecipe IRON = this.create(
      "iron_ingot",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(CreateRecipeProvider.I.iron()))
            .output((ItemLike)AllItems.IRON_SHEET.get())
   );
   BaseRecipeProvider.GeneratedRecipe GOLD = this.create(
      "gold_ingot",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(CreateRecipeProvider.I.gold()))
            .output((ItemLike)AllItems.GOLDEN_SHEET.get())
   );
   BaseRecipeProvider.GeneratedRecipe COPPER = this.create(
      "copper_ingot",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(CreateRecipeProvider.I.copper()))
            .output((ItemLike)AllItems.COPPER_SHEET.get())
   );
   BaseRecipeProvider.GeneratedRecipe BRASS = this.create(
      "brass_ingot",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(CreateRecipeProvider.I.brass()))
            .output((ItemLike)AllItems.BRASS_SHEET.get())
   );
   BaseRecipeProvider.GeneratedRecipe CARDBOARD = this.create(
      "cardboard",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(AllItems.PULP)).output((ItemLike)AllItems.CARDBOARD.get())
   );
   BaseRecipeProvider.GeneratedRecipe ATM = this.moddedPaths(Mods.ATM, new String[]{"crustose"});
   BaseRecipeProvider.GeneratedRecipe BEF = this.moddedPaths(
      Mods.BEF,
      new String[]{"amber_moss", "cave_moss", "chorus_nylium", "crystal_moss", "end_moss", "end_mycelium", "jungle_moss", "pink_moss", "shadow_grass"}
   );
   BaseRecipeProvider.GeneratedRecipe ENV_MYCELIUM = this.create(
      "compat/environmental/mycelium_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.MYCELIUM))
               .output(Mods.ENV, "mycelium_path"))
            .whenModLoaded(Mods.ENV.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ENV_PODZOL = this.create(
      "compat/environmental/podzol_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.PODZOL))
               .output(Mods.ENV, "podzol_path"))
            .whenModLoaded(Mods.ENV.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ENV_DIRT = this.create(
      "compat/environmental/dirt_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  Ingredient.of(new ItemLike[]{Items.DIRT, Items.COARSE_DIRT, Items.ROOTED_DIRT})
               ))
               .output(Mods.ENV, "dirt_path"))
            .whenModLoaded(Mods.ENV.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BWG = this.moddedPaths(Mods.BWG, new String[]{"lush_dirt", "sandy_dirt"});
   BaseRecipeProvider.GeneratedRecipe BWG_GRASS_PATH = this.create(
      Mods.BWG.recipeId("lush_grass_path"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.BWG, "lush_grass_block"))
               .output(Mods.BWG, "lush_dirt_path"))
            .whenModLoaded(Mods.BWG.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IX_CRIMSON_PATH = this.create(
      Mods.IX.recipeId("crimson_nylium_path"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.CRIMSON_NYLIUM))
               .output(Mods.IX, "crimson_nylium_path"))
            .whenModLoaded(Mods.IX.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IX_WARPED_PATH = this.create(
      Mods.IX.recipeId("warped_nylium_path"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.WARPED_NYLIUM))
               .output(Mods.IX, "warped_nylium_path"))
            .whenModLoaded(Mods.IX.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IX_SOUL_PATH = this.create(
      Mods.IX.recipeId("soul_soil_path"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.SOUL_SOIL))
               .output(Mods.IX, "soul_soil_path"))
            .whenModLoaded(Mods.IX.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AET_DIRT_PATH = this.create(
      "aether_dirt_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.AET, "aether_dirt"))
               .output(Mods.AET, "aether_dirt_path"))
            .whenModLoaded(Mods.AET.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AET_DIRT_PATH_GRASS = this.create(
      "aether_dirt_path_from_grass",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.AET, "aether_grass_block"))
               .output(Mods.AET, "aether_dirt_path"))
            .whenModLoaded(Mods.AET.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_PEAT_PATH = this.create(
      "peat_dirt_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.RU, "peat_dirt"))
               .output(Mods.RU, "peat_dirt_path"))
            .whenModLoaded(Mods.RU.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_PEAT_PATH_GRASS = this.create(
      "peat_dirt_path_from_grass",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.RU, "peat_grass_block"))
               .output(Mods.RU, "peat_dirt_path"))
            .whenModLoaded(Mods.RU.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_SILT_PATH = this.create(
      "silt_dirt_path",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.RU, "silt_dirt"))
               .output(Mods.RU, "silt_dirt_path"))
            .whenModLoaded(Mods.RU.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_SILT_PATH_GRASS = this.create(
      "silt_dirt_path_from_grass",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.RU, "silt_grass_block"))
               .output(Mods.RU, "silt_dirt_path"))
            .whenModLoaded(Mods.RU.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IE_PLATES = this.iePlates();
   BaseRecipeProvider.GeneratedRecipe VMP_CURSED_PATH = this.moddedPaths(Mods.VMP, new String[]{"cursed_earth"});
   BaseRecipeProvider.GeneratedRecipe VMP_CURSED_PATH_GRASS = this.create(
      "cursed_earth_path_from_grass",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Mods.VMP, "cursed_grass"))
               .output(Mods.VMP, "cursed_earth_path"))
            .whenModLoaded(Mods.VMP.getId())
   );

   public CreatePressingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }

   private BaseRecipeProvider.GeneratedRecipe iePlates() {
      for (CommonMetal metal : CommonMetal.of(Mods.IE)) {
         this.create(
            Mods.IE.recipeId("plate_" + metal),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(metal.ingots))
                     .output(Mods.IE, "plate_" + metal))
                  .whenModLoaded(Mods.IE.getId())
         );
      }

      return null;
   }
}
