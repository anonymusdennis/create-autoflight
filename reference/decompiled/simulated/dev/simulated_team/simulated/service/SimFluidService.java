package dev.simulated_team.simulated.service;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public interface SimFluidService {
   SimFluidService INSTANCE = ServiceUtil.load(SimFluidService.class);

   long mbToLoaderUnits(long var1);

   Fluid getFluidInItem(ItemStack var1);
}
