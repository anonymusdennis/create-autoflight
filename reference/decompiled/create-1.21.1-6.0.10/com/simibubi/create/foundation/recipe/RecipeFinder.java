package com.simibubi.create.foundation.recipe;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.Create;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class RecipeFinder {
   private static final Cache<Object, List<RecipeHolder<? extends Recipe<?>>>> CACHED_SEARCHES = CacheBuilder.newBuilder().build();
   public static final ResourceManagerReloadListener LISTENER = resourceManager -> CACHED_SEARCHES.invalidateAll();

   public static List<RecipeHolder<? extends Recipe<?>>> get(@Nullable Object cacheKey, Level level, Predicate<RecipeHolder<? extends Recipe<?>>> conditions) {
      if (cacheKey == null) {
         return startSearch(level, conditions);
      } else {
         try {
            return (List<RecipeHolder<? extends Recipe<?>>>)CACHED_SEARCHES.get(cacheKey, () -> startSearch(level, conditions));
         } catch (ExecutionException var4) {
            Create.LOGGER.error("Encountered a exception while searching for recipes", var4);
            return Collections.emptyList();
         }
      }
   }

   private static List<RecipeHolder<? extends Recipe<?>>> startSearch(Level level, Predicate<? super RecipeHolder<? extends Recipe<?>>> conditions) {
      List<RecipeHolder<? extends Recipe<?>>> recipes = new ArrayList<>();

      for (RecipeHolder<? extends Recipe<?>> r : level.getRecipeManager().getRecipes()) {
         if (conditions.test(r)) {
            recipes.add(r);
         }
      }

      return recipes;
   }
}
