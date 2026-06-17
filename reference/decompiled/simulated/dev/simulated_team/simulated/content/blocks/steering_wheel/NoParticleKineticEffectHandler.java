package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;

public class NoParticleKineticEffectHandler extends KineticEffectHandler {
   public NoParticleKineticEffectHandler(KineticBlockEntity kte) {
      super(kte);
   }

   public void spawnRotationIndicators() {
   }
}
