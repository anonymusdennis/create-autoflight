package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.CrushingRecipeGen;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreateCrushingRecipeGen extends CrushingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe BLAZE_ROD = this.create(
      () -> Items.BLAZE_ROD,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(100))
               .output(Items.BLAZE_POWDER, 3))
            .output(0.25F, Items.BLAZE_POWDER, 3)
   );
   BaseRecipeProvider.GeneratedRecipe PRISMARINE_CRYSTALS = this.create(
      () -> Items.PRISMARINE_CRYSTALS,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     150
                  ))
                  .output(1.0F, Items.QUARTZ, 1))
               .output(0.5F, Items.QUARTZ, 2))
            .output(0.1F, Items.GLOWSTONE_DUST, 2)
   );
   BaseRecipeProvider.GeneratedRecipe LEATHER_HORSE_ARMOR = this.create(
      () -> Items.LEATHER_HORSE_ARMOR,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(200)).output(Items.LEATHER, 2))
            .output(0.5F, Items.LEATHER, 2)
   );
   BaseRecipeProvider.GeneratedRecipe IRON_HORSE_ARMOR = this.create(
      () -> Items.IRON_HORSE_ARMOR,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           200
                        ))
                        .output(Items.IRON_INGOT, 2))
                     .output(0.5F, Items.LEATHER, 1))
                  .output(0.5F, Items.IRON_INGOT, 1))
               .output(0.25F, Items.STRING, 2))
            .output(0.25F, Items.IRON_NUGGET, 4)
   );
   BaseRecipeProvider.GeneratedRecipe GOLDEN_HORSE_ARMOR = this.create(
      () -> Items.GOLDEN_HORSE_ARMOR,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           200
                        ))
                        .output(Items.GOLD_INGOT, 2))
                     .output(0.5F, Items.LEATHER, 2))
                  .output(0.5F, Items.GOLD_INGOT, 2))
               .output(0.25F, Items.STRING, 2))
            .output(0.25F, Items.GOLD_NUGGET, 8)
   );
   BaseRecipeProvider.GeneratedRecipe DIAMOND_HORSE_ARMOR = this.create(
      () -> Items.DIAMOND_HORSE_ARMOR,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        200
                     ))
                     .output(Items.DIAMOND, 1))
                  .output(0.5F, Items.LEATHER, 2))
               .output(0.1F, Items.DIAMOND, 3))
            .output(0.25F, Items.STRING, 2)
   );
   BaseRecipeProvider.GeneratedRecipe WOOL = this.create(
      "wool",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .require(ItemTags.WOOL))
               .output(Items.STRING, 2))
            .output(0.5F, Items.STRING)
   );
   BaseRecipeProvider.GeneratedRecipe NETHER_WART = this.create(
      "nether_wart_block",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(150))
               .require(Blocks.NETHER_WART_BLOCK))
            .output(0.25F, Items.NETHER_WART, 1)
   );
   BaseRecipeProvider.GeneratedRecipe AMETHYST_CLUSTER = this.create(
      () -> Blocks.AMETHYST_CLUSTER,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(150))
               .output(Items.AMETHYST_SHARD, 7))
            .output(0.5F, Items.AMETHYST_SHARD)
   );
   BaseRecipeProvider.GeneratedRecipe GLOWSTONE = this.create(
      () -> Blocks.GLOWSTONE,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(150))
               .output(Items.GLOWSTONE_DUST, 3))
            .output(0.5F, Items.GLOWSTONE_DUST)
   );
   BaseRecipeProvider.GeneratedRecipe AMETHYST_BLOCK = this.create(
      () -> Blocks.AMETHYST_BLOCK,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(150))
               .output(Items.AMETHYST_SHARD, 3))
            .output(0.5F, Items.AMETHYST_SHARD)
   );
   BaseRecipeProvider.GeneratedRecipe GRAVEL = this.create(
      () -> Blocks.GRAVEL,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     250
                  ))
                  .output(Blocks.SAND))
               .output(0.1F, Items.FLINT))
            .output(0.05F, Items.CLAY_BALL)
   );
   BaseRecipeProvider.GeneratedRecipe NETHERRACK = this.create(
      () -> Blocks.NETHERRACK,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     250
                  ))
                  .output((ItemLike)AllItems.CINDER_FLOUR.get()))
               .output(0.5F, (ItemLike)AllItems.CINDER_FLOUR.get()))
            .whenModMissing(Mods.ENS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe OBSIDIAN = this.create(
      () -> Blocks.OBSIDIAN,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(500))
               .output((ItemLike)AllItems.POWDERED_OBSIDIAN.get()))
            .output(0.75F, Blocks.OBSIDIAN)
   );
   BaseRecipeProvider.GeneratedRecipe DIORITE = this.ensMineralRecycling(
      AllPaletteStoneTypes.DIORITE, b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(350)).output(0.25F, Items.QUARTZ, 1)
   );
   BaseRecipeProvider.GeneratedRecipe CRIMSITE = this.mineralRecycling(AllPaletteStoneTypes.CRIMSITE, AllItems.CRUSHED_IRON::get, () -> Items.IRON_NUGGET, 0.4F);
   BaseRecipeProvider.GeneratedRecipe VERIDIUM = this.mineralRecycling(
      AllPaletteStoneTypes.VERIDIUM, AllItems.CRUSHED_COPPER::get, () -> AllItems.COPPER_NUGGET::get, 0.8F
   );
   BaseRecipeProvider.GeneratedRecipe ASURINE = this.mineralRecycling(
      AllPaletteStoneTypes.ASURINE, AllItems.CRUSHED_ZINC::get, () -> AllItems.ZINC_NUGGET::get, 0.3F
   );
   BaseRecipeProvider.GeneratedRecipe OCHRUM = this.mineralRecycling(AllPaletteStoneTypes.OCHRUM, AllItems.CRUSHED_GOLD::get, () -> Items.GOLD_NUGGET, 0.2F);
   BaseRecipeProvider.GeneratedRecipe TUFF = this.mineralRecycling(
      AllPaletteStoneTypes.TUFF,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           350
                        ))
                        .output(0.25F, Items.FLINT, 1))
                     .output(0.1F, Items.GOLD_NUGGET, 1))
                  .output(0.1F, (ItemLike)AllItems.COPPER_NUGGET.get(), 1))
               .output(0.1F, (ItemLike)AllItems.ZINC_NUGGET.get(), 1))
            .output(0.1F, Items.IRON_NUGGET, 1)
   );
   BaseRecipeProvider.GeneratedRecipe COAL_ORE = this.stoneOre(() -> Items.COAL_ORE, () -> Items.COAL, 1.75F, 150);
   BaseRecipeProvider.GeneratedRecipe IRON_ORE = this.stoneOre(() -> Items.IRON_ORE, AllItems.CRUSHED_IRON::get, 1.75F, 250);
   BaseRecipeProvider.GeneratedRecipe COPPER_ORE = this.stoneOre(() -> Items.COPPER_ORE, AllItems.CRUSHED_COPPER::get, 5.25F, 250);
   BaseRecipeProvider.GeneratedRecipe GOLD_ORE = this.stoneOre(() -> Items.GOLD_ORE, AllItems.CRUSHED_GOLD::get, 1.75F, 250);
   BaseRecipeProvider.GeneratedRecipe REDSTONE_ORE = this.stoneOre(() -> Items.REDSTONE_ORE, () -> Items.REDSTONE, 6.5F, 250);
   BaseRecipeProvider.GeneratedRecipe EMERALD_ORE = this.stoneOre(() -> Items.EMERALD_ORE, () -> Items.EMERALD, 1.75F, 350);
   BaseRecipeProvider.GeneratedRecipe LAPIS_ORE = this.stoneOre(() -> Items.LAPIS_ORE, () -> Items.LAPIS_LAZULI, 10.5F, 250);
   BaseRecipeProvider.GeneratedRecipe DIAMOND_ORE = this.stoneOre(() -> Items.DIAMOND_ORE, () -> Items.DIAMOND, 1.75F, 350);
   BaseRecipeProvider.GeneratedRecipe ZINC_ORE = this.stoneOre(AllBlocks.ZINC_ORE::get, AllItems.CRUSHED_ZINC::get, 1.75F, 250);
   BaseRecipeProvider.GeneratedRecipe DEEP_COAL_ORE = this.deepslateOre(() -> Items.DEEPSLATE_COAL_ORE, () -> Items.COAL, 2.25F, 300);
   BaseRecipeProvider.GeneratedRecipe DEEP_IRON_ORE = this.deepslateOre(() -> Items.DEEPSLATE_IRON_ORE, AllItems.CRUSHED_IRON::get, 2.25F, 350);
   BaseRecipeProvider.GeneratedRecipe DEEP_COPPER_ORE = this.deepslateOre(() -> Items.DEEPSLATE_COPPER_ORE, AllItems.CRUSHED_COPPER::get, 7.25F, 350);
   BaseRecipeProvider.GeneratedRecipe DEEP_GOLD_ORE = this.deepslateOre(() -> Items.DEEPSLATE_GOLD_ORE, AllItems.CRUSHED_GOLD::get, 2.25F, 350);
   BaseRecipeProvider.GeneratedRecipe DEEP_REDSTONE_ORE = this.deepslateOre(() -> Items.DEEPSLATE_REDSTONE_ORE, () -> Items.REDSTONE, 7.5F, 350);
   BaseRecipeProvider.GeneratedRecipe DEEP_EMERALD_ORE = this.deepslateOre(() -> Items.DEEPSLATE_EMERALD_ORE, () -> Items.EMERALD, 2.25F, 450);
   BaseRecipeProvider.GeneratedRecipe DEEP_LAPIS_ORE = this.deepslateOre(() -> Items.DEEPSLATE_LAPIS_ORE, () -> Items.LAPIS_LAZULI, 12.5F, 350);
   BaseRecipeProvider.GeneratedRecipe DEEP_DIAMOND_ORE = this.deepslateOre(() -> Items.DEEPSLATE_DIAMOND_ORE, () -> Items.DIAMOND, 2.25F, 450);
   BaseRecipeProvider.GeneratedRecipe DEEP_ZINC_ORE = this.deepslateOre(AllBlocks.DEEPSLATE_ZINC_ORE::get, AllItems.CRUSHED_ZINC::get, 2.25F, 350);
   BaseRecipeProvider.GeneratedRecipe NETHER_GOLD_ORE = this.netherOre(() -> Items.NETHER_GOLD_ORE, () -> Items.GOLD_NUGGET, 18.0F, 350);
   BaseRecipeProvider.GeneratedRecipe NETHER_QUARTZ_ORE = this.netherOre(() -> Items.NETHER_QUARTZ_ORE, () -> Items.QUARTZ, 2.25F, 350);
   BaseRecipeProvider.GeneratedRecipe GILDED_BLACKSTONE = this.ore(Items.BLACKSTONE, () -> Items.GILDED_BLACKSTONE, () -> Items.GOLD_NUGGET, 18.0F, 400);
   BaseRecipeProvider.GeneratedRecipe OSMIUM_ORE = this.moddedOre(CommonMetal.OSMIUM, AllItems.CRUSHED_OSMIUM::get);
   BaseRecipeProvider.GeneratedRecipe PLATINUM_ORE = this.moddedOre(CommonMetal.PLATINUM, AllItems.CRUSHED_PLATINUM::get);
   BaseRecipeProvider.GeneratedRecipe SILVER_ORE = this.moddedOre(CommonMetal.SILVER, AllItems.CRUSHED_SILVER::get);
   BaseRecipeProvider.GeneratedRecipe TIN_ORE = this.moddedOre(CommonMetal.TIN, AllItems.CRUSHED_TIN::get);
   BaseRecipeProvider.GeneratedRecipe QUICKSILVER_ORE = this.moddedOre(CommonMetal.QUICKSILVER, AllItems.CRUSHED_QUICKSILVER::get);
   BaseRecipeProvider.GeneratedRecipe LEAD_ORE = this.moddedOre(CommonMetal.LEAD, AllItems.CRUSHED_LEAD::get);
   BaseRecipeProvider.GeneratedRecipe ALUMINUM_ORE = this.moddedOre(CommonMetal.ALUMINUM, AllItems.CRUSHED_BAUXITE::get);
   BaseRecipeProvider.GeneratedRecipe URANIUM_ORE = this.moddedOre(CommonMetal.URANIUM, AllItems.CRUSHED_URANIUM::get);
   BaseRecipeProvider.GeneratedRecipe NICKEL_ORE = this.moddedOre(CommonMetal.NICKEL, AllItems.CRUSHED_NICKEL::get);
   BaseRecipeProvider.GeneratedRecipe RAW_IRON_ORE = this.rawOre(
      "iron", () -> net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS_IRON, AllItems.CRUSHED_IRON::get, 1
   );
   BaseRecipeProvider.GeneratedRecipe RAW_COPPER_ORE = this.rawOre(
      "copper", () -> net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS_COPPER, AllItems.CRUSHED_COPPER::get, 1
   );
   BaseRecipeProvider.GeneratedRecipe RAW_GOLD_ORE = this.rawOre(
      "gold", () -> net.neoforged.neoforge.common.Tags.Items.RAW_MATERIALS_GOLD, AllItems.CRUSHED_GOLD::get, 2
   );
   BaseRecipeProvider.GeneratedRecipe RAW_ZINC_ORE = this.rawOre("zinc", () -> CommonMetal.ZINC.rawOres, AllItems.CRUSHED_ZINC::get, 1);
   BaseRecipeProvider.GeneratedRecipe OSMIUM_RAW_ORE = this.moddedRawOre(CommonMetal.OSMIUM, AllItems.CRUSHED_OSMIUM::get);
   BaseRecipeProvider.GeneratedRecipe PLATINUM_RAW_ORE = this.moddedRawOre(CommonMetal.PLATINUM, AllItems.CRUSHED_PLATINUM::get);
   BaseRecipeProvider.GeneratedRecipe SILVER_RAW_ORE = this.moddedRawOre(CommonMetal.SILVER, AllItems.CRUSHED_SILVER::get);
   BaseRecipeProvider.GeneratedRecipe TIN_RAW_ORE = this.moddedRawOre(CommonMetal.TIN, AllItems.CRUSHED_TIN::get);
   BaseRecipeProvider.GeneratedRecipe QUICKSILVER_RAW_ORE = this.moddedRawOre(CommonMetal.QUICKSILVER, AllItems.CRUSHED_QUICKSILVER::get);
   BaseRecipeProvider.GeneratedRecipe LEAD_RAW_ORE = this.moddedRawOre(CommonMetal.LEAD, AllItems.CRUSHED_LEAD::get);
   BaseRecipeProvider.GeneratedRecipe ALUMINUM_RAW_ORE = this.moddedRawOre(CommonMetal.ALUMINUM, AllItems.CRUSHED_BAUXITE::get);
   BaseRecipeProvider.GeneratedRecipe URANIUM_RAW_ORE = this.moddedRawOre(CommonMetal.URANIUM, AllItems.CRUSHED_URANIUM::get);
   BaseRecipeProvider.GeneratedRecipe NICKEL_RAW_ORE = this.moddedRawOre(CommonMetal.NICKEL, AllItems.CRUSHED_NICKEL::get);
   BaseRecipeProvider.GeneratedRecipe RAW_IRON_BLOCK = this.rawOreBlock(
      "iron", () -> net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS_RAW_IRON, AllItems.CRUSHED_IRON::get, 1
   );
   BaseRecipeProvider.GeneratedRecipe RAW_COPPER_BLOCK = this.rawOreBlock(
      "copper", () -> net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS_RAW_COPPER, AllItems.CRUSHED_COPPER::get, 1
   );
   BaseRecipeProvider.GeneratedRecipe RAW_GOLD_BLOCK = this.rawOreBlock(
      "gold", () -> net.neoforged.neoforge.common.Tags.Items.STORAGE_BLOCKS_RAW_GOLD, AllItems.CRUSHED_GOLD::get, 2
   );
   BaseRecipeProvider.GeneratedRecipe RAW_ZINC_BLOCK = this.rawOreBlock("zinc", CommonMetal.ZINC.rawStorageBlocks::items, AllItems.CRUSHED_ZINC::get, 1);
   BaseRecipeProvider.GeneratedRecipe OSMIUM_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.OSMIUM, AllItems.CRUSHED_OSMIUM::get);
   BaseRecipeProvider.GeneratedRecipe PLATINUM_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.PLATINUM, AllItems.CRUSHED_PLATINUM::get);
   BaseRecipeProvider.GeneratedRecipe SILVER_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.SILVER, AllItems.CRUSHED_SILVER::get);
   BaseRecipeProvider.GeneratedRecipe TIN_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.TIN, AllItems.CRUSHED_TIN::get);
   BaseRecipeProvider.GeneratedRecipe QUICKSILVER_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.QUICKSILVER, AllItems.CRUSHED_QUICKSILVER::get);
   BaseRecipeProvider.GeneratedRecipe LEAD_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.LEAD, AllItems.CRUSHED_LEAD::get);
   BaseRecipeProvider.GeneratedRecipe ALUMINUM_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.ALUMINUM, AllItems.CRUSHED_BAUXITE::get);
   BaseRecipeProvider.GeneratedRecipe URANIUM_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.URANIUM, AllItems.CRUSHED_URANIUM::get);
   BaseRecipeProvider.GeneratedRecipe NICKEL_RAW_BLOCK = this.moddedRawOreBlock(CommonMetal.NICKEL, AllItems.CRUSHED_NICKEL::get);
   BaseRecipeProvider.GeneratedRecipe BWG_RED_ROCK_ORE = this.create(
      Mods.BWG.recipeId("red_rock"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     150
                  ))
                  .require(Mods.BWG, "red_rock"))
               .output(1.0F, Items.RED_SAND, 1))
            .whenModLoaded(Mods.BWG.getId())
   );
   BaseRecipeProvider.GeneratedRecipe DC_AMBER_ORE = this.create(
      Mods.DRUIDCRAFT.recipeId("amber_ore"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           300
                        ))
                        .require(Mods.DRUIDCRAFT, "amber_ore"))
                     .output(1.0F, Mods.DRUIDCRAFT, "amber", 2))
                  .output(0.5F, Mods.DRUIDCRAFT, "amber", 1))
               .output(0.125F, Items.COBBLESTONE, 1))
            .whenModLoaded(Mods.DRUIDCRAFT.getId())
   );
   BaseRecipeProvider.GeneratedRecipe DC_FIERY_GLASS_ORE = this.create(
      Mods.DRUIDCRAFT.recipeId("fiery_glass_ore"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           300
                        ))
                        .require(Mods.DRUIDCRAFT, "fiery_glass_ore"))
                     .output(1.0F, Mods.DRUIDCRAFT, "fiery_glass", 8))
                  .output(0.25F, Mods.DRUIDCRAFT, "fiery_glass", 6))
               .output(0.125F, Items.COBBLESTONE, 1))
            .whenModLoaded(Mods.DRUIDCRAFT.getId())
   );
   BaseRecipeProvider.GeneratedRecipe DC_MOONSTONE_ORE = this.create(
      Mods.DRUIDCRAFT.recipeId("moonstone_ore"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           300
                        ))
                        .require(Mods.DRUIDCRAFT, "moonstone_ore"))
                     .output(1.0F, Mods.DRUIDCRAFT, "moonstone", 2))
                  .output(0.5F, Mods.DRUIDCRAFT, "moonstone", 1))
               .output(0.125F, Items.COBBLESTONE, 1))
            .whenModLoaded(Mods.DRUIDCRAFT.getId())
   );
   BaseRecipeProvider.GeneratedRecipe NEA_ICE = this.create(
      Mods.NEA.recipeId("ice"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        100
                     ))
                     .require(Items.ICE))
                  .output(1.0F, Mods.NEA, "ice_cubes", 3))
               .output(0.25F, Mods.NEA, "ice_cubes", 3))
            .whenModLoaded(Mods.NEA.getId())
   );
   BaseRecipeProvider.GeneratedRecipe Q_MOSS = this.create(
      Mods.Q.recipeId("moss_block"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        50
                     ))
                     .require(Items.MOSS_BLOCK))
                  .output(1.0F, Mods.Q, "moss_paste", 2))
               .output(0.1F, Mods.Q, "moss_paste", 1))
            .whenModLoaded(Mods.Q.getId())
   );
   BaseRecipeProvider.GeneratedRecipe SG = this.sgOres(
      "peridot",
      "ruby",
      "sapphire",
      "topaz",
      "alexandrite",
      "black_diamond",
      "carnelian",
      "citrine",
      "iolite",
      "moldavite",
      "turquoise",
      "ammolite",
      "kyanite",
      "rose_quartz",
      "heliodor",
      "white_diamond",
      "garnet",
      "aquamarine",
      "tanzanite",
      "opal",
      "pearl"
   );
   BaseRecipeProvider.GeneratedRecipe SF = this.sfPlants("barley", "oat", "rice", "rye");
   BaseRecipeProvider.GeneratedRecipe TH = this.thOres("apatite", "cinnabar", "niter", "sulfur");
   BaseRecipeProvider.GeneratedRecipe GS_ALLURITE = this.create(
      Mods.GS.recipeId("allurite"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     300
                  ))
                  .require(AllTags.AllItemTags.ALLURITE.tag))
               .output(0.8F, Mods.GS, "allurite_shard", 4))
            .whenModLoaded(Mods.GS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe GS_LUMIERE = this.create(
      Mods.GS.recipeId("lumiere"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     300
                  ))
                  .require(AllTags.AllItemTags.LUMIERE.tag))
               .output(0.8F, Mods.GS, "lumiere_shard", 4))
            .whenModLoaded(Mods.GS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe GS_AMETHYST = this.create(
      Mods.GS.recipeId("amethyst"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     300
                  ))
                  .require(AllTags.AllItemTags.AMETHYST.tag))
               .output(0.8F, Items.AMETHYST_SHARD, 4))
            .whenModLoaded(Mods.GS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe EO_COAL_NETHER = this.eoNetherOre("coal", Items.COAL, 1);
   BaseRecipeProvider.GeneratedRecipe EO_COPPER_NETHER = this.eoNetherOre("copper", (ItemLike)AllItems.CRUSHED_COPPER.get(), 5);
   BaseRecipeProvider.GeneratedRecipe EO_IRON_NETHER = this.eoNetherOre("iron", (ItemLike)AllItems.CRUSHED_IRON.get(), 1);
   BaseRecipeProvider.GeneratedRecipe EO_EMERALD_NETHER = this.eoNetherOre("emerald", Items.EMERALD, 1);
   BaseRecipeProvider.GeneratedRecipe EO_LAPIS_NETHER = this.eoNetherOre("lapis", Items.LAPIS_LAZULI, 10);
   BaseRecipeProvider.GeneratedRecipe EO_DIAMOND_NETHER = this.eoNetherOre("diamond", Items.DIAMOND, 1);
   BaseRecipeProvider.GeneratedRecipe EO_GHAST_NETHER = this.eoNetherOre("ghast", Items.GHAST_TEAR, 1);
   BaseRecipeProvider.GeneratedRecipe EO_COAL_END = this.eoEndOre("coal", Items.COAL, 1);
   BaseRecipeProvider.GeneratedRecipe EO_COPPER_END = this.eoEndOre("copper", (ItemLike)AllItems.CRUSHED_COPPER.get(), 5);
   BaseRecipeProvider.GeneratedRecipe EO_EMERALD_END = this.eoEndOre("emerald", Items.EMERALD, 1);
   BaseRecipeProvider.GeneratedRecipe EO_LAPIS_END = this.eoEndOre("lapis", Items.LAPIS_LAZULI, 10);
   BaseRecipeProvider.GeneratedRecipe EO_DIAMOND_END = this.eoEndOre("diamond", Items.DIAMOND, 1);
   BaseRecipeProvider.GeneratedRecipe EO_REDSTONE_END = this.eoEndOre("redstone", Items.REDSTONE, 6);
   BaseRecipeProvider.GeneratedRecipe EO_ENDER_END = this.eoEndOre("ender", Items.ENDER_PEARL, 1);
   BaseRecipeProvider.GeneratedRecipe ENS_STONES = this.ensStones("andesite", "diorite", "end_stone", "granite", "netherrack");
   BaseRecipeProvider.GeneratedRecipe ENS_DUST = this.create(
      Mods.ENS.recipeId("dust"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(Blocks.SAND))
               .output(Mods.ENS, "dust"))
            .whenModLoaded(Mods.ENS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ENS_NETHERRACK = this.create(
      Mods.ENS.recipeId("crushed_netherrack"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        100
                     ))
                     .require(Mods.ENS, "crushed_netherrack"))
                  .output((ItemLike)AllItems.CINDER_FLOUR.get()))
               .output(0.5F, (ItemLike)AllItems.CINDER_FLOUR.get()))
            .whenModLoaded(Mods.ENS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ENS_DIORITE = this.create(
      Mods.ENS.recipeId("crushed_diorite"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     100
                  ))
                  .require(Mods.ENS, "crushed_diorite"))
               .output(0.25F, Items.QUARTZ, 1))
            .whenModLoaded(Mods.ENS.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AET_ZANITE = this.create(
      Mods.AET.recipeId("zanite_ore"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              350
                           ))
                           .require(Mods.AET, "zanite_ore"))
                        .output(Mods.AET, "zanite_gemstone"))
                     .output(0.75F, Mods.AET, "zanite_gemstone", 1))
                  .output(0.125F, Mods.AET, "holystone", 1))
               .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
            .whenModLoaded(Mods.AET.getId())
   );
   BaseRecipeProvider.GeneratedRecipe AET_AMBROSIUM = this.create(
      Mods.AET.recipeId("ambrosium_ore"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              150
                           ))
                           .require(Mods.AET, "ambrosium_ore"))
                        .output(Mods.AET, "ambrosium_shard"))
                     .output(0.75F, Mods.AET, "ambrosium_shard", 1))
                  .output(0.125F, Mods.AET, "holystone", 1))
               .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
            .whenModLoaded(Mods.AET.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IE_COKE_DUST = this.create(
      Mods.IE.recipeId("coal_coke"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(Mods.IE, "coal_coke"))
               .output(Mods.IE, "dust_coke"))
            .whenModLoaded(Mods.IE.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IE_COKE_BLOCK = this.create(
      Mods.IE.recipeId("coke_block"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(Mods.IE, "coke"))
               .output(1.0F, Mods.IE.asResource("dust_coke"), 9))
            .whenModLoaded(Mods.IE.getId())
   );
   BaseRecipeProvider.GeneratedRecipe IE_SLAG_GRAVEL = this.create(
      Mods.IE.recipeId("slag"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                     200
                  ))
                  .require(Mods.IE, "slag"))
               .output(Mods.IE, "slag_gravel"))
            .whenModLoaded(Mods.IE.getId())
   );
   BaseRecipeProvider.GeneratedRecipe BOP_ROSE_QUARTZ = this.create(
      Mods.BOP.recipeId("rose_quartz"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        150
                     ))
                     .require(Mods.BOP, "rose_quartz_cluster"))
                  .output(1.0F, Mods.BOP, "rose_quartz_chunk", 7))
               .output(0.5F, Mods.BOP, "rose_quartz_chunk", 1))
            .whenModLoaded(Mods.BOP.getId())
   );

   BaseRecipeProvider.GeneratedRecipe sgOres(String... types) {
      for (String type : types) {
         this.create(
            Mods.SILENT_GEMS.recipeId(type + "_ore"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                    350
                                 ))
                                 .require(Mods.SILENT_GEMS, type + "_ore"))
                              .output(1.0F, Mods.SILENT_GEMS, type, 2))
                           .output(0.25F, Mods.SILENT_GEMS, type, 1))
                        .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                     .output(0.12F, Items.COBBLESTONE))
                  .whenModLoaded(Mods.SILENT_GEMS.getId())
         );
         this.create(
            Mods.SILENT_GEMS.recipeId("deepslate_" + type + "_ore"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                    350
                                 ))
                                 .require(Mods.SILENT_GEMS, "deepslate_" + type + "_ore"))
                              .output(1.0F, Mods.SILENT_GEMS, type, 2))
                           .output(0.25F, Mods.SILENT_GEMS, type, 1))
                        .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                     .output(0.12F, Items.COBBLED_DEEPSLATE))
                  .whenModLoaded(Mods.SILENT_GEMS.getId())
         );
         this.create(
            Mods.SILENT_GEMS.recipeId("nether_" + type + "_ore"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                    350
                                 ))
                                 .require(Mods.SILENT_GEMS, "nether_" + type + "_ore"))
                              .output(1.0F, Mods.SILENT_GEMS, type, 2))
                           .output(0.25F, Mods.SILENT_GEMS, type, 1))
                        .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                     .output(0.12F, Items.NETHERRACK))
                  .whenModLoaded(Mods.SILENT_GEMS.getId())
         );
         this.create(
            Mods.SILENT_GEMS.recipeId("end_" + type + "_ore"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                    350
                                 ))
                                 .require(Mods.SILENT_GEMS, "end_" + type + "_ore"))
                              .output(1.0F, Mods.SILENT_GEMS, type, 2))
                           .output(0.25F, Mods.SILENT_GEMS, type, 1))
                        .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                     .output(0.12F, Items.END_STONE))
                  .whenModLoaded(Mods.SILENT_GEMS.getId())
         );
      }

      return null;
   }

   BaseRecipeProvider.GeneratedRecipe sfPlants(String... types) {
      for (String type : types) {
         this.create(
            Mods.SF.recipeId(type),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 150
                              ))
                              .require(Mods.SF, type))
                           .output(1.0F, (ItemLike)AllItems.WHEAT_FLOUR.get(), 1))
                        .output(0.25F, (ItemLike)AllItems.WHEAT_FLOUR.get(), 2))
                     .output(0.25F, Mods.SF, type + "_seeds", 1))
                  .whenModLoaded(Mods.SF.getId())
         );
      }

      return null;
   }

   BaseRecipeProvider.GeneratedRecipe thOres(String... types) {
      for (String type : types) {
         this.create(
            Mods.TH.recipeId(type + "_ore"),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                    350
                                 ))
                                 .require(Mods.TH, type + "_ore"))
                              .output(1.0F, Mods.TH, type, 2))
                           .output(0.25F, Mods.TH, type, 1))
                        .output(0.12F, Items.COBBLESTONE))
                     .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                  .whenModLoaded(Mods.TH.getId())
         );
      }

      return null;
   }

   BaseRecipeProvider.GeneratedRecipe eoNetherOre(String material, ItemLike result, int count) {
      String oreName = "ore_" + material + "_nether";
      return this.create(
         Mods.EO.recipeId(oreName),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 350
                              ))
                              .require(Mods.EO, oreName))
                           .output(1.0F, result, count))
                        .output(0.25F, result))
                     .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                  .output(0.12F, Items.NETHERRACK))
               .whenModLoaded(Mods.EO.getId())
      );
   }

   BaseRecipeProvider.GeneratedRecipe eoEndOre(String material, ItemLike result, int count) {
      String oreName = "ore_" + material + "_end";
      return this.create(
         Mods.EO.recipeId(oreName),
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                                 350
                              ))
                              .require(Mods.EO, oreName))
                           .output(1.0F, result, count))
                        .output(0.25F, result))
                     .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get()))
                  .output(0.12F, Items.END_STONE))
               .whenModLoaded(Mods.EO.getId())
      );
   }

   BaseRecipeProvider.GeneratedRecipe ensStones(String... stones) {
      for (String stone : stones) {
         String crushed = "crushed_" + stone;
         this.create(
            Mods.ENS.recipeId(stone),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           350
                        ))
                        .require(Mods.MC, stone))
                     .output(Mods.ENS, crushed))
                  .whenModLoaded(Mods.ENS.getId())
         );
      }

      return null;
   }

   public CreateCrushingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }

   BaseRecipeProvider.GeneratedRecipe ensMineralRecycling(AllPaletteStoneTypes type, UnaryOperator<StandardProcessingRecipe.Builder<CrushingRecipe>> transform) {
      this.create(Lang.asId(type.name()) + "_recycling", b -> transform.apply((StandardProcessingRecipe.Builder<CrushingRecipe>)b.require(type.materialTag)));
      return this.create(type.getBaseBlock()::get, b -> transform.apply((StandardProcessingRecipe.Builder<CrushingRecipe>)b.whenModMissing(Mods.ENS.getId())));
   }
}
