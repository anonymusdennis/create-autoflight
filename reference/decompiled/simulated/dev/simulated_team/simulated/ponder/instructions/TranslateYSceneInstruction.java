package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.mixin_interface.ponder.PonderSceneExtension;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.util.Mth;

public class TranslateYSceneInstruction extends TickingInstruction {
   float yOffset;
   float oldY;
   int ticks;
   int progress;
   FloatUnaryOperator smoothing;

   public TranslateYSceneInstruction(float yOffset, int ticks) {
      this(yOffset, ticks, f -> f);
   }

   public TranslateYSceneInstruction(float yOffset, int ticks, FloatUnaryOperator smoothing) {
      super(false, ticks + 1);
      this.yOffset = yOffset;
      this.ticks = ticks;
      this.smoothing = smoothing;
   }

   public void onScheduled(PonderScene scene) {
      super.onScheduled(scene);
      ((PonderSceneExtension)scene).simulated$setYOffset(0.0F);
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      this.oldY = scene.getYOffset();
      this.progress = 0;
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      this.progress++;
      float percentage = Mth.clamp((float)this.progress / (float)this.ticks, 0.0F, 1.0F);
      percentage = this.smoothing.apply(percentage);
      float currentScale = (this.yOffset - this.oldY) * percentage + this.oldY;
      ((PonderSceneExtension)scene).simulated$setYOffset(currentScale);
   }
}
