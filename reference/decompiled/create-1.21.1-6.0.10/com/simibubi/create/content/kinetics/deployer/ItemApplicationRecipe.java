package com.simibubi.create.content.kinetics.deployer;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

public class ItemApplicationRecipe extends ProcessingRecipe<RecipeWrapper, ItemApplicationRecipeParams> {
   private boolean keepHeldItem;

   public ItemApplicationRecipe(AllRecipeTypes type, ItemApplicationRecipeParams params) {
      super(type, params);
      this.keepHeldItem = params.keepHeldItem;
   }

   public boolean matches(RecipeWrapper inv, Level p_77569_2_) {
      return this.getProcessedItem().test(inv.getItem(0)) && this.getRequiredHeldItem().test(inv.getItem(1));
   }

   @Override
   protected int getMaxInputCount() {
      return 2;
   }

   @Override
   protected int getMaxOutputCount() {
      return 4;
   }

   public boolean shouldKeepHeldItem() {
      return this.keepHeldItem;
   }

   public Ingredient getRequiredHeldItem() {
      if (this.ingredients.size() < 2) {
         throw new IllegalStateException("Item Application Recipe has no tool!");
      } else {
         return (Ingredient)this.ingredients.get(1);
      }
   }

   public Ingredient getProcessedItem() {
      if (this.ingredients.isEmpty()) {
         throw new IllegalStateException("Item Application Recipe has no ingredient!");
      } else {
         return (Ingredient)this.ingredients.get(0);
      }
   }

   public static class Builder<R extends ItemApplicationRecipe>
      extends ProcessingRecipeBuilder<ItemApplicationRecipeParams, R, ItemApplicationRecipe.Builder<R>> {
      public Builder(ItemApplicationRecipe.Factory<R> factory, ResourceLocation recipeId) {
         super(factory, recipeId);
      }

      protected ItemApplicationRecipeParams createParams() {
         return new ItemApplicationRecipeParams();
      }

      public ItemApplicationRecipe.Builder<R> self() {
         return this;
      }

      public ItemApplicationRecipe.Builder<R> toolNotConsumed() {
         this.params.keepHeldItem = true;
         return this;
      }
   }

   @FunctionalInterface
   public interface Factory<R extends ItemApplicationRecipe> extends ProcessingRecipe.Factory<ItemApplicationRecipeParams, R> {
      R create(ItemApplicationRecipeParams var1);
   }

   public static class Serializer<R extends ItemApplicationRecipe> implements RecipeSerializer<R> {
      private final MapCodec<R> codec;
      private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

      public Serializer(ProcessingRecipe.Factory<ItemApplicationRecipeParams, R> factory) {
         this.codec = ProcessingRecipe.codec(factory, ItemApplicationRecipeParams.CODEC);
         this.streamCodec = ProcessingRecipe.streamCodec(factory, ItemApplicationRecipeParams.STREAM_CODEC);
      }

      public MapCodec<R> codec() {
         return this.codec;
      }

      public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
         return this.streamCodec;
      }
   }
}
