package com.simibubi.create.content.processing.recipe;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class StandardProcessingRecipe<T extends RecipeInput> extends ProcessingRecipe<T, ProcessingRecipeParams> {
   public StandardProcessingRecipe(IRecipeTypeInfo typeInfo, ProcessingRecipeParams params) {
      super(typeInfo, params);
   }

   public static class Builder<R extends StandardProcessingRecipe<?>>
      extends ProcessingRecipeBuilder<ProcessingRecipeParams, R, StandardProcessingRecipe.Builder<R>> {
      public Builder(StandardProcessingRecipe.Factory<R> factory, ResourceLocation recipeId) {
         super(factory, recipeId);
      }

      @Override
      protected ProcessingRecipeParams createParams() {
         return new ProcessingRecipeParams();
      }

      public StandardProcessingRecipe.Builder<R> self() {
         return this;
      }
   }

   @FunctionalInterface
   public interface Factory<R extends StandardProcessingRecipe<?>> extends ProcessingRecipe.Factory<ProcessingRecipeParams, R> {
      R create(ProcessingRecipeParams var1);
   }

   public static class Serializer<R extends StandardProcessingRecipe<?>> implements RecipeSerializer<R> {
      private final StandardProcessingRecipe.Factory<R> factory;
      private final MapCodec<R> codec;
      private final StreamCodec<RegistryFriendlyByteBuf, R> streamCodec;

      public Serializer(StandardProcessingRecipe.Factory<R> factory) {
         this.factory = factory;
         this.codec = ProcessingRecipe.codec(factory, ProcessingRecipeParams.CODEC);
         this.streamCodec = ProcessingRecipe.streamCodec(factory, ProcessingRecipeParams.STREAM_CODEC);
      }

      public MapCodec<R> codec() {
         return this.codec;
      }

      public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() {
         return this.streamCodec;
      }

      public StandardProcessingRecipe.Factory<R> factory() {
         return this.factory;
      }
   }
}
