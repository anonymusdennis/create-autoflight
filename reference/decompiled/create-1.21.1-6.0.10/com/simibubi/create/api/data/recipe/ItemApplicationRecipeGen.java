package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipeParams;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags.Items;

public abstract class ItemApplicationRecipeGen
   extends ProcessingRecipeGen<ItemApplicationRecipeParams, ManualApplicationRecipe, ItemApplicationRecipe.Builder<ManualApplicationRecipe>> {
   protected BaseRecipeProvider.GeneratedRecipe woodCasing(String type, Supplier<ItemLike> ingredient, Supplier<ItemLike> output) {
      return this.woodCasingIngredient(type, () -> Ingredient.of(new ItemLike[]{ingredient.get()}), output);
   }

   protected BaseRecipeProvider.GeneratedRecipe woodCasingTag(String type, Supplier<TagKey<Item>> ingredient, Supplier<ItemLike> output) {
      return this.woodCasingIngredient(type, () -> Ingredient.of(ingredient.get()), output);
   }

   protected BaseRecipeProvider.GeneratedRecipe woodCasingIngredient(String type, Supplier<Ingredient> ingredient, Supplier<ItemLike> output) {
      this.create(
         type + "_casing_from_log",
         b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(Items.STRIPPED_LOGS))
                  .require(ingredient.get()))
               .output(output.get())
      );
      return this.create(
         type + "_casing_from_wood",
         b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(Items.STRIPPED_WOODS))
                  .require(ingredient.get()))
               .output(output.get())
      );
   }

   public ItemApplicationRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.ITEM_APPLICATION;
   }

   protected ItemApplicationRecipe.Builder<ManualApplicationRecipe> getBuilder(ResourceLocation id) {
      return new ItemApplicationRecipe.Builder<>(ManualApplicationRecipe::new, id);
   }
}
