package com.simibubi.create.foundation.gui.widget;

import java.util.List;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class TooltipArea extends AbstractSimiWidget {
   public TooltipArea(int x, int y, int width, int height) {
      super(x, y, width, height);
   }

   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
         this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
      }
   }

   public TooltipArea withTooltip(List<Component> tooltip) {
      this.toolTip = tooltip;
      return this;
   }
}
