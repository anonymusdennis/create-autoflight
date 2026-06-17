package com.simibubi.create.foundation.recipe;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries.Keys;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AllIngredients {
   public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(Keys.INGREDIENT_TYPES, "create");

   @Internal
   public static void register(IEventBus modEventBus) {
      INGREDIENT_TYPES.register(modEventBus);
   }
}
