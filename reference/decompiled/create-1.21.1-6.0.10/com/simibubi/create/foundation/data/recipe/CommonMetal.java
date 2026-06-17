package com.simibubi.create.foundation.data.recipe;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.lang.Lang;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public enum CommonMetal {
   IRON(Mods.VANILLA),
   GOLD(Mods.VANILLA),
   COPPER(Mods.VANILLA),
   ZINC(Mods.CREATE),
   BRASS(false, Mods.CREATE),
   ALUMINUM(Mods.IE, Mods.IC2),
   LEAD(Mods.MEK, Mods.TH, Mods.IE, Mods.OREGANIZED),
   NICKEL(Mods.TH, Mods.IE),
   OSMIUM(Mods.MEK),
   PLATINUM(),
   QUICKSILVER(),
   SILVER(Mods.TH, Mods.IE, Mods.IC2, Mods.OREGANIZED, Mods.GS, Mods.IF),
   TIN(Mods.TH, Mods.MEK, Mods.IC2),
   URANIUM(Mods.MEK, Mods.IE, Mods.IC2),
   CONSTANTAN(false, Mods.IE),
   ELECTRUM(false, Mods.IE),
   STEEL(false, Mods.IE);

   private static final Map<Mods, Set<CommonMetal>> metalsOfMods = (Map<Mods, Set<CommonMetal>>)Util.make(() -> {
      Map<Mods, Set<CommonMetal>> map = new EnumMap<>(Mods.class);

      for (Mods mod : Mods.values()) {
         Set<CommonMetal> set = EnumSet.noneOf(CommonMetal.class);

         for (CommonMetal metal : values()) {
            if (metal.mods.contains(mod)) {
               set.add(metal);
            }
         }

         map.put(mod, set);
      }

      return map;
   });
   public final String name = Lang.asId(this.name());
   public final Set<Mods> mods;
   public final boolean isNatural;
   public final CommonMetal.ItemLikeTag ores;
   public final TagKey<Item> rawOres;
   public final CommonMetal.ItemLikeTag rawStorageBlocks;
   public final TagKey<Item> ingots;
   public final CommonMetal.ItemLikeTag storageBlocks;
   public final TagKey<Item> nuggets;
   public final TagKey<Item> plates;

   private CommonMetal(Mods... mods) {
      this(true, mods);
   }

   private CommonMetal(boolean natural, Mods... mods) {
      this.mods = mods.length == 0 ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(Set.of(mods)));
      this.isNatural = natural;
      this.ores = new CommonMetal.ItemLikeTag("ores/" + this.name);
      this.rawOres = itemTag("raw_materials/" + this.name);
      this.rawStorageBlocks = new CommonMetal.ItemLikeTag("storage_blocks/raw_" + this.name);
      this.ingots = itemTag("ingots/" + this.name);
      this.storageBlocks = new CommonMetal.ItemLikeTag("storage_blocks/" + this.name);
      this.nuggets = itemTag("nuggets/" + this.name);
      this.plates = itemTag("plates/" + this.name);
   }

   public String getName(Mods mod) {
      return this == ALUMINUM && mod == Mods.IC2 ? "aluminium" : this.name;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public static Set<CommonMetal> of(Mods mod) {
      return metalsOfMods.get(mod);
   }

   private static TagKey<Item> itemTag(String path) {
      return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
   }

   private static TagKey<Block> blockTag(String path) {
      return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", path));
   }

   public static record ItemLikeTag(TagKey<Item> items, TagKey<Block> blocks) {
      private ItemLikeTag(String path) {
         this(CommonMetal.itemTag(path), CommonMetal.blockTag(path));
      }
   }
}
