package com.simibubi.create.content.processing.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.codec.CreateCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class ProcessingRecipeParams {
   public static MapCodec<ProcessingRecipeParams> CODEC = codec(ProcessingRecipeParams::new);
   public static StreamCodec<RegistryFriendlyByteBuf, ProcessingRecipeParams> STREAM_CODEC = streamCodec(ProcessingRecipeParams::new);
   protected NonNullList<Ingredient> ingredients = NonNullList.create();
   protected NonNullList<ProcessingOutput> results = NonNullList.create();
   protected NonNullList<SizedFluidIngredient> fluidIngredients = NonNullList.create();
   protected NonNullList<FluidStack> fluidResults = NonNullList.create();
   protected int processingDuration = 0;
   protected HeatCondition requiredHeat = HeatCondition.NONE;

   protected ProcessingRecipeParams() {
   }

   protected static <P extends ProcessingRecipeParams> MapCodec<P> codec(Supplier<P> factory) {
      return RecordCodecBuilder.mapCodec(
         instance -> instance.group(
                  Codec.either(CreateCodecs.SIZED_FLUID_INGREDIENT, Ingredient.CODEC)
                     .listOf()
                     .fieldOf("ingredients")
                     .forGetter(ProcessingRecipeParams::ingredients),
                  Codec.either(FluidStack.CODEC, ProcessingOutput.CODEC).listOf().fieldOf("results").forGetter(ProcessingRecipeParams::results),
                  Codec.INT.optionalFieldOf("processing_time", 0).forGetter(ProcessingRecipeParams::processingDuration),
                  HeatCondition.CODEC.optionalFieldOf("heat_requirement", HeatCondition.NONE).forGetter(ProcessingRecipeParams::requiredHeat)
               )
               .apply(instance, (ingredients, results, processingDuration, requiredHeat) -> {
                  P params = factory.get();
                  ingredients.forEach(either -> either.ifRight(params.ingredients::add).ifLeft(params.fluidIngredients::add));
                  results.forEach(either -> either.ifRight(params.results::add).ifLeft(params.fluidResults::add));
                  params.processingDuration = processingDuration;
                  params.requiredHeat = requiredHeat;
                  return params;
               })
      );
   }

   protected static <P extends ProcessingRecipeParams> StreamCodec<RegistryFriendlyByteBuf, P> streamCodec(Supplier<P> factory) {
      return StreamCodec.of((buffer, params) -> params.encode(buffer), buffer -> {
         P params = factory.get();
         params.decode(buffer);
         return params;
      });
   }

   protected final List<Either<SizedFluidIngredient, Ingredient>> ingredients() {
      List<Either<SizedFluidIngredient, Ingredient>> ingredients = new ArrayList<>(this.ingredients.size() + this.fluidIngredients.size());
      this.ingredients.forEach(ingredient -> ingredients.add(Either.right(ingredient)));
      this.fluidIngredients.forEach(ingredient -> ingredients.add(Either.left(ingredient)));
      return ingredients;
   }

   protected final List<Either<FluidStack, ProcessingOutput>> results() {
      List<Either<FluidStack, ProcessingOutput>> results = new ArrayList<>(this.results.size() + this.fluidResults.size());
      this.results.forEach(result -> results.add(Either.right(result)));
      this.fluidResults.forEach(result -> results.add(Either.left(result)));
      return results;
   }

   protected final int processingDuration() {
      return this.processingDuration;
   }

   protected final HeatCondition requiredHeat() {
      return this.requiredHeat;
   }

   protected void encode(RegistryFriendlyByteBuf buffer) {
      CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).encode(buffer, this.ingredients);
      CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).encode(buffer, this.fluidIngredients);
      CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).encode(buffer, this.results);
      CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).encode(buffer, this.fluidResults);
      ByteBufCodecs.VAR_INT.encode(buffer, this.processingDuration);
      HeatCondition.STREAM_CODEC.encode(buffer, this.requiredHeat);
   }

   protected void decode(RegistryFriendlyByteBuf buffer) {
      this.ingredients = (NonNullList<Ingredient>)CatnipStreamCodecBuilders.nonNullList(Ingredient.CONTENTS_STREAM_CODEC).decode(buffer);
      this.fluidIngredients = (NonNullList<SizedFluidIngredient>)CatnipStreamCodecBuilders.nonNullList(SizedFluidIngredient.STREAM_CODEC).decode(buffer);
      this.results = (NonNullList<ProcessingOutput>)CatnipStreamCodecBuilders.nonNullList(ProcessingOutput.STREAM_CODEC).decode(buffer);
      this.fluidResults = (NonNullList<FluidStack>)CatnipStreamCodecBuilders.nonNullList(FluidStack.STREAM_CODEC).decode(buffer);
      this.processingDuration = (Integer)ByteBufCodecs.VAR_INT.decode(buffer);
      this.requiredHeat = (HeatCondition)HeatCondition.STREAM_CODEC.decode(buffer);
   }
}
