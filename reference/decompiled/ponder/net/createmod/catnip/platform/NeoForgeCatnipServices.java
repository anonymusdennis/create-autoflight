package net.createmod.catnip.platform;

import net.createmod.catnip.platform.services.ModFluidHelper;
import net.createmod.catnip.render.FluidRenderHelper;
import net.neoforged.neoforge.fluids.FluidStack;

public class NeoForgeCatnipServices {
   public static final ModFluidHelper<FluidStack> FLUID_HELPER = (ModFluidHelper<FluidStack>)CatnipServices.FLUID_HELPER;
   public static final FluidRenderHelper<FluidStack> FLUID_RENDERER = (FluidRenderHelper<FluidStack>)CatnipServices.FLUID_RENDERER;
}
