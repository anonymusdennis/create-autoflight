package com.simibubi.create.foundation.data.recipe;

import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.BaseRecipeProvider;
import com.simibubi.create.api.data.recipe.CompactingRecipeGen;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

public final class CreateCompactingRecipeGen extends CompactingRecipeGen {
   BaseRecipeProvider.GeneratedRecipe GRANITE = this.create(
      "granite_from_flint",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Items.FLINT
                     ))
                     .require(Items.FLINT))
                  .require(Fluids.LAVA, 100))
               .require(Items.RED_SAND))
            .output(Blocks.GRANITE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe DIORITE = this.create(
      "diorite_from_flint",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Items.FLINT
                     ))
                     .require(Items.FLINT))
                  .require(Fluids.LAVA, 100))
               .require(Items.CALCITE))
            .output(Blocks.DIORITE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ANDESITE = this.create(
      "andesite_from_flint",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                        Items.FLINT
                     ))
                     .require(Items.FLINT))
                  .require(Fluids.LAVA, 100))
               .require(Items.GRAVEL))
            .output(Blocks.ANDESITE, 1)
   );
   BaseRecipeProvider.GeneratedRecipe CHOCOLATE = this.create(
      "chocolate",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require((FlowingFluid)AllFluids.CHOCOLATE.get(), 250))
            .output((ItemLike)AllItems.BAR_OF_CHOCOLATE.get(), 1)
   );
   BaseRecipeProvider.GeneratedRecipe BLAZE_CAKE = this.create(
      "blaze_cake",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                     net.neoforged.neoforge.common.Tags.Items.EGGS
                  ))
                  .require(Items.SUGAR))
               .require((ItemLike)AllItems.CINDER_FLOUR.get()))
            .output((ItemLike)AllItems.BLAZE_CAKE_BASE.get(), 1)
   );
   BaseRecipeProvider.GeneratedRecipe HONEY = this.create(
      "honey",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(net.neoforged.neoforge.common.Tags.Fluids.HONEY, 1000))
            .output(Items.HONEY_BLOCK, 1)
   );
   BaseRecipeProvider.GeneratedRecipe ICE = this.create(
      "ice",
      b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(
                                       Blocks.SNOW_BLOCK
                                    ))
                                    .require(Blocks.SNOW_BLOCK))
                                 .require(Blocks.SNOW_BLOCK))
                              .require(Blocks.SNOW_BLOCK))
                           .require(Blocks.SNOW_BLOCK))
                        .require(Blocks.SNOW_BLOCK))
                     .require(Blocks.SNOW_BLOCK))
                  .require(Blocks.SNOW_BLOCK))
               .require(Blocks.SNOW_BLOCK))
            .output(Blocks.ICE)
   );

   public CreateCompactingRecipeGen(PackOutput output, CompletableFuture<Provider> registries) {
      super(output, registries, "create");
   }
}
