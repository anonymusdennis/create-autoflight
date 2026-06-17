package net.createmod.catnip.gui.element;

import net.minecraft.client.gui.GuiGraphics;

@FunctionalInterface
public interface FadableScreenElement extends ScreenElement {
   @Override
   default void render(GuiGraphics graphics, int x, int y) {
      this.render(graphics, x, y, 1.0F);
   }

   void render(GuiGraphics var1, int var2, int var3, float var4);
}
