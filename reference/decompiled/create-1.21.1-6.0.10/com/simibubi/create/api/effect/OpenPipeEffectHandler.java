package com.simibubi.create.api.effect;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

@FunctionalInterface
public interface OpenPipeEffectHandler {
   SimpleRegistry<Fluid, OpenPipeEffectHandler> REGISTRY = SimpleRegistry.create();

   void apply(Level var1, AABB var2, FluidStack var3);
}
