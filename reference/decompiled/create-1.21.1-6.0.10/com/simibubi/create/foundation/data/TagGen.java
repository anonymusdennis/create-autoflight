package com.simibubi.create.foundation.data;

import com.simibubi.create.foundation.data.recipe.CommonMetal;
import com.simibubi.create.foundation.data.recipe.Mods;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.Holder.Reference;
import net.minecraft.data.tags.TagsProvider.TagAppender;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class TagGen {
   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOrPickaxe() {
      return b -> b.tag(new TagKey[]{BlockTags.MINEABLE_WITH_AXE}).tag(new TagKey[]{BlockTags.MINEABLE_WITH_PICKAXE});
   }

   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> axeOnly() {
      return b -> b.tag(new TagKey[]{BlockTags.MINEABLE_WITH_AXE});
   }

   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, BlockBuilder<T, P>> pickaxeOnly() {
      return b -> b.tag(new TagKey[]{BlockTags.MINEABLE_WITH_PICKAXE});
   }

   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
      CommonMetal.ItemLikeTag tag
   ) {
      return tagBlockAndItem(Map.of(tag.blocks(), tag.items()));
   }

   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
      TagKey<Block> blockTag, TagKey<Item> itemTag
   ) {
      return tagBlockAndItem(Map.of(blockTag, itemTag));
   }

   public static <T extends Block, P> NonNullFunction<BlockBuilder<T, P>, ItemBuilder<BlockItem, BlockBuilder<T, P>>> tagBlockAndItem(
      Map<TagKey<Block>, TagKey<Item>> tags
   ) {
      return b -> {
         for (TagKey<Block> blockTag : tags.keySet()) {
            b.tag(new TagKey[]{blockTag});
         }

         ItemBuilder<BlockItem, BlockBuilder<T, P>> item = b.item();

         for (TagKey<Item> itemTag : tags.values()) {
            item.tag(new TagKey[]{itemTag});
         }

         return item;
      };
   }

   public static <T extends TagAppender<?>> T addOptional(T appender, Mods mod, String id) {
      appender.addOptional(mod.asResource(id));
      return appender;
   }

   public static <T extends TagAppender<?>> T addOptional(T appender, Mods mod, List<String> ids) {
      for (String id : ids) {
         appender.addOptional(mod.asResource(id));
      }

      return appender;
   }

   public static class CreateTagAppender<T> extends TagAppender<T> {
      private final Function<T, ResourceKey<T>> keyExtractor;

      public CreateTagAppender(TagBuilder pBuilder, Function<T, ResourceKey<T>> pKeyExtractor) {
         super(pBuilder);
         this.keyExtractor = pKeyExtractor;
      }

      public TagGen.CreateTagAppender<T> add(T entry) {
         this.add(this.keyExtractor.apply(entry));
         return this;
      }

      @SafeVarargs
      public final TagGen.CreateTagAppender<T> add(T... entries) {
         Stream.<T>of(entries).map(this.keyExtractor).forEach(this::add);
         return this;
      }
   }

   public static class CreateTagsProvider<T> {
      private final RegistrateTagsProvider<T> provider;
      private final Function<T, ResourceKey<T>> keyExtractor;

      public CreateTagsProvider(RegistrateTagsProvider<T> provider, Function<T, Reference<T>> refExtractor) {
         this.provider = provider;
         this.keyExtractor = refExtractor.andThen(Reference::key);
      }

      public TagGen.CreateTagAppender<T> tag(TagKey<T> tag) {
         TagBuilder tagbuilder = this.getOrCreateRawBuilder(tag);
         return new TagGen.CreateTagAppender<>(tagbuilder, this.keyExtractor);
      }

      public TagBuilder getOrCreateRawBuilder(TagKey<T> tag) {
         return this.provider.addTag(tag).getInternalBuilder();
      }
   }
}
