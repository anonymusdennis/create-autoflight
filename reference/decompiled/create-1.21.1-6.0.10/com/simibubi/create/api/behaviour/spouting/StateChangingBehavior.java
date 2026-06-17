package com.simibubi.create.api.behaviour.spouting;

import com.simibubi.create.content.fluids.spout.SpoutBlockEntity;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

public record StateChangingBehavior(int amount, Predicate<Fluid> fluidTest, Predicate<BlockState> canFill, UnaryOperator<BlockState> fillFunction)
   implements BlockSpoutingBehaviour {
   @Override
   public int fillBlock(Level level, BlockPos pos, SpoutBlockEntity spout, FluidStack availableFluid, boolean simulate) {
      if (availableFluid.getAmount() >= this.amount && this.fluidTest.test(availableFluid.getFluid())) {
         BlockState state = level.getBlockState(pos);
         if (!this.canFill.test(state)) {
            return 0;
         } else {
            if (!simulate) {
               BlockState newState = this.fillFunction.apply(state);
               level.setBlockAndUpdate(pos, newState);
            }

            return this.amount;
         }
      } else {
         return 0;
      }
   }

   public static BlockSpoutingBehaviour setTo(int amount, Predicate<Fluid> fluidTest, Block block) {
      return setTo(amount, fluidTest, block.defaultBlockState());
   }

   public static BlockSpoutingBehaviour setTo(int amount, Predicate<Fluid> fluidTest, BlockState newState) {
      return new StateChangingBehavior(amount, fluidTest, state -> true, state -> newState);
   }

   public static BlockSpoutingBehaviour incrementingState(int amount, Predicate<Fluid> fluidTest, IntegerProperty property) {
      int max = property.getPossibleValues().stream().max(Integer::compareTo).orElseThrow();
      Predicate<BlockState> canFill = state -> state.hasProperty(property) && (Integer)state.getValue(property) < max;
      UnaryOperator<BlockState> fillFunction = state -> (BlockState)state.setValue(property, (Integer)state.getValue(property) + 1);
      return new StateChangingBehavior(amount, fluidTest, canFill, fillFunction);
   }
}
