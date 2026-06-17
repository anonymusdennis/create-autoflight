package com.simibubi.create.foundation.recipe.trie;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.Create;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RecipeTrie<R extends Recipe<?>> {
   private static final int MAX_CACHE_SIZE = Integer.getInteger("create.recipe_trie.max_cache_size", 512);
   private final IntArrayTrie<R> trie;
   private final Object2IntMap<AbstractVariant> variantToId;
   private final Int2ObjectMap<IntSet> variantToIngredients;
   private final int universalIngredientId;
   private final Cache<Set<AbstractVariant>, IntSet> ingredientCache = CacheBuilder.newBuilder().maximumSize((long)MAX_CACHE_SIZE).build();

   private RecipeTrie(IntArrayTrie<R> trie, Object2IntMap<AbstractVariant> variantToId, Int2ObjectMap<IntSet> variantToIngredients, int universalIngredientId) {
      this.trie = trie;
      this.variantToId = variantToId;
      this.variantToIngredients = variantToIngredients;
      this.universalIngredientId = universalIngredientId;
   }

   @NotNull
   public static Set<AbstractVariant> getVariants(@Nullable IItemHandler itemStorage, @Nullable IFluidHandler fluidStorage) {
      Set<AbstractVariant> variants = new HashSet<>();
      if (itemStorage != null) {
         for (int slot = 0; slot < itemStorage.getSlots(); slot++) {
            ItemStack item = itemStorage.getStackInSlot(slot);
            if (!item.isEmpty()) {
               variants.add(new AbstractVariant.AbstractItem(item.getItem()));
            }
         }
      }

      if (fluidStorage != null) {
         for (int tank = 0; tank < fluidStorage.getTanks(); tank++) {
            FluidStack fluid = fluidStorage.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
               variants.add(new AbstractVariant.AbstractFluid(fluid.getFluid()));
            }
         }
      }

      return variants;
   }

   private IntSet getAvailableIngredients(@NotNull Set<AbstractVariant> pool) {
      pool.retainAll(this.variantToId.keySet());

      try {
         return (IntSet)this.ingredientCache.get(Set.copyOf(pool), () -> {
            IntSet ingredients = new IntOpenHashSet();
            ingredients.add(this.universalIngredientId);

            for (AbstractVariant variant : pool) {
               int id = this.variantToId.getInt(variant);
               if (id >= 0) {
                  IntSet ingredientIds = (IntSet)this.variantToIngredients.get(id);
                  if (ingredientIds != null) {
                     ingredients.addAll(ingredientIds);
                  }
               }
            }

            return ingredients;
         });
      } catch (ExecutionException var3) {
         throw new RuntimeException(var3);
      }
   }

   @NotNull
   public List<R> lookup(@NotNull Set<AbstractVariant> pool) {
      return this.trie.lookup(this.getAvailableIngredients(pool));
   }

   public static <R extends Recipe<?>> RecipeTrie.Builder<R> builder() {
      return new RecipeTrie.Builder<>();
   }

   public static class Builder<R extends Recipe<?>> {
      private final IntArrayTrie<R> trie = new IntArrayTrie<>();
      private final Map<Object, AbstractVariant> variantCache = new HashMap<>();
      private final Object2IntOpenHashMap<AbstractVariant> variantToId = new Object2IntOpenHashMap();
      private int nextVariantId = 0;
      private final Object2IntMap<AbstractIngredient> ingredientToId = new Object2IntOpenHashMap();
      private int nextIngredientId = 0;
      private final int universalIngredientId;
      private final Int2ObjectOpenHashMap<IntSet> variantToIngredients = new Int2ObjectOpenHashMap();

      private Builder() {
         this.variantToId.defaultReturnValue(-1);
         this.ingredientToId.defaultReturnValue(-1);
         this.universalIngredientId = this.getOrAssignId(AbstractIngredient.Universal.INSTANCE);
      }

      private int getOrAssignId(AbstractIngredient ingredient) {
         return this.ingredientToId.computeIfAbsent(ingredient, $ -> {
            int id = this.nextIngredientId++;

            for (AbstractVariant variant : ingredient.variants) {
               ((IntSet)this.variantToIngredients.computeIfAbsent(this.getOrAssignId(variant), $1 -> new IntOpenHashSet())).add(id);
            }

            return id;
         });
      }

      private int getOrAssignId(AbstractVariant variant) {
         return this.variantToId.computeIfAbsent(variant, $ -> this.nextVariantId++);
      }

      private AbstractVariant getOrAssignVariant(Item item) {
         AbstractVariant variant = this.variantCache.computeIfAbsent(item, $ -> new AbstractVariant.AbstractItem(item));
         this.getOrAssignId(variant);
         return variant;
      }

      private AbstractVariant getOrAssignVariant(Fluid fluid) {
         AbstractVariant variant = this.variantCache.computeIfAbsent(fluid, $ -> new AbstractVariant.AbstractFluid(fluid));
         this.getOrAssignId(variant);
         return variant;
      }

      private void insert(AbstractRecipe<? extends R> recipe) {
         int[] key = new int[recipe.ingredients.size()];
         int i = 0;

         for (AbstractIngredient ingredient : recipe.ingredients) {
            key[i++] = this.getOrAssignId(ingredient);
         }

         Arrays.sort(key);
         this.trie.insert(key, (R)recipe.recipe);
      }

      public <R1 extends R> void insert(R1 recipe) {
         this.insert(this.createRecipe((R)recipe));
      }

      private <R1 extends R> AbstractRecipe<R1> createRecipe(R1 recipe) {
         Set<AbstractIngredient> ingredients = new HashSet<>();

         for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) {
               ingredients.add(AbstractIngredient.Universal.INSTANCE);
            } else if (!ingredient.isSimple()) {
               ingredients.add(AbstractIngredient.Universal.INSTANCE);
            } else {
               Set<AbstractVariant> variants = new HashSet<>();

               for (ItemStack stack : ingredient.getItems()) {
                  variants.add(this.getOrAssignVariant(stack.getItem()));
               }

               ingredients.add(new AbstractIngredient(variants));
            }
         }

         if (recipe instanceof BasinRecipe basinRecipe) {
            for (SizedFluidIngredient ingredientx : basinRecipe.getFluidIngredients()) {
               if (ingredientx.amount() == 0) {
                  ingredients.add(AbstractIngredient.Universal.INSTANCE);
               } else {
                  Set<AbstractVariant> variants = new HashSet<>();

                  for (FluidStack stack : ingredientx.getFluids()) {
                     variants.add(this.getOrAssignVariant(stack.getFluid()));
                  }

                  ingredients.add(new AbstractIngredient(variants));
               }
            }
         }

         return new AbstractRecipe((R)recipe, ingredients);
      }

      public RecipeTrie<R> build() {
         this.variantToId.trim();
         this.variantToIngredients.trim();
         Create.LOGGER
            .info(
               "RecipeTrie of depth {} with {} nodes built with {} variants, {} ingredients, and {} recipes",
               new Object[]{this.trie.getMaxDepth(), this.trie.getNodeCount(), this.variantToId.size(), this.ingredientToId.size(), this.trie.getValueCount()}
            );
         return new RecipeTrie<>(this.trie, this.variantToId, this.variantToIngredients, this.universalIngredientId);
      }
   }
}
