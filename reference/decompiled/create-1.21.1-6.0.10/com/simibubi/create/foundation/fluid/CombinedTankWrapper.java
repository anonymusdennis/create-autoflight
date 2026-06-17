package com.simibubi.create.foundation.fluid;

import net.createmod.catnip.data.Iterate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler;

public class CombinedTankWrapper implements IFluidHandler {
   protected final IFluidHandler[] itemHandler;
   protected final int[] baseIndex;
   protected final int tankCount;
   protected boolean enforceVariety;

   public CombinedTankWrapper(IFluidHandler... fluidHandlers) {
      this.itemHandler = fluidHandlers;
      this.baseIndex = new int[fluidHandlers.length];
      int index = 0;

      for (int i = 0; i < fluidHandlers.length; i++) {
         index += fluidHandlers[i].getTanks();
         this.baseIndex[i] = index;
      }

      this.tankCount = index;
   }

   public CombinedTankWrapper enforceVariety() {
      this.enforceVariety = true;
      return this;
   }

   public int getTanks() {
      return this.tankCount;
   }

   public FluidStack getFluidInTank(int tank) {
      int index = this.getIndexForSlot(tank);
      IFluidHandler handler = this.getHandlerFromIndex(index);
      tank = this.getSlotFromIndex(tank, index);
      return handler.getFluidInTank(tank);
   }

   public int getTankCapacity(int tank) {
      int index = this.getIndexForSlot(tank);
      IFluidHandler handler = this.getHandlerFromIndex(index);
      int localSlot = this.getSlotFromIndex(tank, index);
      return handler.getTankCapacity(localSlot);
   }

   public boolean isFluidValid(int tank, FluidStack stack) {
      int index = this.getIndexForSlot(tank);
      IFluidHandler handler = this.getHandlerFromIndex(index);
      int localSlot = this.getSlotFromIndex(tank, index);
      return handler.isFluidValid(localSlot, stack);
   }

   public int fill(FluidStack resource, FluidAction action) {
      if (resource.isEmpty()) {
         return 0;
      } else {
         int filled = 0;
         resource = resource.copy();
         boolean fittingHandlerFound = false;

         for (boolean searchPass : Iterate.trueAndFalse) {
            for (IFluidHandler iFluidHandler : this.itemHandler) {
               for (int i = 0; i < iFluidHandler.getTanks(); i++) {
                  if (searchPass && FluidStack.isSameFluidSameComponents(iFluidHandler.getFluidInTank(i), resource)) {
                     fittingHandlerFound = true;
                  }
               }

               if (!searchPass || fittingHandlerFound) {
                  int filledIntoCurrent = iFluidHandler.fill(resource, action);
                  resource.shrink(filledIntoCurrent);
                  filled += filledIntoCurrent;
                  if (resource.isEmpty() || fittingHandlerFound && (this.enforceVariety || filledIntoCurrent != 0)) {
                     return filled;
                  }
               }
            }
         }

         return filled;
      }
   }

   public FluidStack drain(FluidStack resource, FluidAction action) {
      if (resource.isEmpty()) {
         return resource;
      } else {
         FluidStack drained = FluidStack.EMPTY;
         resource = resource.copy();

         for (IFluidHandler iFluidHandler : this.itemHandler) {
            FluidStack drainedFromCurrent = iFluidHandler.drain(resource, action);
            int amount = drainedFromCurrent.getAmount();
            resource.shrink(amount);
            if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || FluidStack.isSameFluidSameComponents(drainedFromCurrent, drained))) {
               drained = new FluidStack(drainedFromCurrent.getFluidHolder(), amount + drained.getAmount(), drainedFromCurrent.getComponentsPatch());
            }

            if (resource.isEmpty()) {
               break;
            }
         }

         return drained;
      }
   }

   public FluidStack drain(int maxDrain, FluidAction action) {
      FluidStack drained = FluidStack.EMPTY;

      for (IFluidHandler iFluidHandler : this.itemHandler) {
         FluidStack drainedFromCurrent = iFluidHandler.drain(maxDrain, action);
         int amount = drainedFromCurrent.getAmount();
         maxDrain -= amount;
         if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || FluidStack.isSameFluidSameComponents(drainedFromCurrent, drained))) {
            drained = new FluidStack(drainedFromCurrent.getFluidHolder(), amount + drained.getAmount(), drainedFromCurrent.getComponentsPatch());
         }

         if (maxDrain == 0) {
            break;
         }
      }

      return drained;
   }

   protected int getIndexForSlot(int slot) {
      if (slot < 0) {
         return -1;
      } else {
         for (int i = 0; i < this.baseIndex.length; i++) {
            if (slot - this.baseIndex[i] < 0) {
               return i;
            }
         }

         return -1;
      }
   }

   protected IFluidHandler getHandlerFromIndex(int index) {
      return (IFluidHandler)(index >= 0 && index < this.itemHandler.length ? this.itemHandler[index] : EmptyFluidHandler.INSTANCE);
   }

   protected int getSlotFromIndex(int slot, int index) {
      return index > 0 && index < this.baseIndex.length ? slot - this.baseIndex[index - 1] : slot;
   }
}
