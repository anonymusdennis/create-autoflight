package net.createmod.catnip.gui.element;

import net.minecraft.client.gui.GuiGraphics;

public interface RenderElement extends FadableScreenElement {
   static RenderElement of(ScreenElement renderable) {
      return new AbstractRenderElement.SimpleRenderElement(renderable);
   }

   <T extends RenderElement> T at(float var1, float var2);

   <T extends RenderElement> T at(float var1, float var2, float var3);

   <T extends RenderElement> T withBounds(int var1, int var2);

   <T extends RenderElement> T withAlpha(float var1);

   int getWidth();

   int getHeight();

   float getX();

   float getY();

   float getZ();

   void render(GuiGraphics var1);

   @Override
   default void render(GuiGraphics graphics, int x, int y, float alpha) {
      this.<RenderElement>at((float)x, (float)y).<RenderElement>withAlpha(alpha).render(graphics);
   }
}
