package net.createmod.ponder.foundation.instruction;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.foundation.PonderScene;

public class RotateSceneInstruction extends PonderInstruction {
   private final float xRot;
   private final float yRot;
   private final boolean relative;

   public RotateSceneInstruction(float xRot, float yRot, boolean relative) {
      this.xRot = xRot;
      this.yRot = yRot;
      this.relative = relative;
   }

   @Override
   public boolean isComplete() {
      return true;
   }

   @Override
   public void tick(PonderScene scene) {
      PonderScene.SceneTransform transform = scene.getTransform();
      float targetX = this.relative ? transform.xRotation.getChaseTarget() + this.xRot : this.xRot;
      float targetY = this.relative ? transform.yRotation.getChaseTarget() + this.yRot : this.yRot;
      transform.xRotation.chase((double)targetX, 0.1F, LerpedFloat.Chaser.EXP);
      transform.yRotation.chase((double)targetY, 0.1F, LerpedFloat.Chaser.EXP);
   }
}
