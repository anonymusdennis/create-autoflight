package com.simibubi.create.foundation.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.data.recipe.DatagenMod;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.simibubi.create.foundation.mixin.accessor.MappedRegistryAccessor;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder.Reference;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class SimpleDatagenIngredient implements ICustomIngredient {
   private static final MapCodec<SimpleDatagenIngredient> INTERNAL_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(ResourceLocation.CODEC.fieldOf("item").forGetter(i -> i.mod.asResource(i.id)))
            .apply(
               instance,
               location -> {
                  for (Mods mod : Mods.values()) {
                     if (mod.getId().equals(location.getNamespace())) {
                        return new SimpleDatagenIngredient(mod, location.getPath());
                     }
                  }

                  throw new AssertionError(
                     "ID " + location.getNamespace() + " doesn't correspond to any compat mod. SimpleDatagenIngredient is not meant for deserialization anyway"
                  );
               }
            )
   );
   private static final MapCodec<SimpleDatagenIngredient> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(INTERNAL_CODEC.codec().listOf().fieldOf("ingredients").forGetter(List::of)).apply(instance, list -> {
            assert list.size() == 1 : "SimpleDatagenIngredient should only be serialized as a single-element list, and shouldn't be deserialized anyway";

            return (SimpleDatagenIngredient)list.getFirst();
         })
   );
   private static final IngredientType<?> INGREDIENT_TYPE = new IngredientType(CODEC);
   private final DatagenMod mod;
   private final String id;
   private static boolean didRegistryInjection = false;

   public SimpleDatagenIngredient(DatagenMod mod, String id) {
      this.mod = mod;
      this.id = id;
   }

   public boolean test(@NotNull ItemStack stack) {
      return stack.getItemHolder().getKey().location().equals(this.mod.asResource(this.id));
   }

   @NotNull
   public Stream<ItemStack> getItems() {
      return Stream.empty();
   }

   public boolean isSimple() {
      return false;
   }

   @NotNull
   public IngredientType<?> getType() {
      if (!didRegistryInjection) {
         if (!(NeoForgeRegistries.INGREDIENT_TYPES instanceof MappedRegistryAccessor<?> mra)) {
            throw new AssertionError(
               "SimpleDatagenIngredient will not be able to serialize without injecting into a registry. Expected NeoForgeRegistries.INGREDIENT_TYPES to be of class MappedRegistry, is of class "
                  + NeoForgeRegistries.INGREDIENT_TYPES.getClass()
            );
         }

         IngredientType<?> baseType = (IngredientType<?>)NeoForgeMod.COMPOUND_INGREDIENT_TYPE.get();
         int wrappedId = mra.getToId().getOrDefault(baseType, -1);
         ResourceKey<IngredientType<?>> wrappedKey = NeoForgeMod.COMPOUND_INGREDIENT_TYPE.getKey();
         mra.getToId().put(INGREDIENT_TYPE, wrappedId);
         ((Map<IngredientType<?>, Reference<?>>)mra.getByValue()).put(INGREDIENT_TYPE, Reference.createStandAlone(null, wrappedKey));
         didRegistryInjection = true;
      }

      return INGREDIENT_TYPE;
   }
}
