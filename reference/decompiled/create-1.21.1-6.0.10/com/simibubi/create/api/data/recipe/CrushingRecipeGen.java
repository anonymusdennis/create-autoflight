package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.data.recipe.CommonMetal;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.conditions.TagEmptyCondition;

public abstract class CrushingRecipeGen extends StandardProcessingRecipeGen<CrushingRecipe> {
   protected BaseRecipeProvider.GeneratedRecipe mineralRecycling(AllPaletteStoneTypes type, Supplier<ItemLike> crushed, Supplier<ItemLike> nugget, float chance) {
      return this.mineralRecycling(
         type,
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(250))
                  .output(chance, crushed.get(), 1))
               .output(chance, nugget.get(), 1)
      );
   }

   protected BaseRecipeProvider.GeneratedRecipe mineralRecycling(
      AllPaletteStoneTypes type, UnaryOperator<StandardProcessingRecipe.Builder<CrushingRecipe>> transform
   ) {
      this.create(Lang.asId(type.name()) + "_recycling", b -> transform.apply((StandardProcessingRecipe.Builder<CrushingRecipe>)b.require(type.materialTag)));
      return this.create(type.getBaseBlock()::get, transform);
   }

   protected BaseRecipeProvider.GeneratedRecipe stoneOre(Supplier<ItemLike> ore, Supplier<ItemLike> raw, float expectedAmount, int duration) {
      return this.ore(Blocks.COBBLESTONE, ore, raw, expectedAmount, duration);
   }

   protected BaseRecipeProvider.GeneratedRecipe deepslateOre(Supplier<ItemLike> ore, Supplier<ItemLike> raw, float expectedAmount, int duration) {
      return this.ore(Blocks.COBBLED_DEEPSLATE, ore, raw, expectedAmount, duration);
   }

   protected BaseRecipeProvider.GeneratedRecipe netherOre(Supplier<ItemLike> ore, Supplier<ItemLike> raw, float expectedAmount, int duration) {
      return this.ore(Blocks.NETHERRACK, ore, raw, expectedAmount, duration);
   }

   protected BaseRecipeProvider.GeneratedRecipe ore(ItemLike stoneType, Supplier<ItemLike> ore, Supplier<ItemLike> raw, float expectedAmount, int duration) {
      return this.create(ore, b -> {
         ((StandardProcessingRecipe.Builder)b.duration(duration)).output(raw.get(), Mth.floor(expectedAmount));
         float extra = expectedAmount - (float)Mth.floor(expectedAmount);
         if (extra > 0.0F) {
            b.output(extra, raw.get(), 1);
         }

         b.output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get(), raw.get() == AllItems.CRUSHED_GOLD.get() ? 2 : 1);
         return (StandardProcessingRecipe.Builder)b.output(0.125F, stoneType);
      });
   }

   protected BaseRecipeProvider.GeneratedRecipe moddedOre(CommonMetal metal, Supplier<ItemLike> result) {
      TagKey<Item> tag = metal.ores.items();
      return this.create(
         metal + "_ore",
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                              400
                           ))
                           .withCondition(new NotCondition(new TagEmptyCondition(tag.location()))))
                        .require(tag))
                     .output(result.get(), 1))
                  .output(0.75F, result.get(), 1))
               .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get())
      );
   }

   protected BaseRecipeProvider.GeneratedRecipe rawOre(String metalName, Supplier<TagKey<Item>> input, Supplier<ItemLike> result, int xpMult) {
      return this.rawOre(metalName, input, result, false, xpMult);
   }

   protected BaseRecipeProvider.GeneratedRecipe rawOreBlock(String metalName, Supplier<TagKey<Item>> input, Supplier<ItemLike> result, int xpMult) {
      return this.rawOre(metalName, input, result, true, xpMult);
   }

   protected BaseRecipeProvider.GeneratedRecipe rawOre(String metalName, Supplier<TagKey<Item>> input, Supplier<ItemLike> result, boolean block, int xpMult) {
      return this.create(
         "raw_" + metalName + (block ? "_block" : ""),
         b -> {
            int amount = block ? 9 : 1;
            return (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                        400
                     ))
                     .require(input.get()))
                  .output(result.get(), amount))
               .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get(), amount * xpMult);
         }
      );
   }

   protected BaseRecipeProvider.GeneratedRecipe moddedRawOre(CommonMetal metal, Supplier<ItemLike> result) {
      return this.moddedRawOre(metal, result, false);
   }

   protected BaseRecipeProvider.GeneratedRecipe moddedRawOreBlock(CommonMetal metal, Supplier<ItemLike> result) {
      return this.moddedRawOre(metal, result, true);
   }

   protected BaseRecipeProvider.GeneratedRecipe moddedRawOre(CommonMetal metal, Supplier<ItemLike> result, boolean block) {
      return this.create(
         "raw_" + metal + (block ? "_block" : ""),
         b -> {
            int amount = block ? 9 : 1;
            TagKey<Item> material = block ? metal.rawStorageBlocks.items() : metal.rawOres;
            return (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.duration(
                           400
                        ))
                        .withCondition(new NotCondition(new TagEmptyCondition(material.location()))))
                     .require(material))
                  .output(result.get(), amount))
               .output(0.75F, (ItemLike)AllItems.EXP_NUGGET.get(), amount);
         }
      );
   }

   public CrushingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.CRUSHING;
   }
}
