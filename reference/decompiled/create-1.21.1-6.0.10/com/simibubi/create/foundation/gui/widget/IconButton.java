package com.simibubi.create.foundation.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.AllKeys;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.element.ScreenElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class IconButton extends AbstractSimiWidget {
   protected ScreenElement icon;
   public boolean green;

   public IconButton(int x, int y, ScreenElement icon) {
      this(x, y, 18, 18, icon);
   }

   public IconButton(int x, int y, int w, int h, ScreenElement icon) {
      super(x, y, w, h);
      this.icon = icon;
   }

   public void doRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
         this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;
         AllGuiTextures button = !this.active
            ? AllGuiTextures.BUTTON_DISABLED
            : (
               this.isHovered && AllKeys.isMouseButtonDown(0)
                  ? AllGuiTextures.BUTTON_DOWN
                  : (this.isHovered ? AllGuiTextures.BUTTON_HOVER : (this.green ? AllGuiTextures.BUTTON_GREEN : AllGuiTextures.BUTTON))
            );
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.drawBg(graphics, button);
         this.icon.render(graphics, this.getX() + 1, this.getY() + 1);
      }
   }

   protected void drawBg(GuiGraphics graphics, AllGuiTextures button) {
      graphics.blit(button.location, this.getX(), this.getY(), button.getStartX(), button.getStartY(), button.getWidth(), button.getHeight());
   }

   public void setToolTip(Component text) {
      this.toolTip.clear();
      this.toolTip.add(text);
   }

   public void setIcon(ScreenElement icon) {
      this.icon = icon;
   }
}
