package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.tterrag.registrate.util.entry.ItemEntry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public abstract class WashingRecipeGen extends StandardProcessingRecipeGen<SplashingRecipe> {
   public BaseRecipeProvider.GeneratedRecipe convert(Block block, Block result) {
      return this.create(() -> block, b -> (StandardProcessingRecipe.Builder)b.output(result));
   }

   public BaseRecipeProvider.GeneratedRecipe crushedOre(ItemEntry<Item> crushed, Supplier<ItemLike> nugget, Supplier<ItemLike> secondary, float secondaryChance) {
      return this.create(
         crushed::get,
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.output(nugget.get(), 9)).output(secondaryChance, secondary.get(), 1)
      );
   }

   protected BaseRecipeProvider.GeneratedRecipe simpleModded(DatagenMod mod, String input, String output) {
      return this.create(
         mod.getId() + "/" + output,
         b -> (StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)((StandardProcessingRecipe.Builder)b.require(mod, input))
                  .output(mod, output))
               .whenModLoaded(mod.getId())
      );
   }

   public WashingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.SPLASHING;
   }
}
