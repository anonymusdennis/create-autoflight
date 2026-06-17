package com.simibubi.create.content.kinetics.deployer;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public class DeployerRecipeSearchEvent extends Event implements ICancellableEvent {
   private final DeployerBlockEntity blockEntity;
   private final RecipeWrapper inventory;
   @Nullable
   RecipeHolder<? extends Recipe<? extends RecipeInput>> recipe = null;
   private int maxPriority = 0;

   public DeployerRecipeSearchEvent(DeployerBlockEntity blockEntity, RecipeWrapper inventory) {
      this.blockEntity = blockEntity;
      this.inventory = inventory;
   }

   public DeployerBlockEntity getBlockEntity() {
      return this.blockEntity;
   }

   public RecipeWrapper getInventory() {
      return this.inventory;
   }

   public boolean shouldAddRecipeWithPriority(int priority) {
      return !this.isCanceled() && priority > this.maxPriority;
   }

   @Nullable
   public RecipeHolder<? extends Recipe<? extends RecipeInput>> getRecipe() {
      return this.isCanceled() ? null : this.recipe;
   }

   public void addRecipe(Supplier<Optional<? extends RecipeHolder<? extends Recipe<? extends RecipeInput>>>> recipeSupplier, int priority) {
      if (this.shouldAddRecipeWithPriority(priority)) {
         recipeSupplier.get().ifPresent(newRecipe -> {
            this.recipe = (RecipeHolder<? extends Recipe<? extends RecipeInput>>)newRecipe;
            this.maxPriority = priority;
         });
      }
   }
}
