package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;

public class OutlineSelectionInstruction extends TickingInstruction {
   private final PonderPalette color;
   private final Object slot;
   private final Selection selection;

   public OutlineSelectionInstruction(PonderPalette color, Object slot, Selection selection, int ticks) {
      super(false, ticks);
      this.color = color;
      this.slot = slot;
      this.selection = selection;
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      this.selection.makeOutline(scene.getOutliner(), this.slot).lineWidth(0.0625F).colored(this.color.getColor());
   }
}
