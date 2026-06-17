package dev.eriksonn.aeronautics.neoforge.content.fluids.levitite;

import dev.eriksonn.aeronautics.api.levitite_blend_crystallization.LevititeBlendDummyInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Properties;

public class LevititeBlendNeoForge extends BaseFlowingFluid implements LevititeBlendDummyInterface {
   public LevititeBlendNeoForge(Properties properties) {
      super(properties);
   }

   public void tick(Level level, BlockPos pos, FluidState state) {
      super.tick(level, pos, state);
      LevititeBlendDummyInterface.super.levititeBlendTick(level, pos, state);
   }

   public boolean isSource(FluidState fluidState) {
      return true;
   }

   public int getAmount(FluidState fluidState) {
      return 8;
   }
}
