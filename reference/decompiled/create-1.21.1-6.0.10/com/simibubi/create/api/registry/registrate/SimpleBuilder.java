package com.simibubi.create.api.registry.registrate;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.impl.registry.TagProviderImpl;
import com.tterrag.registrate.AbstractRegistrate;
import com.tterrag.registrate.builders.AbstractBuilder;
import com.tterrag.registrate.builders.BuilderCallback;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class SimpleBuilder<R, T extends R, P> extends AbstractBuilder<R, T, P, SimpleBuilder<R, T, P>> {
   private final Supplier<T> value;
   private SimpleBuilder.SimpleRegistryAccess<Block, R> byBlock;
   private SimpleBuilder.SimpleRegistryAccess<BlockEntityType<?>, R> byBlockEntity;
   private SimpleBuilder.SimpleRegistryAccess<EntityType<?>, R> byEntity;
   private SimpleBuilder.SimpleRegistryAccess<Fluid, R> byFluid;

   public SimpleBuilder(AbstractRegistrate<?> owner, P parent, String name, BuilderCallback callback, ResourceKey<Registry<R>> registryKey, Supplier<T> value) {
      super(owner, parent, name, callback, registryKey);
      this.value = value;
   }

   protected T createEntry() {
      return this.value.get();
   }

   public SimpleBuilder<R, T, P> byBlock(SimpleRegistry<Block, R> registry) {
      this.byBlock = SimpleBuilder.SimpleRegistryAccess.of(registry, Block::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byBlock(SimpleRegistry.Multi<Block, R> registry) {
      this.byBlock = SimpleBuilder.SimpleRegistryAccess.of(registry, Block::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byBlockEntity(SimpleRegistry<BlockEntityType<?>, R> registry) {
      this.byBlockEntity = SimpleBuilder.SimpleRegistryAccess.of(registry, TagProviderImpl::getBeHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byBlockEntity(SimpleRegistry.Multi<BlockEntityType<?>, R> registry) {
      this.byBlockEntity = SimpleBuilder.SimpleRegistryAccess.of(registry, TagProviderImpl::getBeHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byEntity(SimpleRegistry<EntityType<?>, R> registry) {
      this.byEntity = SimpleBuilder.SimpleRegistryAccess.of(registry, EntityType::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byEntity(SimpleRegistry.Multi<EntityType<?>, R> registry) {
      this.byEntity = SimpleBuilder.SimpleRegistryAccess.of(registry, EntityType::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byFluid(SimpleRegistry<Fluid, R> registry) {
      this.byFluid = SimpleBuilder.SimpleRegistryAccess.of(registry, Fluid::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> byFluid(SimpleRegistry.Multi<Fluid, R> registry) {
      this.byFluid = SimpleBuilder.SimpleRegistryAccess.of(registry, Fluid::builtInRegistryHolder);
      return this;
   }

   public SimpleBuilder<R, T, P> associate(Block block) {
      assertPresent(this.byBlock, "Block");
      this.onRegister(value -> this.byBlock.adder.accept(block, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associateBlockTag(TagKey<Block> tag) {
      assertPresent(this.byBlock, "Block");
      this.onRegister(value -> this.byBlock.tagAdder.accept(tag, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associate(BlockEntityType<?> type) {
      assertPresent(this.byBlockEntity, "BlockEntityType");
      this.onRegister(value -> this.byBlockEntity.adder.accept(type, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associateBeTag(TagKey<BlockEntityType<?>> tag) {
      assertPresent(this.byBlockEntity, "BlockEntityType");
      this.onRegister(value -> this.byBlockEntity.tagAdder.accept(tag, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associate(EntityType<?> type) {
      assertPresent(this.byEntity, "EntityType");
      this.onRegister(value -> this.byEntity.adder.accept(type, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associateEntityTag(TagKey<EntityType<?>> tag) {
      assertPresent(this.byEntity, "EntityType");
      this.onRegister(value -> this.byEntity.tagAdder.accept(tag, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associate(Fluid fluid) {
      assertPresent(this.byFluid, "Fluid");
      this.onRegister(value -> this.byFluid.adder.accept(fluid, (R)value));
      return this;
   }

   public SimpleBuilder<R, T, P> associateFluidTag(TagKey<Fluid> tag) {
      assertPresent(this.byFluid, "Fluid");
      this.onRegister(value -> this.byFluid.tagAdder.accept(tag, (R)value));
      return this;
   }

   private static void assertPresent(@Nullable Object object, String type) {
      if (object == null) {
         throw new IllegalStateException("This type does not support " + type + " associations");
      }
   }

   protected static record SimpleRegistryAccess<K, V>(BiConsumer<K, V> adder, BiConsumer<TagKey<K>, V> tagAdder) {
      public static <K, V> SimpleBuilder.SimpleRegistryAccess<K, V> of(SimpleRegistry<K, V> registry, Function<K, Holder<K>> holderGetter) {
         return new SimpleBuilder.SimpleRegistryAccess<>(
            registry::register, (tag, value) -> registry.registerProvider(SimpleRegistry.Provider.forTag(tag, holderGetter, value))
         );
      }

      public static <K, V> SimpleBuilder.SimpleRegistryAccess<K, V> of(SimpleRegistry.Multi<K, V> registry, Function<K, Holder<K>> holderGetter) {
         return new SimpleBuilder.SimpleRegistryAccess<>(
            registry::add, (tag, value) -> registry.addProvider(SimpleRegistry.Provider.forTag(tag, holderGetter, value))
         );
      }
   }
}
