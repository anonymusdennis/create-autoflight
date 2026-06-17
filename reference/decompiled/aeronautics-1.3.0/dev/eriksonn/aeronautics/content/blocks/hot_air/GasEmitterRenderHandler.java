package dev.eriksonn.aeronautics.content.blocks.hot_air;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;

public class GasEmitterRenderHandler {
   private final LerpedFloat position = LerpedFloat.linear();
   private final LerpedFloat fade = LerpedFloat.linear();

   public GasEmitterRenderHandler() {
      this.position.chase(0.0, 0.2, Chaser.EXP);
      this.fade.chase(0.0, 0.2, Chaser.EXP);
   }

   public void targetFromRedstoneSignal(int signal) {
      this.targetFromValue((float)signal / 15.0F);
   }

   public void targetFromValue(float value) {
      this.position.updateChaseTarget(value);
   }

   public void tick() {
      this.position.tickChaser();
      this.fade.updateChaseTarget(!(this.position.getChaseTarget() > 0.0F) && !((double)this.position.getValue() > 0.5) ? 0.0F : 1.0F);
      this.fade.tickChaser();
   }

   public int getAlpha(float partialTick) {
      return (int)(this.fade.getValue(partialTick) * 255.0F);
   }

   public float getPosition(float partialTick) {
      return this.position.getValue(partialTick);
   }
}
