package net.createmod.ponder.api.element;

import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.gui.GuiGraphics;

public interface AnimatedOverlayElement extends PonderOverlayElement {
   void setFade(float var1);

   float getFade(float var1);

   @Override
   default void render(PonderScene scene, PonderUI screen, GuiGraphics graphics, float partialTicks) {
      this.render(scene, screen, graphics, partialTicks, this.getFade(partialTicks));
   }

   void render(PonderScene var1, PonderUI var2, GuiGraphics var3, float var4, float var5);
}
