package dev.ryanhcode.sable.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockSubLevelAssemblyListener {
   default void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
   }

   void afterMove(ServerLevel var1, ServerLevel var2, BlockState var3, BlockPos var4, BlockPos var5);
}
