package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public interface LevititeBlendDummyInterface {
   default void levititeBlendTick(Level level, BlockPos pos, FluidState state) {
      Set<BlockPos> tickedPositions = LevititeCrystallizerManager.getTickedPositions(level);
      if (!tickedPositions.contains(pos)) {
         LevititeBlendHelper.checkSurroundingSources(level, pos, state);
      }
   }
}
