package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BoilerHeaters {
   public static void registerDefaults() {
      BoilerHeater.REGISTRY.register((Block)AllBlocks.BLAZE_BURNER.get(), BoilerHeater.BLAZE_BURNER);
      BoilerHeater.REGISTRY.registerProvider(SimpleRegistry.Provider.forBlockTag(AllTags.AllBlockTags.PASSIVE_BOILER_HEATERS.tag, BoilerHeater.PASSIVE));
   }

   public static int passive(Level level, BlockPos pos, BlockState state) {
      return BlockHelper.isNotUnheated(state) ? 0 : -1;
   }

   public static int blazeBurner(Level level, BlockPos pos, BlockState state) {
      BlazeBurnerBlock.HeatLevel value = (BlazeBurnerBlock.HeatLevel)state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
      if (value == BlazeBurnerBlock.HeatLevel.NONE) {
         return -1;
      } else if (value == BlazeBurnerBlock.HeatLevel.SEETHING) {
         return 2;
      } else {
         return value.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING) ? 1 : 0;
      }
   }
}
