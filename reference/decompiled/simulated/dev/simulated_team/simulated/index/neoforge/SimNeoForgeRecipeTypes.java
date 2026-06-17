package dev.simulated_team.simulated.index.neoforge;

import com.mojang.serialization.Codec;
import com.simibubi.create.foundation.recipe.IRecipeTypeInfo;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.neoforge.PortableEngineDyeingRecipe;
import java.util.function.Supplier;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public enum SimNeoForgeRecipeTypes implements IRecipeTypeInfo, StringRepresentable {
   PORTABLE_ENGINE_DYEING(() -> new SimpleCraftingRecipeSerializer(PortableEngineDyeingRecipe::new), () -> RecipeType.CRAFTING, false);

   public static final Codec<SimNeoForgeRecipeTypes> CODEC = StringRepresentable.fromEnum(SimNeoForgeRecipeTypes::values);
   public final ResourceLocation id;
   public final Supplier<RecipeSerializer<?>> serializerSupplier;
   private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> serializerObject;
   @Nullable
   private final DeferredHolder<RecipeType<?>, RecipeType<?>> typeObject;
   private final Supplier<RecipeType<?>> type;

   private SimNeoForgeRecipeTypes(
      final Supplier<RecipeSerializer<?>> serializerSupplier, final Supplier<RecipeType<?>> typeSupplier, final boolean registerType
   ) {
      String name = Lang.asId(this.name());
      this.id = Simulated.path(name);
      this.serializerSupplier = serializerSupplier;
      this.serializerObject = SimNeoForgeRecipeTypes.Registers.SERIALIZER_REGISTER.register(name, serializerSupplier);
      if (registerType) {
         this.typeObject = SimNeoForgeRecipeTypes.Registers.TYPE_REGISTER.register(name, typeSupplier);
         this.type = this.typeObject;
      } else {
         this.typeObject = null;
         this.type = typeSupplier;
      }
   }

   @Internal
   public static void register(IEventBus modEventBus) {
      SimNeoForgeRecipeTypes.Registers.SERIALIZER_REGISTER.register(modEventBus);
      SimNeoForgeRecipeTypes.Registers.TYPE_REGISTER.register(modEventBus);
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public <T extends RecipeSerializer<?>> T getSerializer() {
      return (T)this.serializerObject.get();
   }

   public <I extends RecipeInput, R extends Recipe<I>> RecipeType<R> getType() {
      return (RecipeType<R>)this.type.get();
   }

   @NotNull
   public String getSerializedName() {
      return this.id.toString();
   }

   private static class Registers {
      private static final DeferredRegister<RecipeSerializer<?>> SERIALIZER_REGISTER = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "simulated");
      private static final DeferredRegister<RecipeType<?>> TYPE_REGISTER = DeferredRegister.create(Registries.RECIPE_TYPE, "simulated");
   }
}
