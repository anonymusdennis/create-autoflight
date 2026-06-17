package dev.eriksonn.aeronautics.index;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.data.TagGen.CreateTagsProvider;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateItemTagsProvider;
import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.eriksonn.aeronautics.Aeronautics;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AeroTags {
   public static void addGenerators() {
      Aeronautics.getRegistrate().addDataGenerator(ProviderType.BLOCK_TAGS, AeroTags.BlockTags::genBlockTags);
      Aeronautics.getRegistrate().addDataGenerator(ProviderType.ITEM_TAGS, AeroTags.ItemTags::genItemTags);
   }

   public static class BlockTags {
      public static final TagKey<Block> AIRTIGHT = create("airtight");
      public static final TagKey<Block> ENVELOPE = create("envelope");
      public static final TagKey<Block> LEVITITE = create("levitite");
      public static final TagKey<Block> LEVITITE_BREAKABLE = create("levitite_breakable");
      public static final TagKey<Block> LEVITITE_CATALYZER = create("levitite_catalyzer");
      public static final TagKey<Block> LEVITITE_SOUL_CATALYZER = create("levitite_soul_catalyzer");
      public static final TagKey<Block> LEVITITE_ADJACENT_CATALYZER = create("levitite_adjacent_catalyzer");
      public static final TagKey<Block> LEVITITE_ADJACENT_SOUL_CATALYZER = create("levitite_adjacent_soul_catalyzer");

      private static TagKey<Block> create(String path) {
         return TagKey.create(Registries.BLOCK, Aeronautics.path(path));
      }

      private static void genBlockTags(RegistrateTagsProvider<Block> provIn) {
         CreateTagsProvider<Block> prov = new CreateTagsProvider(provIn, Block::builtInRegistryHolder);
         prov.tag(AIRTIGHT).addTag(ENVELOPE);
         prov.tag(AIRTIGHT).addTag(net.minecraft.tags.BlockTags.WOOL);
         prov.tag(net.minecraft.tags.BlockTags.DAMPENS_VIBRATIONS).addTag(ENVELOPE);
         prov.tag(LEVITITE_BREAKABLE).add(new Block[]{Blocks.CLAY, Blocks.MUD, Blocks.PACKED_MUD, Blocks.COARSE_DIRT});
         prov.tag(LEVITITE_CATALYZER)
            .add(new Block[]{Blocks.CAMPFIRE, Blocks.MAGMA_BLOCK, Blocks.TORCH, Blocks.WALL_TORCH, (Block)AllBlocks.LIT_BLAZE_BURNER.get(), Blocks.FIRE});
         prov.tag(LEVITITE_ADJACENT_CATALYZER).add(Blocks.NETHERRACK).addTag(net.neoforged.neoforge.common.Tags.Blocks.STORAGE_BLOCKS_COAL);
         prov.tag(LEVITITE_SOUL_CATALYZER).add(new Block[]{Blocks.SOUL_CAMPFIRE, Blocks.SOUL_TORCH, Blocks.SOUL_WALL_TORCH, Blocks.SOUL_FIRE});
         prov.tag(LEVITITE_ADJACENT_SOUL_CATALYZER).addTag(net.minecraft.tags.BlockTags.SOUL_FIRE_BASE_BLOCKS);
      }
   }

   public static class ItemTags {
      public static final TagKey<Item> LEATHERS = AllTags.commonItemTag("leathers");
      public static final TagKey<Item> ARMORS = AllTags.commonItemTag("armors");
      public static final TagKey<Item> HEAD_ARMOR = TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("head_armor"));
      public static final TagKey<Item> IRON_SHEET = AllTags.commonItemTag("plates/iron");
      public static final TagKey<Item> GOLD_SHEET = AllTags.commonItemTag("plates/gold");
      public static final TagKey<Item> MUSIC_DISCS = AllTags.commonItemTag("music_discs");
      public static final TagKey<Item> ENVELOPE = create("envelope");
      public static final TagKey<Item> SHAFTLESS_ENVELOPE = create("shaftless_envelope");
      public static final TagKey<Item> LEVITITE_CATALYZER = create("levitite_catalyzer");
      public static final TagKey<Item> LEVITITE_SOUL_CATALYZER = create("levitite_soul_catalyzer");
      public static final TagKey<Item> LEVITITE_CATALYZER_NO_CONSUME = create("levitite_catalyzer_no_consume");
      public static final TagKey<Item> LEVITITE = create("levitite");
      public static final TagKey<Item> BURNER_FIRE = create("burner_fire");
      public static final TagKey<Item> CONVERTS_TO_CLOUD_SKIPPER = create("converts_to_cloud_skipper");

      private static TagKey<Item> create(String path) {
         return TagKey.create(Registries.ITEM, Aeronautics.path(path));
      }

      public static void genItemTags(RegistrateItemTagsProvider provIn) {
         CreateTagsProvider<Item> prov = new CreateTagsProvider(provIn, Item::builtInRegistryHolder);
         prov.tag(LEVITITE_CATALYZER).add(new Item[]{Items.FLINT_AND_STEEL, Items.FIRE_CHARGE, Items.TORCH, Items.CAMPFIRE});
         prov.tag(LEVITITE_SOUL_CATALYZER).add(new Item[]{Items.SOUL_TORCH, Items.SOUL_CAMPFIRE});
         prov.tag(LEVITITE_CATALYZER_NO_CONSUME).add(new Item[]{Items.TORCH, Items.CAMPFIRE, Items.SOUL_TORCH, Items.SOUL_CAMPFIRE});
         prov.tag(BURNER_FIRE).add(Items.COAL_BLOCK);
      }
   }
}
