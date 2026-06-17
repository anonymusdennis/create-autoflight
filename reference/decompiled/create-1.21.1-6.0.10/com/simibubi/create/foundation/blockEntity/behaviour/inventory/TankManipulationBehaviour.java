package com.simibubi.create.foundation.blockEntity.behaviour.inventory;

import com.google.common.base.Predicates;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public class TankManipulationBehaviour extends CapManipulationBehaviourBase<IFluidHandler, TankManipulationBehaviour> {
   public static final BehaviourType<TankManipulationBehaviour> OBSERVE = new BehaviourType<>();
   private BehaviourType<TankManipulationBehaviour> behaviourType;

   public TankManipulationBehaviour(SmartBlockEntity be, CapManipulationBehaviourBase.InterfaceProvider target) {
      this(OBSERVE, be, target);
   }

   private TankManipulationBehaviour(BehaviourType<TankManipulationBehaviour> type, SmartBlockEntity be, CapManipulationBehaviourBase.InterfaceProvider target) {
      super(be, target);
      this.behaviourType = type;
   }

   public FluidStack extractAny() {
      if (!this.hasInventory()) {
         return FluidStack.EMPTY;
      } else {
         IFluidHandler inventory = this.getInventory();
         Predicate<FluidStack> filterTest = this.getFilterTest(Predicates.alwaysTrue());

         for (int i = 0; i < inventory.getTanks(); i++) {
            FluidStack fluidInTank = inventory.getFluidInTank(i);
            if (!fluidInTank.isEmpty() && filterTest.test(fluidInTank)) {
               FluidStack drained = inventory.drain(fluidInTank, this.simulateNext ? FluidAction.SIMULATE : FluidAction.EXECUTE);
               if (!drained.isEmpty()) {
                  return drained;
               }
            }
         }

         return FluidStack.EMPTY;
      }
   }

   protected Predicate<FluidStack> getFilterTest(Predicate<FluidStack> customFilter) {
      Predicate<FluidStack> test = customFilter;
      FilteringBehaviour filter = this.blockEntity.getBehaviour(FilteringBehaviour.TYPE);
      if (filter != null) {
         test = customFilter.and(filter::test);
      }

      return test;
   }

   @Override
   protected BlockCapability<IFluidHandler, Direction> capability() {
      return FluidHandler.BLOCK;
   }

   @Override
   public BehaviourType<?> getType() {
      return this.behaviourType;
   }
}
