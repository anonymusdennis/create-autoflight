package net.createmod.ponder.api.scene;

import net.createmod.ponder.api.ParticleEmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;

public interface EffectInstructions {
   void emitParticles(Vec3 var1, ParticleEmitter var2, float var3, int var4);

   <T extends ParticleOptions> ParticleEmitter simpleParticleEmitter(T var1, Vec3 var2);

   <T extends ParticleOptions> ParticleEmitter particleEmitterWithinBlockSpace(T var1, Vec3 var2);

   void indicateRedstone(BlockPos var1);

   void indicateSuccess(BlockPos var1);

   void createRedstoneParticles(BlockPos var1, int var2, int var3);
}
