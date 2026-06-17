package net.createmod.catnip.gui.element;

import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.gui.GuiGraphics;

public class DelegatedStencilElement extends AbstractRenderElement implements StencilElement {
   protected static final FadableScreenElement EMPTY_RENDERER = (graphics, width, height, alpha) -> {
   };
   protected static final FadableScreenElement DEFAULT_ELEMENT = (graphics, width, height, alpha) -> UIRenderHelper.angledGradient(
         graphics, 0.0F, -3, 5, (float)(height + 4), (float)(width + 6), new Color(-15672048).scaleAlpha(alpha), new Color(-15724323).scaleAlpha(alpha)
      );
   protected FadableScreenElement stencil;
   protected FadableScreenElement element;

   public DelegatedStencilElement() {
      this.stencil = EMPTY_RENDERER;
      this.element = DEFAULT_ELEMENT;
   }

   public DelegatedStencilElement(FadableScreenElement stencil, FadableScreenElement element) {
      this.stencil = stencil;
      this.element = element;
   }

   public <T extends DelegatedStencilElement> T withStencilRenderer(FadableScreenElement renderer) {
      this.stencil = renderer;
      return (T)this;
   }

   public <T extends DelegatedStencilElement> T withElementRenderer(FadableScreenElement renderer) {
      this.element = renderer;
      return (T)this;
   }

   @Override
   public void renderStencil(GuiGraphics graphics) {
      this.stencil.render(graphics, this.width, this.height, 1.0F);
   }

   @Override
   public void renderElement(GuiGraphics graphics) {
      this.element.render(graphics, this.width, this.height, this.alpha);
   }
}
