package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.MillingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreateMillingRecipeGen extends MillingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe GRANITE = this.create(
      () -> Blocks.GRANITE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output(Blocks.RED_SAND)
   );
   BaseRecipeProvider.GeneratedRecipe WOOL = this.create(
      "wool",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).require(ItemTags.WOOL))
            .output(Items.STRING)
   );
   BaseRecipeProvider.GeneratedRecipe CLAY = this.create(
      () -> Blocks.CLAY, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.CLAY_BALL, 4)
   );
   BaseRecipeProvider.GeneratedRecipe CALCITE = this.create(
      () -> Items.CALCITE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(250)).output(0.75F, Items.BONE_MEAL, 1)
   );
   BaseRecipeProvider.GeneratedRecipe DRIPSTONE = this.create(
      () -> Items.DRIPSTONE_BLOCK, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(250)).output(Items.CLAY_BALL, 1)
   );
   BaseRecipeProvider.GeneratedRecipe TERRACOTTA = this.create(
      () -> Blocks.TERRACOTTA, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output(Blocks.RED_SAND)
   );
   BaseRecipeProvider.GeneratedRecipe ANDESITE = this.create(
      () -> Blocks.ANDESITE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output(Blocks.COBBLESTONE)
   );
   BaseRecipeProvider.GeneratedRecipe COBBLESTONE = this.create(
      () -> Blocks.COBBLESTONE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(250)).output(Blocks.GRAVEL)
   );
   BaseRecipeProvider.GeneratedRecipe GRAVEL = this.create(
      () -> Blocks.GRAVEL, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(250)).output(Items.FLINT)
   );
   BaseRecipeProvider.GeneratedRecipe SANDSTONE = this.create(
      () -> Blocks.SANDSTONE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(150)).output(Blocks.SAND)
   );
   BaseRecipeProvider.GeneratedRecipe WHEAT = this.create(
      () -> Items.WHEAT,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     150
                  ))
                  .output((ItemLike)AllItems.WHEAT_FLOUR.get()))
               .output(0.25F, (ItemLike)AllItems.WHEAT_FLOUR.get(), 2))
            .output(0.25F, Items.WHEAT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe BONE = this.create(
      () -> Items.BONE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .output(Items.BONE_MEAL, 3))
               .output(0.25F, Items.WHITE_DYE, 1))
            .output(0.25F, Items.BONE_MEAL, 3)
   );
   BaseRecipeProvider.GeneratedRecipe CACTUS = this.create(
      () -> Blocks.CACTUS,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.GREEN_DYE, 2))
            .output(0.1F, Items.GREEN_DYE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe SEA_PICKLE = this.create(
      () -> Blocks.SEA_PICKLE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.LIME_DYE, 2))
            .output(0.1F, Items.GREEN_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe BONE_MEAL = this.create(
      () -> Items.BONE_MEAL,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(70)).output(Items.WHITE_DYE, 2))
            .output(0.1F, Items.LIGHT_GRAY_DYE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe COCOA_BEANS = this.create(
      () -> Items.COCOA_BEANS,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(70)).output(Items.BROWN_DYE, 2))
            .output(0.1F, Items.BROWN_DYE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe SADDLE = this.create(
      () -> Items.SADDLE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output(Items.LEATHER, 2))
            .output(0.5F, Items.LEATHER, 2)
   );
   BaseRecipeProvider.GeneratedRecipe SUGAR_CANE = this.create(
      () -> Items.SUGAR_CANE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.SUGAR, 2))
            .output(0.1F, Items.SUGAR)
   );
   BaseRecipeProvider.GeneratedRecipe BEETROOT = this.create(
      () -> Items.BEETROOT,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(70)).output(Items.RED_DYE, 2))
            .output(0.1F, Items.BEETROOT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe INK_SAC = this.create(
      () -> Items.INK_SAC,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).output(Items.BLACK_DYE, 2))
            .output(0.1F, Items.GRAY_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe CHARCOAL = this.create(
      () -> Items.CHARCOAL,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).output(Items.BLACK_DYE, 1))
            .output(0.1F, Items.GRAY_DYE, 2)
   );
   BaseRecipeProvider.GeneratedRecipe COAL = this.create(
      () -> Items.COAL,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).output(Items.BLACK_DYE, 2))
            .output(0.1F, Items.GRAY_DYE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe LAPIS_LAZULI = this.create(
      () -> Items.LAPIS_LAZULI,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).output(Items.BLUE_DYE, 2))
            .output(0.1F, Items.BLUE_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe AZURE_BLUET = this.create(
      () -> Blocks.AZURE_BLUET,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50))
               .output(Items.LIGHT_GRAY_DYE, 2))
            .output(0.1F, Items.WHITE_DYE, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BLUE_ORCHID = this.create(
      () -> Blocks.BLUE_ORCHID,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50))
               .output(Items.LIGHT_BLUE_DYE, 2))
            .output(0.05F, Items.LIGHT_GRAY_DYE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe FERN = this.create(
      () -> Blocks.FERN,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.GREEN_DYE))
            .output(0.1F, Items.WHEAT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe LARGE_FERN = this.create(
      () -> Blocks.LARGE_FERN,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     50
                  ))
                  .output(Items.GREEN_DYE, 2))
               .output(0.5F, Items.GREEN_DYE))
            .output(0.1F, Items.WHEAT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe LILAC = this.create(
      () -> Blocks.LILAC,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .output(Items.MAGENTA_DYE, 3))
               .output(0.25F, Items.MAGENTA_DYE))
            .output(0.25F, Items.PURPLE_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe PEONY = this.create(
      () -> Blocks.PEONY,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .output(Items.PINK_DYE, 3))
               .output(0.25F, Items.MAGENTA_DYE))
            .output(0.25F, Items.PINK_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe ALLIUM = this.create(
      () -> Blocks.ALLIUM,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     50
                  ))
                  .output(Items.MAGENTA_DYE, 2))
               .output(0.1F, Items.PURPLE_DYE, 2))
            .output(0.1F, Items.PINK_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe LILY_OF_THE_VALLEY = this.create(
      () -> Blocks.LILY_OF_THE_VALLEY,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     50
                  ))
                  .output(Items.WHITE_DYE, 2))
               .output(0.1F, Items.LIME_DYE))
            .output(0.1F, Items.WHITE_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe ROSE_BUSH = this.create(
      () -> Blocks.ROSE_BUSH,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     50
                  ))
                  .output(Items.RED_DYE, 3))
               .output(0.05F, Items.GREEN_DYE, 2))
            .output(0.25F, Items.RED_DYE, 2)
   );
   BaseRecipeProvider.GeneratedRecipe SUNFLOWER = this.create(
      () -> Blocks.SUNFLOWER,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .output(Items.YELLOW_DYE, 3))
               .output(0.25F, Items.ORANGE_DYE))
            .output(0.25F, Items.YELLOW_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe OXEYE_DAISY = this.create(
      () -> Blocks.OXEYE_DAISY,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     50
                  ))
                  .output(Items.LIGHT_GRAY_DYE, 2))
               .output(0.2F, Items.WHITE_DYE))
            .output(0.05F, Items.YELLOW_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe POPPY = this.create(
      () -> Blocks.POPPY,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.RED_DYE, 2))
            .output(0.05F, Items.GREEN_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe DANDELION = this.create(
      () -> Blocks.DANDELION,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.YELLOW_DYE, 2))
            .output(0.05F, Items.YELLOW_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe CORNFLOWER = this.create(
      () -> Blocks.CORNFLOWER, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.BLUE_DYE, 2)
   );
   BaseRecipeProvider.GeneratedRecipe WITHER_ROSE = this.create(
      () -> Blocks.WITHER_ROSE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.BLACK_DYE, 2))
            .output(0.1F, Items.BLACK_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe ORANGE_TULIP = this.create(
      () -> Blocks.ORANGE_TULIP,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.ORANGE_DYE, 2))
            .output(0.1F, Items.LIME_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe RED_TULIP = this.create(
      () -> Blocks.RED_TULIP,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.RED_DYE, 2))
            .output(0.1F, Items.LIME_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe WHITE_TULIP = this.create(
      () -> Blocks.WHITE_TULIP,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.WHITE_DYE, 2))
            .output(0.1F, Items.LIME_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe PINK_TULIP = this.create(
      () -> Blocks.PINK_TULIP,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.PINK_DYE, 2))
            .output(0.1F, Items.LIME_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe PINK_PETALS = this.create(
      () -> Blocks.PINK_PETALS,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.PINK_DYE, 2))
            .output(0.1F, Items.LIME_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe PITCHER_PLANT = this.create(
      () -> Blocks.PITCHER_PLANT,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.CYAN_DYE, 4))
            .output(0.1F, Items.PURPLE_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe TORCHFLOWER = this.create(
      () -> Blocks.TORCHFLOWER,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(Items.ORANGE_DYE, 2))
            .output(0.1F, Items.GREEN_DYE)
   );
   BaseRecipeProvider.GeneratedRecipe TALL_GRASS = this.create(
      () -> Blocks.TALL_GRASS, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100)).output(0.5F, Items.WHEAT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe GRASS = this.create(
      () -> Blocks.SHORT_GRASS, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(50)).output(0.25F, Items.WHEAT_SEEDS)
   );
   BaseRecipeProvider.GeneratedRecipe AE2_CERTUS = this.create(
      Mods.AE2.recipeId("certus_quartz"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(AllTags.AllItemTags.CERTUS_QUARTZ.tag))
               .output(Mods.AE2, "certus_quartz_dust"))
            .whenModLoaded(Mods.AE2.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AE2_ENDER = this.create(
      Mods.AE2.recipeId("ender_pearl"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .require(net.neoforged.neoforge.common.Tags.Items.ENDER_PEARLS))
               .output(Mods.AE2, "ender_dust"))
            .whenModLoaded(Mods.AE2.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AE2_FLUIX = this.create(
      Mods.AE2.recipeId("fluix_crystal"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(Mods.AE2, "fluix_crystal"))
               .output(Mods.AE2, "fluix_dust"))
            .whenModLoaded(Mods.AE2.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AE2_SKY_STONE = this.create(
      Mods.AE2.recipeId("sky_stone_block"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     300
                  ))
                  .require(Mods.AE2, "sky_stone_block"))
               .output(Mods.AE2, "sky_dust"))
            .whenModLoaded(Mods.AE2.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_GILIA = this.create(
      Mods.ATM.recipeId("gilia"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "gilia"))
                     .output(Items.PURPLE_DYE, 2))
                  .output(0.1F, Items.MAGENTA_DYE, 2))
               .output(0.1F, Items.PINK_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_HOT_BRUSH = this.create(
      Mods.ATM.recipeId("hot_monkey_brush"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "hot_monkey_brush"))
                     .output(Items.ORANGE_DYE, 2))
                  .output(0.05F, Items.RED_DYE))
               .output(0.05F, Items.YELLOW_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_SCALDING_BRUSH = this.create(
      Mods.ATM.recipeId("scalding_monkey_brush"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "scalding_monkey_brush"))
                     .output(Items.RED_DYE, 2))
                  .output(0.1F, Items.RED_DYE, 2))
               .output(0.1F, Items.ORANGE_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_WARM_BRUSH = this.create(
      Mods.ATM.recipeId("warm_monkey_brush"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "warm_monkey_brush"))
                     .output(Items.YELLOW_DYE, 2))
                  .output(0.1F, Items.YELLOW_DYE, 2))
               .output(0.1F, Items.ORANGE_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_YUCCA_FLOWER = this.create(
      Mods.ATM.recipeId("yucca_flower"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.ATM, "yucca_flower"))
                  .output(Items.LIGHT_GRAY_DYE, 2))
               .output(0.05F, Items.WHITE_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_TALL_YUCCA_FLOWER = this.create(
      Mods.ATM.recipeId("tall_yucca_flower"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "tall_yucca_flower"))
                     .output(Items.LIGHT_GRAY_DYE, 3))
                  .output(0.25F, Items.LIGHT_GRAY_DYE, 2))
               .output(0.05F, Items.WHITE_DYE, 2))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_FIRETHORN = this.create(
      Mods.ATM.recipeId("firethorn"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "firethorn"))
                     .output(Items.RED_DYE, 2))
                  .output(0.1F, Items.ORANGE_DYE, 2))
               .output(0.1F, Items.GREEN_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_FORSYTHIA = this.create(
      Mods.ATM.recipeId("forsythia"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "forsythia"))
                     .output(Items.YELLOW_DYE, 2))
                  .output(0.1F, Items.LIME_DYE, 2))
               .output(0.1F, Items.YELLOW_DYE))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_CACTUS = this.create(
      Mods.ATM.recipeId("barrel_cactus"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.ATM, "barrel_cactus"))
                  .output(Items.ORANGE_DYE, 2))
               .output(0.1F, Items.GREEN_DYE, 3))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_HYACINTH = this.create(
      Mods.ATM.recipeId("water_hyacinth"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.ATM, "water_hyacinth"))
                     .output(Items.PURPLE_DYE, 3))
                  .output(0.25F, Items.LIME_DYE, 2))
               .output(0.05F, Items.BROWN_DYE, 2))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_SAND_1 = this.moddedSandstone(Mods.ATM, "arid");
   BaseRecipeProvider.GeneratedRecipe ATMO_SAND_2 = this.moddedSandstone(Mods.ATM, "red_arid");
   BaseRecipeProvider.GeneratedRecipe AUTUM_CROCUS = this.create(
      Mods.AUTUM.recipeId("autumn_crocus"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(Mods.AUTUM, "autumn_crocus"))
                     .output(Items.MAGENTA_DYE, 2))
                  .output(0.1F, Items.PINK_DYE, 2))
               .output(0.1F, Items.PURPLE_DYE))
            .whenModLoaded(Mods.AUTUM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BOP_HYDRANGEA = this.bopFlower(
      "blue_hydrangea", List.of(1.0F, 0.05F, 0.25F), List.of(Items.LIGHT_BLUE_DYE, Items.GREEN_DYE, Items.LIGHT_BLUE_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_GOLDENROD = this.bopFlower(
      "goldenrod", List.of(1.0F, 0.05F, 0.25F), List.of(Items.YELLOW_DYE, Items.YELLOW_DYE, Items.GREEN_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_BLOSSOM = this.bopFlower(
      "burning_blossom", List.of(1.0F, 0.1F), List.of(Items.ORANGE_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_GLOWFLOWER = this.bopFlower(
      "glowflower", List.of(1.0F, 0.1F), List.of(Items.CYAN_DYE, Items.WHITE_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_LAVENDER = this.bopFlower("lavender", List.of(1.0F, 0.05F), List.of(Items.PURPLE_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BOP_TALL_LAVENDER = this.bopFlower(
      "tall_lavender", List.of(1.0F, 0.25F, 0.05F), List.of(Items.PURPLE_DYE, Items.PURPLE_DYE, Items.GREEN_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_WHITE_LAVENDER = this.bopFlower(
      "white_lavender", List.of(1.0F, 0.05F), List.of(Items.WHITE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_TALL_WHITE_LAVENDER = this.bopFlower(
      "tall_white_lavender", List.of(1.0F, 0.25F, 0.05F), List.of(Items.WHITE_DYE, Items.LIGHT_BLUE_DYE, Items.GREEN_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_COSMOS = this.bopFlower(
      "orange_cosmos", List.of(1.0F, 0.1F), List.of(Items.ORANGE_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_DAFFODIL = this.bopFlower(
      "pink_daffodil", List.of(1.0F, 0.25F, 0.05F), List.of(Items.PINK_DYE, Items.MAGENTA_DYE, Items.CYAN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_HIBISCUS = this.bopFlower(
      "pink_hibiscus", List.of(1.0F, 0.25F, 0.1F), List.of(Items.PINK_DYE, Items.YELLOW_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_ROSE = this.bopFlower("rose", List.of(1.0F, 0.05F), List.of(Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BOP_VIOLET = this.bopFlower("violet", 1.0F, Items.PURPLE_DYE, 2);
   BaseRecipeProvider.GeneratedRecipe BOP_WILDFLOWER = this.bopFlower(
      "wildflower", List.of(1.0F, 0.1F), List.of(Items.MAGENTA_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_PETALS = this.bopFlower("white_petals", 1.0F, Items.WHITE_DYE, 2);
   BaseRecipeProvider.GeneratedRecipe BOP_IRIS = this.bopFlower(
      "icy_iris", List.of(1.0F, 0.05F, 0.25F), List.of(Items.LIGHT_BLUE_DYE, Items.LIGHT_GRAY_DYE, Items.LIGHT_BLUE_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_LILY = this.bopFlower("wilted_lily", 1.0F, Items.GRAY_DYE, 2);
   BaseRecipeProvider.GeneratedRecipe BOP_ENDBLOOM = this.bopFlower("endbloom", 1.0F, Items.LIGHT_GRAY_DYE, 2);
   BaseRecipeProvider.GeneratedRecipe BOP_WATERLILY = this.bopFlower("waterlily", List.of(1.0F, 0.05F), List.of(Items.RED_DYE, Items.PINK_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BOP_CACTUS = this.bopFlower("tiny_cactus", List.of(1.0F, 0.1F), List.of(Items.GREEN_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BOP_CATTAIL = this.bopFlower(
      "cattail", List.of(1.0F, 0.05F, 0.25F), List.of(Items.BROWN_DYE, Items.GREEN_DYE, Items.BROWN_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BOP_SAND_1 = this.moddedSandstone(Mods.BOP, "white");
   BaseRecipeProvider.GeneratedRecipe BOP_SAND_2 = this.moddedSandstone(Mods.BOP, "orange");
   BaseRecipeProvider.GeneratedRecipe BOP_SAND_3 = this.moddedSandstone(Mods.BOP, "black");
   BaseRecipeProvider.GeneratedRecipe BTN_PETALS = this.botaniaPetals(
      "black", "blue", "brown", "cyan", "gray", "green", "light_blue", "light_gray", "lime", "magenta", "orange", "pink", "purple", "red", "white", "yellow"
   );
   BaseRecipeProvider.GeneratedRecipe BB_BUTTERCUP = this.create(
      Mods.BB.recipeId("buttercup"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.BB, "buttercup"))
                  .output(Items.YELLOW_DYE, 2))
               .output(0.1F, Items.LIME_DYE))
            .whenModLoaded(Mods.BB.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BB_PINK_CLOVER = this.create(
      Mods.BB.recipeId("pink_clover"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.BB, "pink_clover"))
                  .output(Items.PINK_DYE, 2))
               .output(0.1F, Items.LIME_DYE))
            .whenModLoaded(Mods.BB.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BB_WHITE_CLOVER = this.create(
      Mods.BB.recipeId("white_clover"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.BB, "white_clover"))
                  .output(Items.WHITE_DYE, 2))
               .output(0.1F, Items.LIME_DYE))
            .whenModLoaded(Mods.BB.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BWG_ALLIUM_BUSH = this.bwgFlower(
      "allium_flower_bush", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PURPLE_DYE, Items.GREEN_DYE, Items.MAGENTA_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_BELLFLOWER = this.bwgFlower(
      "alpine_bellflower", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PURPLE_DYE, Items.BLUE_DYE, Items.GREEN_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_AMARANTH = this.bwgFlower(
      "amaranth", List.of(1.0F, 0.05F, 0.25F), List.of(Items.RED_DYE, Items.GREEN_DYE, Items.RED_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_ANGELICA = this.bwgFlower("angelica", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_BEGONIA = this.bwgFlower("begonia", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_BISTORT = this.bwgFlower(
      "bistort", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PINK_DYE, Items.RED_DYE, Items.GREEN_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_BLACK_ROSE = this.bwgFlower(
      "black_rose", List.of(1.0F, 0.1F), List.of(Items.BLACK_DYE, Items.BLACK_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_BLUE_SAGE = this.bwgFlower("blue_sage", List.of(1.0F, 0.1F), List.of(Items.BLUE_DYE, Items.CYAN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_CALIFORNIA_POPPY = this.bwgFlower(
      "california_poppy", List.of(1.0F, 0.05F), List.of(Items.ORANGE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_CROCUS = this.bwgFlower(
      "crocus", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PURPLE_DYE, Items.BLUE_DYE, Items.GREEN_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_CYAN_AMARANTH = this.bwgFlower(
      "cyan_amaranth", List.of(1.0F, 0.05F, 0.25F), List.of(Items.CYAN_DYE, Items.GREEN_DYE, Items.CYAN_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_CYAN_ROSE = this.bwgFlower("cyan_rose", List.of(1.0F, 0.1F), List.of(Items.CYAN_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_CYAN_TULIP = this.bwgFlower("cyan_tulip", List.of(1.0F, 0.1F), List.of(Items.CYAN_DYE, Items.LIME_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_DAFFODIL = this.bwgFlower(
      "daffodil", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PINK_DYE, Items.GREEN_DYE, Items.MAGENTA_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_DELPHINIUM = this.bwgFlower("delphinium", List.of(1.0F, 0.1F), List.of(Items.BLUE_DYE, Items.BLUE_DYE), List.of(3, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_FAIRY_SLIPPER = this.bwgFlower(
      "fairy_slipper", List.of(1.0F, 0.1F, 0.1F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE, Items.YELLOW_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_FIRECRACKER_BUSH = this.bwgFlower(
      "firecracker_flower_bush", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PINK_DYE, Items.GREEN_DYE, Items.RED_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_FOXGLOVE = this.bwgFlower(
      "foxglove", List.of(1.0F, 0.25F, 0.25F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_GREEN_TULIP = this.bwgFlower(
      "green_tulip", List.of(1.0F, 0.1F), List.of(Items.LIME_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_GUZMANIA = this.bwgFlower(
      "guzmania", List.of(1.0F, 0.25F, 0.25F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_HYDRANGEA = this.bwgFlower(
      "hydrangea_bush", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PURPLE_DYE, Items.BLUE_DYE, Items.WHITE_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_INCAN_LILY = this.bwgFlower(
      "incan_lily", List.of(1.0F, 0.1F, 0.1F), List.of(Items.ORANGE_DYE, Items.GREEN_DYE, Items.RED_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_IRIS = this.bwgFlower("iris", List.of(1.0F, 0.05F), List.of(Items.PURPLE_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_ORCHID = this.bwgFlower(
      "japanese_orchid", List.of(1.0F, 0.05F), List.of(Items.PINK_DYE, Items.WHITE_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PURPLE_SAGE = this.bwgFlower(
      "purple_sage", List.of(1.0F, 0.1F), List.of(Items.PURPLE_DYE, Items.MAGENTA_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_KOVAN = this.bwgFlower(
      "kovan_flower", List.of(1.0F, 0.2F, 0.05F), List.of(Items.RED_DYE, Items.LIME_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_LAZARUS_BELLFLOWER = this.bwgFlower(
      "lazarus_bellflower", List.of(1.0F, 0.1F), List.of(Items.MAGENTA_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_LOLLIPOP = this.bwgFlower(
      "lollipop_flower", List.of(1.0F, 0.25F, 0.05F), List.of(Items.YELLOW_DYE, Items.YELLOW_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_MAGENTA_AMARANTH = this.bwgFlower(
      "magenta_amaranth", List.of(1.0F, 0.05F, 0.25F), List.of(Items.MAGENTA_DYE, Items.GREEN_DYE, Items.MAGENTA_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_MAGENTA_TULIP = this.bwgFlower(
      "magenta_tulip", List.of(1.0F, 0.1F), List.of(Items.MAGENTA_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_ORANGE_AMARANTH = this.bwgFlower(
      "orange_amaranth", List.of(1.0F, 0.05F, 0.25F), List.of(Items.ORANGE_DYE, Items.GREEN_DYE, Items.ORANGE_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_DAISY = this.bwgFlower(
      "orange_daisy", List.of(1.0F, 0.2F, 0.05F), List.of(Items.ORANGE_DYE, Items.YELLOW_DYE, Items.LIME_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_OSIRIA_ROSE = this.bwgFlower(
      "osiria_rose", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PEACH_LEATHER = this.bwgFlower(
      "peach_leather_flower", List.of(1.0F, 0.25F), List.of(Items.PINK_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PINK_ALLIUM = this.bwgFlower(
      "pink_allium", List.of(1.0F, 0.1F, 0.1F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE, Items.PURPLE_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PINK_ALLIUM_BUSH = this.bwgFlower(
      "pink_allium_flower_bush", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PURPLE_DYE, Items.GREEN_DYE, Items.MAGENTA_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PINK_ANEMONE = this.bwgFlower(
      "pink_anemone", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.PURPLE_DYE), List.of(2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PINK_DAFODIL = this.bwgFlower(
      "pink_daffodil", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PINK_DYE, Items.GREEN_DYE, Items.WHITE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PROTEA = this.bwgFlower(
      "protea_flower", List.of(1.0F, 0.1F, 0.05F), List.of(Items.MAGENTA_DYE, Items.LIME_DYE, Items.PURPLE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PURPLE_AMARANTH = this.bwgFlower(
      "purple_amaranth", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PURPLE_DYE, Items.GREEN_DYE, Items.PURPLE_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PURPLE_TULIP = this.bwgFlower(
      "purple_tulip", List.of(1.0F, 0.1F), List.of(Items.PURPLE_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_RICHEA = this.bwgFlower(
      "richea", List.of(1.0F, 0.1F, 0.05F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_ROSE = this.bwgFlower("rose", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe BWG_SILVER_VASE = this.bwgFlower(
      "silver_vase_flower", List.of(1.0F, 0.1F, 0.05F), List.of(Items.PINK_DYE, Items.GREEN_DYE, Items.WHITE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_SNOWDROPS = this.bwgFlower(
      "snowdrops", List.of(1.0F, 0.1F, 0.1F), List.of(Items.WHITE_DYE, Items.LIME_DYE, Items.WHITE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_TALL_ALLIUM = this.bwgFlower(
      "tall_allium", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PURPLE_DYE, Items.PURPLE_DYE, Items.MAGENTA_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_TALL_PINK_ALLIUM = this.bwgFlower(
      "tall_pink_allium", List.of(1.0F, 0.05F, 0.25F), List.of(Items.PINK_DYE, Items.PINK_DYE, Items.MAGENTA_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_VIOLET_LEATHER = this.bwgFlower(
      "violet_leather_flower", List.of(1.0F, 0.25F), List.of(Items.BLUE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WHITE_ANEMONE = this.bwgFlower(
      "white_anemone", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.LIGHT_GRAY_DYE), List.of(2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_PUFFBALL = this.create(
      Mods.BWG.recipeId("white_puffball_cap"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     150
                  ))
                  .require(Mods.BWG, "white_puffball_cap"))
               .output(0.25F, Mods.BWG, "white_puffball_spores", 1))
            .whenModLoaded(Mods.BWG.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WHITE_SAGE = this.bwgFlower(
      "white_sage", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.GRAY_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WINTER_CYCLAMEN = this.bwgFlower(
      "winter_cyclamen", List.of(1.0F, 0.1F), List.of(Items.CYAN_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WINTER_ROSE = this.bwgFlower(
      "winter_rose", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WINTER_SCILLA = this.bwgFlower(
      "winter_scilla", List.of(1.0F, 0.1F), List.of(Items.LIGHT_BLUE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_YELLOW_DAFFODIL = this.bwgFlower(
      "yellow_daffodil", List.of(1.0F, 0.1F, 0.1F), List.of(Items.YELLOW_DYE, Items.GREEN_DYE, Items.PINK_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_YELLOW_TULIP = this.bwgFlower(
      "yellow_tulip", List.of(1.0F, 0.1F), List.of(Items.YELLOW_DYE, Items.LIME_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WHITE_ALLIUM = this.bwgFlower(
      "white_allium", List.of(1.0F, 0.1F, 0.1F), List.of(Items.WHITE_DYE, Items.LIGHT_GRAY_DYE, Items.GRAY_DYE), List.of(2, 2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_TALL_WHITE_ALLIUM = this.bwgFlower(
      "tall_white_allium", List.of(1.0F, 0.05F, 0.25F), List.of(Items.WHITE_DYE, Items.WHITE_DYE, Items.LIGHT_GRAY_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WHITE_ALLIUM_BUSH = this.bwgFlower(
      "white_allium_flower_bush", List.of(1.0F, 0.05F, 0.25F), List.of(Items.WHITE_DYE, Items.GREEN_DYE, Items.LIGHT_GRAY_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_BLUE_ROSE_BUSH = this.bwgFlower(
      "blue_rose_bush", List.of(1.0F, 0.05F, 0.25F), List.of(Items.BLUE_DYE, Items.GREEN_DYE, Items.BLUE_DYE), List.of(3, 2, 2)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_HORSEWEED = this.bwgFlower(
      "horseweed", List.of(1.0F, 0.25F), List.of(Items.GREEN_DYE, Items.BROWN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_WINTER_SUCCULENT = this.bwgFlower(
      "winter_succulent", List.of(1.0F, 0.25F), List.of(Items.GREEN_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_MINI_CACTUS = this.bwgFlower("mini_cactus", List.of(1.0F), List.of(Items.GREEN_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe BWG_PRICKLY_PEAR_CACTUS = this.bwgFlower(
      "prickly_pear_cactus", List.of(1.0F, 0.25F), List.of(Items.GREEN_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_GOLDEN_SPINED_CACTUS = this.bwgFlower(
      "golden_spined_cactus", List.of(1.0F, 0.25F), List.of(Items.GREEN_DYE, Items.YELLOW_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_1 = this.moddedSandstone(Mods.BWG, "black");
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_2 = this.moddedSandstone(Mods.BWG, "white");
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_3 = this.moddedSandstone(Mods.BWG, "blue");
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_4 = this.moddedSandstone(Mods.BWG, "purple");
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_5 = this.moddedSandstone(Mods.BWG, "pink");
   BaseRecipeProvider.GeneratedRecipe BWG_SAND_6 = this.moddedSandstone(Mods.BWG, "windswept");
   BaseRecipeProvider.GeneratedRecipe ENV_BIRD_OF_PARADISE = this.envFlower(
      "bird_of_paradise", List.of(1.0F, 0.25F, 0.25F), List.of(Items.ORANGE_DYE, Items.BLUE_DYE, Items.RED_DYE), List.of(3, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_BLUE_DELPHINIUM = this.envFlower(
      "blue_delphinium", List.of(1.0F, 0.1F), List.of(Items.LIGHT_BLUE_DYE, Items.LIGHT_BLUE_DYE), List.of(3, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_BLUEBELL = this.envFlower("bluebell", List.of(1.0F), List.of(Items.BLUE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe ENV_CARTWHEEL = this.envFlower("cartwheel", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.ORANGE_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe ENV_DIANTHUS = this.envFlower("dianthus", List.of(1.0F, 0.1F), List.of(Items.LIME_DYE, Items.LIME_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe ENV_MAGENTA_HIBISCUS = this.envFlower(
      "magenta_hibiscus", List.of(1.0F, 0.1F), List.of(Items.MAGENTA_DYE, Items.MAGENTA_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_ORANGE_HIBISCUS = this.envFlower(
      "orange_hibiscus", List.of(1.0F, 0.1F), List.of(Items.ORANGE_DYE, Items.ORANGE_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_PINK_DELPHINIUM = this.envFlower(
      "pink_delphinium", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.PINK_DYE), List.of(3, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_PINK_HIBISCUS = this.envFlower(
      "pink_hibiscus", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.PINK_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_PURPLE_DELPHINIUM = this.envFlower(
      "purple_delphinium", List.of(1.0F, 0.1F), List.of(Items.PURPLE_DYE, Items.PURPLE_DYE), List.of(3, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_PURPLE_HIBISCUS = this.envFlower(
      "purple_hibiscus", List.of(1.0F, 0.1F), List.of(Items.PURPLE_DYE, Items.PURPLE_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_RED_HIBISCUS = this.envFlower(
      "red_hibiscus", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.RED_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_RED_LOTUS = this.envFlower(
      "red_lotus_flower", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.RED_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_VIOLET = this.envFlower("violet", List.of(1.0F, 0.1F), List.of(Items.PURPLE_DYE, Items.PURPLE_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe ENV_WHITE_DELPHINIUM = this.envFlower(
      "white_delphinium", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.WHITE_DYE), List.of(3, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_WHITE_LOTUS_FLOWER = this.envFlower(
      "white_lotus_flower", List.of(1.0F, 0.1F), List.of(Items.WHITE_DYE, Items.WHITE_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_YELLOW_HIBISCUS = this.envFlower(
      "yellow_hibiscus", List.of(1.0F, 0.1F), List.of(Items.YELLOW_DYE, Items.YELLOW_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ENV_TASSELFLOWER = this.envFlower(
      "tasselflower", List.of(1.0F, 0.1F), List.of(Items.ORANGE_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe DC_LAVENDER = this.create(
      Mods.DRUIDCRAFT.recipeId("lavender"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.DRUIDCRAFT, "lavender"))
                  .output(Items.PURPLE_DYE, 2))
               .output(0.1F, Items.PURPLE_DYE))
            .whenModLoaded(Mods.DRUIDCRAFT.getId())
   );
   BaseRecipeProvider.GeneratedRecipe SUP_FLAX = this.create(
      Mods.SUP.recipeId("flax"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           150
                        ))
                        .require(Mods.SUP, "flax"))
                     .output(Items.STRING))
                  .output(0.25F, Items.STRING, 2))
               .output(0.25F, Mods.SUP, "flax_seeds", 1))
            .whenModLoaded(Mods.SUP.getId())
   );
   BaseRecipeProvider.GeneratedRecipe TIC_NERCOTIC_BONE = this.create(
      Mods.TIC.recipeId("nercotic_bone"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           100
                        ))
                        .require(Mods.TIC, "necrotic_bone"))
                     .output(Items.BONE_MEAL, 3))
                  .output(0.25F, Items.BLACK_DYE))
               .output(0.25F, Items.BONE_MEAL, 3))
            .whenModLoaded(Mods.TIC.getId())
   );
   BaseRecipeProvider.GeneratedRecipe UA_FLOWERING_RUSH = this.create(
      Mods.UA.recipeId("flowering_rush"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.UA, "flowering_rush"))
                  .output(Items.PINK_DYE, 3))
               .output(0.25F, Items.PINK_DYE, 2))
            .whenModLoaded(Mods.UA.getId())
   );
   BaseRecipeProvider.GeneratedRecipe UA_PINK_SEAROCKET = this.create(
      Mods.UA.recipeId("pink_searocket"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.UA, "pink_searocket"))
                  .output(Items.PINK_DYE, 2))
               .output(0.1F, Items.GREEN_DYE))
            .whenModLoaded(Mods.UA.getId())
   );
   BaseRecipeProvider.GeneratedRecipe UA_WHITE_SEAROCKET = this.create(
      Mods.UA.recipeId("white_searocket"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.UA, "white_searocket"))
                  .output(Items.WHITE_DYE, 2))
               .output(0.1F, Items.GREEN_DYE))
            .whenModLoaded(Mods.UA.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_ALPHA_DANDELION = this.ruFlower(
      "alpha_dandelion", List.of(1.0F, 0.05F), List.of(Items.YELLOW_DYE, Items.YELLOW_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_ALPHA_ROSE = this.ruFlower("alpha_rose", List.of(1.0F, 0.05F), List.of(Items.RED_DYE, Items.RED_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_ASTER = this.ruFlower(
      "aster", List.of(1.0F, 0.2F, 0.05F), List.of(Items.LIGHT_BLUE_DYE, Items.WHITE_DYE, Items.LIGHT_GRAY_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_BLACK_SNOWBELLE = this.ruFlower("black_snowbelle", List.of(1.0F), List.of(Items.BLACK_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_BLEEDING_HEART = this.ruFlower(
      "bleeding_heart", List.of(1.0F, 0.1F), List.of(Items.MAGENTA_DYE, Items.PINK_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_BLUE_LUPINE = this.ruFlower("blue_lupine", List.of(1.0F), List.of(Items.BLUE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_BLUE_SNOWBELLE = this.ruFlower("blue_snowbelle", List.of(1.0F), List.of(Items.BLUE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_BROWN_SNOWBELLE = this.ruFlower("brown_snowbelle", List.of(1.0F), List.of(Items.BROWN_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_CACTUS_FLOWER = this.ruFlower(
      "cactus_flower", List.of(1.0F, 0.2F, 0.1F), List.of(Items.MAGENTA_DYE, Items.PURPLE_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_CYAN_SNOWBELLE = this.ruFlower("cyan_snowbelle", List.of(1.0F), List.of(Items.CYAN_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_DAISY = this.ruFlower(
      "daisy", List.of(1.0F, 0.2F, 0.05F), List.of(Items.LIGHT_GRAY_DYE, Items.WHITE_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_DAY_LILY = this.ruFlower(
      "day_lily", List.of(1.0F, 0.1F, 0.1F), List.of(Items.ORANGE_DYE, Items.LIME_DYE, Items.RED_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_DORCEL = this.ruFlower("dorcel", List.of(1.0F, 0.1F), List.of(Items.BLACK_DYE, Items.BROWN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_FELICIA_DAISY = this.ruFlower(
      "felicia_daisy", List.of(1.0F, 0.2F, 0.05F), List.of(Items.LIGHT_BLUE_DYE, Items.BLUE_DYE, Items.WHITE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_FIREWEED = this.ruFlower("fireweed", List.of(1.0F), List.of(Items.MAGENTA_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_GLITERING_BLOOM = this.ruFlower(
      "glistering_bloom", List.of(1.0F, 0.25F, 0.25F), List.of(Items.PINK_DYE, Items.MAGENTA_DYE, Items.LIGHT_BLUE_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_GRAY_SNOWBELLE = this.ruFlower("gray_snowbelle", List.of(1.0F), List.of(Items.GRAY_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_GREEN_SNOWBELLE = this.ruFlower("green_snowbelle", List.of(1.0F), List.of(Items.GREEN_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_HIBISCUS = this.ruFlower("hibiscus", List.of(1.0F, 0.2F), List.of(Items.YELLOW_DYE, Items.RED_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_HYSSOP = this.ruFlower(
      "hyssop", List.of(1.0F, 0.1F, 0.1F), List.of(Items.PURPLE_DYE, Items.MAGENTA_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_LIGHT_BLUE_SNOWBELLE = this.ruFlower("light_blue_snowbelle", List.of(1.0F), List.of(Items.LIGHT_BLUE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_LIGHT_GRAY_SNOWBELLE = this.ruFlower("light_gray_snowbelle", List.of(1.0F), List.of(Items.LIGHT_GRAY_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_LIME_SNOWBELLE = this.ruFlower("lime_snowbelle", List.of(1.0F), List.of(Items.LIME_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_MAGENTA_SNOWBELLE = this.ruFlower("magenta_snowbelle", List.of(1.0F), List.of(Items.MAGENTA_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_MALLOW = this.ruFlower("mallow", List.of(1.0F, 0.1F), List.of(Items.ORANGE_DYE, Items.LIME_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_ORANGE_CONEFLOWER = this.ruFlower("orange_coneflower", List.of(1.0F), List.of(Items.ORANGE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_ORANGE_SNOWBELLE = this.ruFlower("orange_snowbelle", List.of(1.0F), List.of(Items.ORANGE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_PINK_LUPINE = this.ruFlower("pink_lupine", List.of(1.0F), List.of(Items.PINK_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_PINK_SNOWBELLE = this.ruFlower("pink_snowbelle", List.of(1.0F), List.of(Items.PINK_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_POPPY_BUSH = this.ruFlower("poppy_bush", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_PURPLE_CONEFLOWER = this.ruFlower("purple_coneflower", List.of(1.0F), List.of(Items.PURPLE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_PURPLE_LUPINE = this.ruFlower("purple_lupine", List.of(1.0F), List.of(Items.PURPLE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_PURPLE_SNOWBELLE = this.ruFlower("purple_snowbelle", List.of(1.0F), List.of(Items.PURPLE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_RED_LUPINE = this.ruFlower("red_lupine", List.of(1.0F), List.of(Items.RED_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_RED_SNOWBELLE = this.ruFlower("red_snowbelle", List.of(1.0F), List.of(Items.RED_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_SALMON_POPPY_BUSH = this.ruFlower(
      "salmon_poppy_bush", List.of(1.0F, 0.1F), List.of(Items.PINK_DYE, Items.GREEN_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_TASSEL = this.ruFlower(
      "tassel", List.of(1.0F, 0.2F, 0.05F), List.of(Items.LIGHT_GRAY_DYE, Items.WHITE_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_TSUBAKI = this.ruFlower("tsubaki", List.of(1.0F, 0.1F), List.of(Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1));
   BaseRecipeProvider.GeneratedRecipe RU_WARATAH = this.ruFlower(
      "waratah", List.of(1.0F, 0.2F, 0.1F), List.of(Items.RED_DYE, Items.RED_DYE, Items.GREEN_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_WHITE_SNOWBELLE = this.ruFlower("white_snowbelle", List.of(1.0F), List.of(Items.WHITE_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_WHITE_TRILLIUM = this.ruFlower(
      "white_trillium", List.of(1.0F, 0.2F, 0.05F), List.of(Items.LIGHT_GRAY_DYE, Items.WHITE_DYE, Items.YELLOW_DYE), List.of(2, 1, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_WILTING_TRILLIUM = this.ruFlower(
      "wilting_trillium", List.of(1.0F, 0.1F), List.of(Items.BROWN_DYE, Items.LIGHT_GRAY_DYE), List.of(2, 1)
   );
   BaseRecipeProvider.GeneratedRecipe RU_YELLOW_LUPINE = this.ruFlower("yellow_lupine", List.of(1.0F), List.of(Items.YELLOW_DYE), List.of(2));
   BaseRecipeProvider.GeneratedRecipe RU_YELLOW_SNOWBELLE = this.ruFlower("yellow_snowbelle", List.of(1.0F), List.of(Items.YELLOW_DYE), List.of(2));

   BaseRecipeProvider.GeneratedRecipe bopFlower(String input, List<Float> chances, List<Item> dyes, List<Integer> amounts) {
      if (chances.size() == 2) {
         return this.create(
            Mods.BOP.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.BOP, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                  .whenModLoaded(Mods.BOP.getId())
         );
      } else if (chances.size() == 3) {
         return this.create(
            Mods.BOP.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 50
                              ))
                              .require(Mods.BOP, input))
                           .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                        .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                     .output(chances.get(2), (ItemLike)dyes.get(2), amounts.get(2)))
                  .whenModLoaded(Mods.BOP.getId())
         );
      } else {
         return chances.size() == 1
            ? this.create(
               Mods.BOP.recipeId(input),
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.BOP, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .whenModLoaded(Mods.BOP.getId())
            )
            : null;
      }
   }

   BaseRecipeProvider.GeneratedRecipe bwgFlower(String input, List<Float> chances, List<Item> dyes, List<Integer> amounts) {
      if (chances.size() == 2) {
         return this.create(
            Mods.BWG.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.BWG, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                  .whenModLoaded(Mods.BWG.getId())
         );
      } else if (chances.size() == 3) {
         return this.create(
            Mods.BWG.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 50
                              ))
                              .require(Mods.BWG, input))
                           .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                        .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                     .output(chances.get(2), (ItemLike)dyes.get(2), amounts.get(2)))
                  .whenModLoaded(Mods.BWG.getId())
         );
      } else {
         return chances.size() == 1
            ? this.create(
               Mods.BWG.recipeId(input),
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.BWG, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .whenModLoaded(Mods.BWG.getId())
            )
            : null;
      }
   }

   BaseRecipeProvider.GeneratedRecipe envFlower(String input, List<Float> chances, List<Item> dyes, List<Integer> amounts) {
      if (chances.size() == 2) {
         return this.create(
            Mods.ENV.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.ENV, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                  .whenModLoaded(Mods.ENV.getId())
         );
      } else if (chances.size() == 3) {
         return this.create(
            Mods.ENV.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 50
                              ))
                              .require(Mods.ENV, input))
                           .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                        .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                     .output(chances.get(2), (ItemLike)dyes.get(2), amounts.get(2)))
                  .whenModLoaded(Mods.ENV.getId())
         );
      } else {
         return chances.size() == 1
            ? this.create(
               Mods.ENV.recipeId(input),
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.ENV, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .whenModLoaded(Mods.ENV.getId())
            )
            : null;
      }
   }

   BaseRecipeProvider.GeneratedRecipe bopFlower(String input, Float chance, Item dye, int amount) {
      return this.create(
         Mods.BOP.recipeId(input),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Mods.BOP, input))
                  .output(chance, dye, amount))
               .whenModLoaded(Mods.BOP.getId())
      );
   }

   BaseRecipeProvider.GeneratedRecipe botaniaPetals(String... colors) {
      for (String color : colors) {
         this.create(
            Mods.BTN.recipeId(color + "_petal"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           50
                        ))
                        .require(TagKey.create(Registries.ITEM, Mods.BTN.asResource("petals/" + color))))
                     .output(Mods.MC, color + "_dye"))
                  .whenModLoaded(Mods.BTN.getId())
         );
      }

      return null;
   }

   BaseRecipeProvider.GeneratedRecipe ruFlower(String input, List<Float> chances, List<Item> dyes, List<Integer> amounts) {
      if (chances.size() == 2) {
         return this.create(
            Mods.RU.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.RU, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                  .whenModLoaded(Mods.RU.getId())
         );
      } else if (chances.size() == 3) {
         return this.create(
            Mods.RU.recipeId(input),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 50
                              ))
                              .require(Mods.RU, input))
                           .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                        .output(chances.get(1), (ItemLike)dyes.get(1), amounts.get(1)))
                     .output(chances.get(2), (ItemLike)dyes.get(2), amounts.get(2)))
                  .whenModLoaded(Mods.RU.getId())
         );
      } else {
         return chances.size() == 1
            ? this.create(
               Mods.RU.recipeId(input),
               b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              50
                           ))
                           .require(Mods.RU, input))
                        .output(chances.get(0), (ItemLike)dyes.get(0), amounts.get(0)))
                     .whenModLoaded(Mods.RU.getId())
            )
            : null;
      }
   }

   public CreateMillingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
