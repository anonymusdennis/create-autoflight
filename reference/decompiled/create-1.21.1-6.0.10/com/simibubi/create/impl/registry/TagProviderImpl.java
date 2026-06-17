package com.simibubi.create.impl.registry;

import com.simibubi.create.api.registry.SimpleRegistry;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public class TagProviderImpl<K, V> implements SimpleRegistry.Provider<K, V> {
   private final TagKey<K> tag;
   private final Function<K, Holder<K>> holderGetter;
   private final V value;

   public TagProviderImpl(TagKey<K> tag, Function<K, Holder<K>> holderGetter, V value) {
      this.tag = tag;
      this.holderGetter = holderGetter;
      this.value = value;
   }

   @Nullable
   @Override
   public V get(K object) {
      Holder<K> holder = this.holderGetter.apply(object);
      return holder.is(this.tag) ? this.value : null;
   }

   @Override
   public void onRegister(Runnable invalidate) {
      NeoForge.EVENT_BUS.addListener(event -> {
         if (event.shouldUpdateStaticData()) {
            invalidate.run();
         }
      });
   }

   public static Holder<BlockEntityType<?>> getBeHolder(BlockEntityType<?> type) {
      ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
      if (key == null) {
         throw new IllegalStateException("Unregistered BlockEntityType: " + type);
      } else {
         return (Holder<BlockEntityType<?>>)BuiltInRegistries.BLOCK_ENTITY_TYPE.getHolder(key).orElseThrow();
      }
   }
}
