package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.InputWindowElement;

public class ShowInputInstruction extends FadeInOutInstruction {
   private final InputWindowElement element;

   public ShowInputInstruction(InputWindowElement element, int ticks) {
      super(ticks);
      this.element = element;
   }

   @Override
   protected void show(PonderScene scene) {
      scene.addElement(this.element);
      this.element.setVisible(true);
   }

   @Override
   protected void hide(PonderScene scene) {
      this.element.setVisible(false);
   }

   @Override
   protected void applyFade(PonderScene scene, float fade) {
      this.element.setFade(fade);
   }
}
