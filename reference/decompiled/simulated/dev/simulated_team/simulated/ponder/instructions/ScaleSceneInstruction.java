package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.mixin_interface.ponder.PonderSceneExtension;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;

public class ScaleSceneInstruction extends TickingInstruction {
   private final float scaleFactor;
   private float oldScale;
   private final int ticks;
   private int progress;

   public ScaleSceneInstruction(float scaleFactor, int ticks) {
      super(false, ticks);
      this.scaleFactor = scaleFactor;
      this.ticks = ticks;
   }

   public void onScheduled(PonderScene scene) {
      super.onScheduled(scene);
      ((PonderSceneExtension)scene).simulated$setScaleFactor(1.0F);
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.oldScale = scene.getScaleFactor();
      this.progress = 0;
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      this.progress++;
      float percentage = (float)this.progress / (float)this.ticks;
      percentage = percentage * percentage * (3.0F - 2.0F * percentage);
      float currentScale = (this.scaleFactor - this.oldScale) * percentage + this.oldScale;
      ((PonderSceneExtension)scene).simulated$setScaleFactor(currentScale);
   }
}
