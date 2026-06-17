package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.WashingRecipeGen;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.tterrag.registrate.util.entry.ItemEntry;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public final class CreateWashingRecipeGen extends WashingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe WOOL = this.create(
      "wool", b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(ItemTags.WOOL)).output(Items.WHITE_WOOL)
   );
   BaseRecipeProvider.GeneratedRecipe STAINED_GLASS = this.create(
      "stained_glass",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(net.neoforged.neoforge.common.Tags.Items.GLASS_BLOCKS))
            .output(Items.GLASS)
   );
   BaseRecipeProvider.GeneratedRecipe STAINED_GLASS_PANE = this.create(
      "stained_glass_pane",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(net.neoforged.neoforge.common.Tags.Items.GLASS_PANES))
            .output(Items.GLASS_PANE)
   );
   BaseRecipeProvider.GeneratedRecipe GRAVEL = this.create(
      () -> Blocks.GRAVEL,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.output(0.25F, Items.FLINT)).output(0.125F, Items.IRON_NUGGET)
   );
   BaseRecipeProvider.GeneratedRecipe SOUL_SAND = this.create(
      () -> Blocks.SOUL_SAND,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.output(0.125F, Items.QUARTZ, 4)).output(0.02F, Items.GOLD_NUGGET)
   );
   BaseRecipeProvider.GeneratedRecipe RED_SAND = this.create(
      () -> Blocks.RED_SAND,
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.output(0.125F, Items.GOLD_NUGGET, 3)).output(0.05F, Items.DEAD_BUSH)
   );
   BaseRecipeProvider.GeneratedRecipe SAND = this.create(() -> Blocks.SAND, b -> (StandardProcessingRecipe.Builder)b.output(0.25F, Items.CLAY_BALL));
   BaseRecipeProvider.GeneratedRecipe WEATHERED_IRON_BLOCK = this.create(
      AllBlocks.INDUSTRIAL_IRON_BLOCK::get, b -> (StandardProcessingRecipe.Builder)b.output(AllBlocks.WEATHERED_IRON_BLOCK)
   );
   BaseRecipeProvider.GeneratedRecipe WEATHERED_IRON_WINDOW = this.create(
      AllPaletteBlocks.INDUSTRIAL_IRON_WINDOW::get, b -> (StandardProcessingRecipe.Builder)b.output(AllPaletteBlocks.WEATHERED_IRON_WINDOW)
   );
   BaseRecipeProvider.GeneratedRecipe WEATHERED_IRON_WINDOW_PANE = this.create(
      AllPaletteBlocks.INDUSTRIAL_IRON_WINDOW_PANE::get, b -> (StandardProcessingRecipe.Builder)b.output(AllPaletteBlocks.WEATHERED_IRON_WINDOW_PANE)
   );
   BaseRecipeProvider.GeneratedRecipe CRUSHED_COPPER = this.crushedOre(AllItems.CRUSHED_COPPER, AllItems.COPPER_NUGGET::get, () -> Items.CLAY_BALL, 0.5F);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_ZINC = this.crushedOre(AllItems.CRUSHED_ZINC, AllItems.ZINC_NUGGET::get, () -> Items.GUNPOWDER, 0.25F);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_GOLD = this.crushedOre(AllItems.CRUSHED_GOLD, () -> Items.GOLD_NUGGET, () -> Items.QUARTZ, 0.5F);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_IRON = this.crushedOre(AllItems.CRUSHED_IRON, () -> Items.IRON_NUGGET, () -> Items.REDSTONE, 0.75F);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_OSMIUM = this.moddedCrushedOre(AllItems.CRUSHED_OSMIUM, CommonMetal.OSMIUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_PLATINUM = this.moddedCrushedOre(AllItems.CRUSHED_PLATINUM, CommonMetal.PLATINUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_SILVER = this.moddedCrushedOre(AllItems.CRUSHED_SILVER, CommonMetal.SILVER);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_TIN = this.moddedCrushedOre(AllItems.CRUSHED_TIN, CommonMetal.TIN);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_LEAD = this.moddedCrushedOre(AllItems.CRUSHED_LEAD, CommonMetal.LEAD);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_QUICKSILVER = this.moddedCrushedOre(AllItems.CRUSHED_QUICKSILVER, CommonMetal.QUICKSILVER);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_BAUXITE = this.moddedCrushedOre(AllItems.CRUSHED_BAUXITE, CommonMetal.ALUMINUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_URANIUM = this.moddedCrushedOre(AllItems.CRUSHED_URANIUM, CommonMetal.URANIUM);
   BaseRecipeProvider.GeneratedRecipe CRUSHED_NICKEL = this.moddedCrushedOre(AllItems.CRUSHED_NICKEL, CommonMetal.NICKEL);
   BaseRecipeProvider.GeneratedRecipe ICE = this.convert(Blocks.ICE, Blocks.PACKED_ICE);
   BaseRecipeProvider.GeneratedRecipe MAGMA_BLOCK = this.convert(Blocks.MAGMA_BLOCK, Blocks.OBSIDIAN);
   BaseRecipeProvider.GeneratedRecipe FLOUR = this.create(
      "wheat_flour",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(CreateRecipeProvider.I.wheatFlour()))
            .output((ItemLike)AllItems.DOUGH.get())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_SAND = this.create(
      "atmospheric/arid_sand",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     Mods.ATM, "arid_sand"
                  ))
                  .output(0.25F, Items.CLAY_BALL, 1))
               .output(0.05F, Mods.ATM, "aloe_kernels", 1))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ATMO_RED_SAND = this.create(
      "atmospheric/red_arid_sand",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     Mods.ATM, "red_arid_sand"
                  ))
                  .output(0.125F, Items.CLAY_BALL, 4))
               .output(0.05F, Mods.ATM, "aloe_kernels", 1))
            .whenModLoaded(Mods.ATM.getId())
   );
   BaseRecipeProvider.GeneratedRecipe ENDER_END = this.simpleModded(Mods.ENDER, "end_corrock", "petrified_end_corrock");
   BaseRecipeProvider.GeneratedRecipe ENDER_END_BLOCK = this.simpleModded(Mods.ENDER, "end_corrock_block", "petrified_end_corrock_block");
   BaseRecipeProvider.GeneratedRecipe ENDER_END_CROWN = this.simpleModded(Mods.ENDER, "end_corrock_crown", "petrified_end_corrock_crown");
   BaseRecipeProvider.GeneratedRecipe ENDER_NETHER = this.simpleModded(Mods.ENDER, "nether_corrock", "petrified_nether_corrock");
   BaseRecipeProvider.GeneratedRecipe ENDER_NETHER_BLOCK = this.simpleModded(Mods.ENDER, "nether_corrock_block", "petrified_nether_corrock_block");
   BaseRecipeProvider.GeneratedRecipe ENDER_NETHER_CROWN = this.simpleModded(Mods.ENDER, "nether_corrock_crown", "petrified_nether_corrock_crown");
   BaseRecipeProvider.GeneratedRecipe ENDER_OVERWORLD = this.simpleModded(Mods.ENDER, "overworld_corrock", "petrified_overworld_corrock");
   BaseRecipeProvider.GeneratedRecipe ENDER_OVERWORLD_BLOCK = this.simpleModded(Mods.ENDER, "overworld_corrock_block", "petrified_overworld_corrock_block");
   BaseRecipeProvider.GeneratedRecipe ENDER_OVERWORLD_CROWN = this.simpleModded(Mods.ENDER, "overworld_corrock_crown", "petrified_overworld_corrock_crown");
   BaseRecipeProvider.GeneratedRecipe Q = this.simpleModded(Mods.Q, "iron_plate", "rusty_iron_plate");
   BaseRecipeProvider.GeneratedRecipe SUP = this.simpleModded(Mods.SUP, "blackboard", "blackboard");
   BaseRecipeProvider.GeneratedRecipe VH = this.simpleModded(Mods.VH, "ornate_chain", "ornate_chain_rusty");

   public CreateWashingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }

   public BaseRecipeProvider.GeneratedRecipe moddedCrushedOre(ItemEntry<? extends Item> crushed, CommonMetal metal) {
      for (Mods mod : metal.mods) {
         String metalName = metal.getName(mod);
         ResourceLocation nugget = mod.nuggetOf(metalName);
         this.create(
            mod.getId() + "/" + crushed.getId().getPath(),
            b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.withItemIngredients(
                        new Ingredient[]{Ingredient.of(new ItemLike[]{crushed::get})}
                     ))
                     .output(1.0F, nugget, 9))
                  .whenModLoaded(mod.getId())
         );
      }

      return null;
   }
}
