package dev.simulated_team.simulated.multiloader.tanks.neoforge;

import dev.simulated_team.simulated.multiloader.tanks.CFluidType;
import dev.simulated_team.simulated.multiloader.tanks.SingleTank;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

public class SingleTankWrapper extends FluidTank {
   private final SingleTank tank;

   public SingleTankWrapper(SingleTank tank) {
      super((int)tank.capacity);
      this.tank = tank;
   }

   public static FluidStack fromCType(CFluidType type, int amount) {
      return new FluidStack(type.fluid.builtInRegistryHolder(), amount);
   }

   public static CFluidType toCType(FluidStack stack) {
      return new CFluidType(stack.getFluid(), stack.getComponents());
   }

   public int fill(FluidStack resource, FluidAction action) {
      return (int)this.tank.insert(toCType(resource), (long)resource.getAmount(), action.simulate());
   }

   @NotNull
   public FluidStack drain(int maxDrain, FluidAction action) {
      return fromCType(this.tank.type, (int)this.tank.extract(this.tank.type, (long)maxDrain, action.simulate()));
   }

   @NotNull
   public FluidStack getFluid() {
      return fromCType(this.tank.type, (int)this.tank.amount);
   }
}
