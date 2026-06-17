package com.simibubi.create.api.contraption.storage.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;

public abstract class WrapperMountedFluidStorage<T extends IFluidHandler> extends MountedFluidStorage {
   protected final T wrapped;

   protected WrapperMountedFluidStorage(MountedFluidStorageType<?> type, T wrapped) {
      super(type);
      this.wrapped = wrapped;
   }

   public int getTanks() {
      return this.wrapped.getTanks();
   }

   @NotNull
   public FluidStack getFluidInTank(int tank) {
      return this.wrapped.getFluidInTank(tank);
   }

   public int getTankCapacity(int tank) {
      return this.wrapped.getTankCapacity(tank);
   }

   public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
      return this.wrapped.isFluidValid(tank, stack);
   }

   public int fill(FluidStack resource, FluidAction action) {
      return this.wrapped.fill(resource, action);
   }

   @NotNull
   public FluidStack drain(FluidStack resource, FluidAction action) {
      return this.wrapped.drain(resource, action);
   }

   @NotNull
   public FluidStack drain(int maxDrain, FluidAction action) {
      return this.wrapped.drain(maxDrain, action);
   }
}
