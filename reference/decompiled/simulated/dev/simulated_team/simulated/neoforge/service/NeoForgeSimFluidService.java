package dev.simulated_team.simulated.neoforge.service;

import dev.simulated_team.simulated.service.SimFluidService;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public class NeoForgeSimFluidService implements SimFluidService {
   @Override
   public long mbToLoaderUnits(long mb) {
      return mb;
   }

   @Override
   public Fluid getFluidInItem(ItemStack stack) {
      IFluidHandlerItem handler = (IFluidHandlerItem)stack.getCapability(FluidHandler.ITEM);
      if (handler != null) {
         FluidStack fluid = handler.getFluidInTank(0);
         if (!fluid.isEmpty()) {
            return fluid.getFluid();
         }
      }

      return null;
   }
}
