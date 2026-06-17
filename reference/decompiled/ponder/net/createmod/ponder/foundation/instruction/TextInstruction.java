package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.OutlinerElement;
import net.createmod.ponder.foundation.element.TextWindowElement;

public class TextInstruction extends FadeInOutInstruction {
   private final TextWindowElement element;
   private OutlinerElement outline;

   public TextInstruction(TextWindowElement element, int duration) {
      super(duration);
      this.element = element;
   }

   public TextInstruction(TextWindowElement element, int duration, Selection selection) {
      this(element, duration);
      this.outline = new OutlinerElement(o -> selection.makeOutline(o).lineWidth(0.0625F));
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      if (this.outline != null) {
         this.outline.setColor(this.element.getPalette().getColor());
      }
   }

   @Override
   protected void show(PonderScene scene) {
      scene.addElement(this.element);
      this.element.setVisible(true);
      if (this.outline != null) {
         scene.addElement(this.outline);
         this.outline.setFade(1.0F);
         this.outline.setVisible(true);
      }
   }

   @Override
   protected void hide(PonderScene scene) {
      this.element.setVisible(false);
      if (this.outline != null) {
         this.outline.setFade(0.0F);
         this.outline.setVisible(false);
      }
   }

   @Override
   protected void applyFade(PonderScene scene, float fade) {
      this.element.setFade(fade);
   }
}
