package net.createmod.ponder.foundation.instruction;

import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.PonderScene;

public abstract class WorldModifyInstruction extends PonderInstruction {
   private final Selection selection;

   public WorldModifyInstruction(Selection selection) {
      this.selection = selection;
   }

   @Override
   public boolean isComplete() {
      return true;
   }

   @Override
   public void tick(PonderScene scene) {
      this.runModification(this.selection, scene);
      if (this.needsRedraw()) {
         scene.forEach(WorldSectionElement.class, WorldSectionElement::queueRedraw);
      }
   }

   protected abstract void runModification(Selection var1, PonderScene var2);

   protected abstract boolean needsRedraw();
}
