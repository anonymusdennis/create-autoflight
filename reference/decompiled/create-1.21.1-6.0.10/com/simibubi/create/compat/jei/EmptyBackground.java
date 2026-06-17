package com.simibubi.create.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;

public class EmptyBackground implements IDrawable {
   private int width;
   private int height;

   public EmptyBackground(int width, int height) {
      this.width = width;
      this.height = height;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
   }
}
