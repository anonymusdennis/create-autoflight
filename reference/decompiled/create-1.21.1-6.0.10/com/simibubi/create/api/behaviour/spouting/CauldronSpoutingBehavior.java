package com.simibubi.create.api.behaviour.spouting;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public enum CauldronSpoutingBehavior implements BlockSpoutingBehaviour {
   INSTANCE;

   public static final SimpleRegistry<Fluid, CauldronSpoutingBehavior.CauldronInfo> CAULDRON_INFO = (SimpleRegistry<Fluid, CauldronSpoutingBehavior.CauldronInfo>)Util.make(
      () -> {
         SimpleRegistry<Fluid, CauldronSpoutingBehavior.CauldronInfo> registry = SimpleRegistry.create();
         registry.register(Fluids.WATER, new CauldronSpoutingBehavior.CauldronInfo(250, Blocks.WATER_CAULDRON));
         registry.register(Fluids.LAVA, new CauldronSpoutingBehavior.CauldronInfo(1000, Blocks.LAVA_CAULDRON));
         return registry;
      }
   );

   @Override
   public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
      CauldronSpoutingBehavior.CauldronInfo info = CAULDRON_INFO.get(availableFluid.getFluid());
      if (info == null) {
         return 0;
      } else if (availableFluid.getAmount() < info.amount) {
         return 0;
      } else {
         if (!simulate) {
            level.setBlockAndUpdate(pos, info.cauldron);
         }

         return info.amount;
      }
   }

   public static record CauldronInfo(int amount, BlockState cauldron) {
      public CauldronInfo(int amount, Block block) {
         this(amount, block.defaultBlockState());
      }
   }
}
