package net.createmod.ponder.foundation.element;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.api.element.AnimatedOverlayElement;

public abstract class AnimatedOverlayElementBase extends PonderElementBase implements AnimatedOverlayElement {
   protected LerpedFloat fade = LerpedFloat.linear().startWithValue(0.0);

   @Override
   public void setFade(float fade) {
      this.fade.setValue((double)fade);
   }

   @Override
   public float getFade(float partialTicks) {
      return this.fade.getValue(partialTicks);
   }
}
