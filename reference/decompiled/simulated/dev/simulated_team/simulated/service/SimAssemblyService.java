package dev.simulated_team.simulated.service;

import net.minecraft.world.level.block.state.BlockState;

public interface SimAssemblyService {
   SimAssemblyService INSTANCE = ServiceUtil.load(SimAssemblyService.class);

   boolean canStickTo(BlockState var1, BlockState var2);
}
