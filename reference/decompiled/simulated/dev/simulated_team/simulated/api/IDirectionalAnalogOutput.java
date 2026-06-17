package dev.simulated_team.simulated.api;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IDirectionalAnalogOutput {
   int getAnalogOutputSignalFrom(BlockState var1, Level var2, BlockPos var3, Direction var4);
}
