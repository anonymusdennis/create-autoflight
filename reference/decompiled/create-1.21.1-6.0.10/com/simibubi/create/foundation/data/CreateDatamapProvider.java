package com.simibubi.create.foundation.data;

import com.simibubi.create.foundation.block.CopperRegistries;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.common.data.DataMapProvider.Builder;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Oxidizable;
import net.neoforged.neoforge.registries.datamaps.builtin.Waxable;

public class CreateDatamapProvider extends DataMapProvider {
   public CreateDatamapProvider(PackOutput packOutput, CompletableFuture<Provider> lookupProvider) {
      super(packOutput, lookupProvider);
   }

   protected void gather(Provider provider) {
      Builder<Oxidizable, Block> oxidizables = this.builder(NeoForgeDataMaps.OXIDIZABLES);
      CopperRegistries.getWeatheringView().forEach((now, after) -> add(oxidizables, (Holder<Block>)now, new Oxidizable((Block)after.value())));
      Builder<Waxable, Block> waxables = this.builder(NeoForgeDataMaps.WAXABLES);
      CopperRegistries.getWaxableView().forEach((now, after) -> add(waxables, (Holder<Block>)now, new Waxable((Block)after.value())));
   }

   public static <T> void add(Builder<T, Block> b, Holder<Block> now, T after) {
      b.add(now, after, false, new ICondition[0]);
   }
}
