package dev.simulated_team.simulated.ponder.instructions;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class OffsetBreakParticlesInstruction extends PonderInstruction {
   AABB bb;
   BlockState state;

   public boolean isComplete() {
      return true;
   }

   public void addBlockDestroyEffects(PonderLevel level, AABB bb, BlockState state) {
      double d1 = Math.min(1.0, bb.maxX - bb.minX);
      double d2 = Math.min(1.0, bb.maxY - bb.minY);
      double d3 = Math.min(1.0, bb.maxZ - bb.minZ);
      int i = Math.max(2, Mth.ceil(d1 / 0.25));
      int j = Math.max(2, Mth.ceil(d2 / 0.25));
      int k = Math.max(2, Mth.ceil(d3 / 0.25));

      for (int l = 0; l < i; l++) {
         for (int i1 = 0; i1 < j; i1++) {
            for (int j1 = 0; j1 < k; j1++) {
               double subPosX = ((double)l + 0.5) / (double)i;
               double subPosY = ((double)i1 + 0.5) / (double)j;
               double subPosZ = ((double)j1 + 0.5) / (double)k;
               double posX = subPosX * d1 + bb.minX;
               double posY = subPosY * d2 + bb.minY;
               double posZ = subPosZ * d3 + bb.minZ;
               level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), posX, posY, posZ, subPosX - 0.5, subPosY - 0.5, subPosZ - 0.5);
            }
         }
      }
   }

   public OffsetBreakParticlesInstruction(AABB bb, BlockState state) {
      this.bb = bb;
      this.state = state;
   }

   public void tick(PonderScene scene) {
      PonderLevel level = scene.getWorld();
      this.addBlockDestroyEffects(level, this.bb, this.state);
   }
}
