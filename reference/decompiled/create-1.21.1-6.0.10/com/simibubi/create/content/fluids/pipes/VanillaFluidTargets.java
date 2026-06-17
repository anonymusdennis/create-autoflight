package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.AllFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;

public class VanillaFluidTargets {
   public static boolean canProvideFluidWithoutCapability(BlockState state) {
      if (state.hasProperty(BlockStateProperties.LEVEL_HONEY)) {
         return true;
      } else if (state.is(Blocks.CAULDRON)) {
         return true;
      } else {
         return state.is(Blocks.LAVA_CAULDRON) ? true : state.is(Blocks.WATER_CAULDRON);
      }
   }

   public static FluidStack drainBlock(Level level, BlockPos pos, BlockState state, boolean simulate) {
      if (state.hasProperty(BlockStateProperties.LEVEL_HONEY) && (Integer)state.getValue(BlockStateProperties.LEVEL_HONEY) >= 5) {
         if (!simulate) {
            level.setBlock(pos, (BlockState)state.setValue(BlockStateProperties.LEVEL_HONEY, 0), 3);
         }

         return new FluidStack(((Flowing)AllFluids.HONEY.get()).getSource(), 250);
      } else if (state.is(Blocks.LAVA_CAULDRON)) {
         if (!simulate) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
         }

         return new FluidStack(Fluids.LAVA, 1000);
      } else if (!state.is(Blocks.WATER_CAULDRON) || !(state.getBlock() instanceof LayeredCauldronBlock lcb)) {
         return FluidStack.EMPTY;
      } else if (!lcb.isFull(state)) {
         return FluidStack.EMPTY;
      } else {
         if (!simulate) {
            level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
         }

         return new FluidStack(Fluids.WATER, 1000);
      }
   }
}
