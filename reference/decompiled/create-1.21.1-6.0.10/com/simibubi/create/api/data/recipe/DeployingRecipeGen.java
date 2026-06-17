package com.simibubi.create.api.data.recipe;

import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipe;
import com.simibubi.create.content.kinetics.deployer.ItemApplicationRecipeParams;
import com.simibubi.create.foundation.block.CopperBlockSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

public abstract class DeployingRecipeGen
   extends ProcessingRecipeGen<ItemApplicationRecipeParams, DeployerApplicationRecipe, ItemApplicationRecipe.Builder<DeployerApplicationRecipe>> {
   public BaseRecipeProvider.GeneratedRecipe copperChain(CopperBlockSet set) {
      for (CopperBlockSet.Variant<?> variant : set.getVariants()) {
         List<Supplier<ItemLike>> chain = new ArrayList<>(4);
         List<Supplier<ItemLike>> waxedChain = new ArrayList<>(4);

         for (WeatherState state : WeatherState.values()) {
            waxedChain.add(set.get(variant, state, true)::get);
            chain.add(set.get(variant, state, false)::get);
         }

         this.oxidizationChain(chain, waxedChain);
      }

      return null;
   }

   public BaseRecipeProvider.GeneratedRecipe addWax(Supplier<ItemLike> waxed, Supplier<ItemLike> nonWaxed) {
      this.createWithDeferredId(
         this.idWithSuffix(nonWaxed, "_from_removing_wax"),
         b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(waxed.get())).require(ItemTags.AXES))
               .toolNotConsumed()
               .output(nonWaxed.get())
      );
      return this.createWithDeferredId(
         this.idWithSuffix(waxed, "_from_adding_wax"),
         b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(nonWaxed.get()))
                  .require(Items.HONEYCOMB_BLOCK))
               .toolNotConsumed()
               .output(waxed.get())
      );
   }

   public BaseRecipeProvider.GeneratedRecipe oxidizationChain(List<Supplier<ItemLike>> chain, List<Supplier<ItemLike>> waxedChain) {
      for (int i = 0; i < chain.size() - 1; i++) {
         Supplier<ItemLike> to = chain.get(i);
         Supplier<ItemLike> from = chain.get(i + 1);
         this.createWithDeferredId(
            this.idWithSuffix(to, "_from_deoxidising"),
            b -> (ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)((ItemApplicationRecipe.Builder)b.require(from.get())).require(ItemTags.AXES))
                  .toolNotConsumed()
                  .output(to.get())
         );
      }

      for (int i = 0; i < chain.size(); i++) {
         this.addWax(waxedChain.get(i), chain.get(i));
      }

      return null;
   }

   public DeployingRecipeGen(PackOutput output, CompletableFuture<Provider> registries, String defaultNamespace) {
      super(output, registries, defaultNamespace);
   }

   protected AllRecipeTypes getRecipeType() {
      return AllRecipeTypes.DEPLOYING;
   }

   protected ItemApplicationRecipe.Builder<DeployerApplicationRecipe> getBuilder(ResourceLocation id) {
      return new ItemApplicationRecipe.Builder<>(DeployerApplicationRecipe::new, id);
   }
}
