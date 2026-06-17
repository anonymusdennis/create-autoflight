package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.MixingRecipeGen;
import com.simibubi.create.content.processing.recipe.HeatCondition;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.common.crafting.BlockTagIngredient;

public final class CreateMixingRecipeGen extends MixingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe TEMP_LAVA = this.create(
      "lava_from_cobble",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Items.COBBLESTONES))
               .output(Fluids.LAVA, 50))
            .requiresHeat(HeatCondition.SUPERHEATED)
   );
   BaseRecipeProvider.GeneratedRecipe TEA = this.create(
      "tea",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Fluids.WATER, 250
                     ))
                     .require(net.neoforged.neoforge.common.Tags.Fluids.MILK, 250))
                  .require(ItemTags.LEAVES))
               .output((Fluid)AllFluids.TEA.get(), 500))
            .requiresHeat(HeatCondition.HEATED)
   );
   BaseRecipeProvider.GeneratedRecipe CHOCOLATE = this.create(
      "chocolate",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        net.neoforged.neoforge.common.Tags.Fluids.MILK, 250
                     ))
                     .require(net.minecraft.world.item.Items.SUGAR))
                  .require(net.minecraft.world.item.Items.COCOA_BEANS))
               .output((Fluid)AllFluids.CHOCOLATE.get(), 250))
            .requiresHeat(HeatCondition.HEATED)
   );
   BaseRecipeProvider.GeneratedRecipe CHOCOLATE_MELTING = this.create(
      "chocolate_melting",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  (ItemLike)AllItems.BAR_OF_CHOCOLATE.get()
               ))
               .output((Fluid)AllFluids.CHOCOLATE.get(), 250))
            .requiresHeat(HeatCondition.HEATED)
   );
   BaseRecipeProvider.GeneratedRecipe HONEY = this.create(
      "honey",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  net.minecraft.world.item.Items.HONEY_BLOCK
               ))
               .output((Fluid)AllFluids.HONEY.get(), 1000))
            .requiresHeat(HeatCondition.HEATED)
   );
   BaseRecipeProvider.GeneratedRecipe DOUGH = this.create(
      "dough_by_mixing",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  CreateRecipeProvider.I.wheatFlour()
               ))
               .require(Fluids.WATER, 1000))
            .output((ItemLike)AllItems.DOUGH.get(), 1)
   );
   BaseRecipeProvider.GeneratedRecipe BRASS_INGOT = this.create(
      "brass_ingot",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     CreateRecipeProvider.I.copper()
                  ))
                  .require(CreateRecipeProvider.I.zinc()))
               .output((ItemLike)AllItems.BRASS_INGOT.get(), 2))
            .requiresHeat(HeatCondition.HEATED)
   );
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY = this.create(
      "andesite_alloy",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.ANDESITE))
               .require(CreateRecipeProvider.I.ironNugget()))
            .output(CreateRecipeProvider.I.andesiteAlloy(), 1)
   );
   BaseRecipeProvider.GeneratedRecipe ANDESITE_ALLOY_FROM_ZINC = this.create(
      "andesite_alloy_from_zinc",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(Blocks.ANDESITE))
               .require(CreateRecipeProvider.I.zincNugget()))
            .output(CreateRecipeProvider.I.andesiteAlloy(), 1)
   );
   BaseRecipeProvider.GeneratedRecipe MUD = this.create(
      "mud_by_mixing",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                  new BlockTagIngredient(BlockTags.CONVERTABLE_TO_MUD)
               ))
               .require(Fluids.WATER, 250))
            .output(Blocks.MUD, 1)
   );
   BaseRecipeProvider.GeneratedRecipe PULP = this.create(
      "cardboard_pulp",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                           AllTags.AllItemTags.PULPIFIABLE.tag
                        ))
                        .require(AllTags.AllItemTags.PULPIFIABLE.tag))
                     .require(AllTags.AllItemTags.PULPIFIABLE.tag))
                  .require(AllTags.AllItemTags.PULPIFIABLE.tag))
               .require(Fluids.WATER, 250))
            .output(AllItems.PULP, 1)
   );
   BaseRecipeProvider.GeneratedRecipe AE2_FLUIX = this.create(
      Mods.AE2.recipeId("fluix_crystal"),
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                           Items.DUSTS_REDSTONE
                        ))
                        .require(Fluids.WATER, 250))
                     .require(Mods.AE2, "charged_certus_quartz_crystal"))
                  .require(Items.GEMS_QUARTZ))
               .output(1.0F, Mods.AE2, "fluix_crystal", 2))
            .whenModLoaded(Mods.AE2.getId())
   );
   BaseRecipeProvider.GeneratedRecipe RU_PEAT_MUD = this.moddedMud(Mods.RU, "peat");
   BaseRecipeProvider.GeneratedRecipe RU_SILT_MUD = this.moddedMud(Mods.RU, "silt");

   public CreateMixingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
