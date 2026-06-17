package com.simibubi.create.compat.jei;

import com.simibubi.create.foundation.gui.AllGuiTextures;
import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class ScreenResourceWrapper implements IDrawable {
   private AllGuiTextures resource;

   public ScreenResourceWrapper(AllGuiTextures resource) {
      this.resource = resource;
   }

   public int getWidth() {
      return this.resource.getWidth();
   }

   public int getHeight() {
      return this.resource.getHeight();
   }

   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
      graphics.blit(
         this.resource.location,
         xOffset,
         yOffset,
         0,
         (float)this.resource.getStartX(),
         (float)this.resource.getStartY(),
         this.resource.getWidth(),
         this.resource.getHeight(),
         256,
         256
      );
   }
}
