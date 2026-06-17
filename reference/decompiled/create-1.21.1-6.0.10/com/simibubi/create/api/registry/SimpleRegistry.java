package com.simibubi.create.api.registry;

import com.simibubi.create.impl.registry.SimpleRegistryImpl;
import com.simibubi.create.impl.registry.TagProviderImpl;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface SimpleRegistry<K, V> {
   void register(K var1, V var2);

   void registerProvider(SimpleRegistry.Provider<K, V> var1);

   void invalidate();

   @Nullable
   V get(K var1);

   @Nullable
   V get(StateHolder<K, ?> var1);

   static <K, V> SimpleRegistry<K, V> create() {
      return SimpleRegistryImpl.single();
   }

   public interface Multi<K, V> extends SimpleRegistry<K, List<V>> {
      void add(K var1, V var2);

      void addProvider(SimpleRegistry.Provider<K, V> var1);

      @NotNull
      List<V> get(K var1);

      @NotNull
      List<V> get(StateHolder<K, ?> var1);

      static <K, V> SimpleRegistry.Multi<K, V> create() {
         return SimpleRegistryImpl.multi();
      }
   }

   @FunctionalInterface
   public interface Provider<K, V> {
      @Nullable
      V get(K var1);

      default void onRegister(Runnable invalidate) {
      }

      static <K, V> SimpleRegistry.Provider<K, V> forTag(TagKey<K> tag, Function<K, Holder<K>> holderGetter, V value) {
         return new TagProviderImpl<>(tag, holderGetter, value);
      }

      static <V> SimpleRegistry.Provider<Block, V> forBlockTag(TagKey<Block> tag, V value) {
         return new TagProviderImpl<>(tag, Block::builtInRegistryHolder, value);
      }

      static <V> SimpleRegistry.Provider<BlockEntityType<?>, V> forBlockEntityTag(TagKey<BlockEntityType<?>> tag, V value) {
         return new TagProviderImpl<>(tag, TagProviderImpl::getBeHolder, value);
      }

      static <V> SimpleRegistry.Provider<Item, V> forItemTag(TagKey<Item> tag, V value) {
         return new TagProviderImpl<>(tag, Item::builtInRegistryHolder, value);
      }

      static <V> SimpleRegistry.Provider<EntityType<?>, V> forEntityTag(TagKey<EntityType<?>> tag, V value) {
         return new TagProviderImpl<>(tag, EntityType::builtInRegistryHolder, value);
      }

      static <V> SimpleRegistry.Provider<Fluid, V> forFluidTag(TagKey<Fluid> tag, V value) {
         return new TagProviderImpl<>(tag, Fluid::builtInRegistryHolder, value);
      }
   }
}
