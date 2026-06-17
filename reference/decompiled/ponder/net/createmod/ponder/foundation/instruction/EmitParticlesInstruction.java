package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.Ponder;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.world.phys.Vec3;

public class EmitParticlesInstruction extends TickingInstruction {
   private final Vec3 anchor;
   private final ParticleEmitter emitter;
   private final float runsPerTick;

   public EmitParticlesInstruction(Vec3 anchor, ParticleEmitter emitter, float runsPerTick, int ticks) {
      super(false, ticks);
      this.anchor = anchor;
      this.emitter = emitter;
      this.runsPerTick = runsPerTick;
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      int runs = (int)this.runsPerTick;
      if (Ponder.RANDOM.nextFloat() < this.runsPerTick - (float)runs) {
         runs++;
      }

      for (int i = 0; i < runs; i++) {
         this.emitter.create(scene.getWorld(), this.anchor.x, this.anchor.y, this.anchor.z);
      }
   }
}
