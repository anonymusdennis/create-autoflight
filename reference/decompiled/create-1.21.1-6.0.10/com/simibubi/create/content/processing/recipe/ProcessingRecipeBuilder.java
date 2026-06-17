package com.simibubi.create.content.processing.recipe;

import com.google.common.base.Joiner;
import com.simibubi.create.Create;
import com.simibubi.create.api.data.recipe.DatagenMod;
import com.simibubi.create.foundation.data.SimpleDatagenIngredient;
import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.FluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public abstract class ProcessingRecipeBuilder<P extends ProcessingRecipeParams, R extends ProcessingRecipe<?, P>, S extends ProcessingRecipeBuilder<P, R, S>> {
   protected ResourceLocation recipeId;
   protected ProcessingRecipe.Factory<P, R> factory;
   protected P params;
   protected List<ICondition> recipeConditions;

   public ProcessingRecipeBuilder(ProcessingRecipe.Factory<P, R> factory, ResourceLocation recipeId) {
      this.recipeId = recipeId;
      this.factory = factory;
      this.params = this.createParams();
      this.recipeConditions = new ArrayList<>();
   }

   protected abstract P createParams();

   public abstract S self();

   public S withItemIngredients(Ingredient... ingredients) {
      return this.withItemIngredients(NonNullList.of(Ingredient.EMPTY, ingredients));
   }

   public S withItemIngredients(NonNullList<Ingredient> ingredients) {
      this.params.ingredients = ingredients;
      return this.self();
   }

   public S withSingleItemOutput(ItemStack output) {
      return this.withItemOutputs(new ProcessingOutput(output, 1.0F));
   }

   public S withItemOutputs(ProcessingOutput... outputs) {
      return this.withItemOutputs(NonNullList.of(ProcessingOutput.EMPTY, outputs));
   }

   public S withItemOutputs(NonNullList<ProcessingOutput> outputs) {
      this.params.results = outputs;
      return this.self();
   }

   public S withFluidIngredients(SizedFluidIngredient... ingredients) {
      return this.withFluidIngredients(NonNullList.of(new SizedFluidIngredient(FluidIngredient.empty(), 1000), ingredients));
   }

   public S withFluidIngredients(NonNullList<SizedFluidIngredient> ingredients) {
      this.params.fluidIngredients = ingredients;
      return this.self();
   }

   public S withFluidOutputs(FluidStack... outputs) {
      return this.withFluidOutputs(NonNullList.of(FluidStack.EMPTY, outputs));
   }

   public S withFluidOutputs(NonNullList<FluidStack> outputs) {
      this.params.fluidResults = outputs;
      return this.self();
   }

   public S duration(int ticks) {
      this.params.processingDuration = ticks;
      return this.self();
   }

   public S averageProcessingDuration() {
      return this.duration(100);
   }

   public S requiresHeat(HeatCondition condition) {
      this.params.requiredHeat = condition;
      return this.self();
   }

   public R build() {
      return this.factory.create(this.params);
   }

   public void build(RecipeOutput consumer) {
      R recipe = this.build();
      IRecipeTypeInfo recipeType = recipe.getTypeInfo();
      ResourceLocation typeId = recipeType.getId();
      ResourceLocation id = this.recipeId.withPrefix(typeId.getPath() + "/");
      List<String> errors = recipe.validate();
      if (!errors.isEmpty()) {
         errors.add(recipe.getClass().getSimpleName() + "with id " + id + " failed validation:");
         Create.LOGGER.warn(Joiner.on('\n').join(errors));
      }

      consumer.accept(id, recipe, null, this.recipeConditions.toArray(new ICondition[0]));
   }

   public S require(TagKey<Item> tag) {
      return this.require(Ingredient.of(tag));
   }

   public S require(ItemLike item) {
      return this.require(Ingredient.of(new ItemLike[]{item}));
   }

   public S require(Ingredient ingredient) {
      this.params.ingredients.add(ingredient);
      return this.self();
   }

   public S require(ICustomIngredient ingredient) {
      this.params.ingredients.add(ingredient.toVanilla());
      return this.self();
   }

   public S require(DatagenMod mod, String id) {
      this.params.ingredients.add(new SimpleDatagenIngredient(mod, id).toVanilla());
      return this.self();
   }

   public S require(FlowingFluid fluid, int amount) {
      return this.require(SizedFluidIngredient.of(fluid.getSource(), amount));
   }

   public S require(TagKey<Fluid> fluidTag, int amount) {
      return this.require(SizedFluidIngredient.of(fluidTag, amount));
   }

   public S require(SizedFluidIngredient ingredient) {
      this.params.fluidIngredients.add(ingredient);
      return this.self();
   }

   public S output(ItemLike item) {
      return this.output(item, 1);
   }

   public S output(float chance, ItemLike item) {
      return this.output(chance, item, 1);
   }

   public S output(ItemLike item, int amount) {
      return this.output(1.0F, item, amount);
   }

   public S output(float chance, ItemLike item, int amount) {
      return this.output(chance, new ItemStack(item, amount));
   }

   public S output(ItemStack output) {
      return this.output(1.0F, output);
   }

   public S output(float chance, ItemStack output) {
      return this.output(new ProcessingOutput(output, chance));
   }

   public S output(float chance, DatagenMod mod, String id, int amount) {
      return this.output(new ProcessingOutput(mod.asResource(id), amount, chance));
   }

   public S output(ResourceLocation id) {
      return this.output(1.0F, id, 1);
   }

   public S output(DatagenMod mod, String id) {
      return this.output(1.0F, mod.asResource(id), 1);
   }

   public S output(float chance, ResourceLocation registryName, int amount) {
      return this.output(new ProcessingOutput(registryName, amount, chance));
   }

   public S output(ProcessingOutput output) {
      this.params.results.add(output);
      return this.self();
   }

   public S output(Fluid fluid, int amount) {
      fluid = FluidHelper.convertToStill(fluid);
      return this.output(new FluidStack(fluid, amount));
   }

   public S output(FluidStack fluidStack) {
      this.params.fluidResults.add(fluidStack);
      return this.self();
   }

   public S whenModLoaded(String modid) {
      return this.withCondition(new ModLoadedCondition(modid));
   }

   public S whenModMissing(String modid) {
      return this.withCondition(new NotCondition(new ModLoadedCondition(modid)));
   }

   public S withCondition(ICondition condition) {
      this.recipeConditions.add(condition);
      return this.self();
   }
}
