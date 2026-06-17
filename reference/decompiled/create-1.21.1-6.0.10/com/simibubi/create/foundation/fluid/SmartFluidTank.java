package com.simibubi.create.foundation.fluid;

import java.util.function.Consumer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class SmartFluidTank extends FluidTank {
   private Consumer<FluidStack> updateCallback;

   public SmartFluidTank(int capacity, Consumer<FluidStack> updateCallback) {
      super(capacity);
      this.updateCallback = updateCallback;
   }

   protected void onContentsChanged() {
      super.onContentsChanged();
      this.updateCallback.accept(this.getFluid());
   }

   public void setFluid(FluidStack stack) {
      super.setFluid(stack);
      this.updateCallback.accept(stack);
   }
}
