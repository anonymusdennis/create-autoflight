package com.simibubi.create.compat;

import java.util.Optional;
import java.util.function.Supplier;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.loading.LoadingModList;

public enum Mods {
   AETHER,
   AETHER_II,
   BETTEREND,
   COMPUTERCRAFT,
   CURIOS,
   DYNAMICTREES,
   JEI,
   FUNCTIONALSTORAGE,
   OCCULTISM,
   PACKETFIXER,
   SOPHISTICATEDBACKPACKS,
   SOPHISTICATEDSTORAGE,
   STORAGEDRAWERS,
   TCONSTRUCT,
   FRAMEDBLOCKS,
   XLPACKETS,
   MODERNUI,
   FTBCHUNKS,
   JOURNEYMAP,
   XAEROWORLDMAP,
   FTBLIBRARY,
   SODIUM,
   INVENTORYSORTER,
   FARMERSDELIGHT;

   private final String id = Lang.asId(this.name());
   private final boolean isLoaded = LoadingModList.get().getModFileById(this.id) != null;

   public String id() {
      return this.id;
   }

   public ResourceLocation rl(String path) {
      return ResourceLocation.fromNamespaceAndPath(this.id, path);
   }

   public Block getBlock(String id) {
      return (Block)BuiltInRegistries.BLOCK.get(this.rl(id));
   }

   public Item getItem(String id) {
      return (Item)BuiltInRegistries.ITEM.get(this.rl(id));
   }

   public boolean contains(ItemLike entry) {
      if (!this.isLoaded()) {
         return false;
      } else {
         Item asItem = entry.asItem();
         return asItem != null && RegisteredObjectsHelper.getKeyOrThrow(asItem).getNamespace().equals(this.id);
      }
   }

   public boolean isLoaded() {
      return this.isLoaded;
   }

   public <T> Optional<T> runIfInstalled(Supplier<Supplier<T>> toRun) {
      return this.isLoaded() ? Optional.of(toRun.get().get()) : Optional.empty();
   }

   public void executeIfInstalled(Supplier<Runnable> toExecute) {
      if (this.isLoaded()) {
         toExecute.get().run();
      }
   }
}
