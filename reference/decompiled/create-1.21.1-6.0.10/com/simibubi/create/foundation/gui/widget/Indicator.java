package com.simibubi.create.foundation.gui.widget;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class Indicator extends AbstractSimiWidget {
   public Indicator.State state;

   public Indicator(int x, int y, Component tooltip) {
      super(x, y, AllGuiTextures.INDICATOR.getWidth(), AllGuiTextures.INDICATOR.getHeight());
      this.toolTip = this.toolTip.isEmpty() ? ImmutableList.of() : ImmutableList.of(tooltip);
      this.state = Indicator.State.OFF;
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.visible) {
         (switch (this.state) {
            case OFF -> AllGuiTextures.INDICATOR;
            case ON -> AllGuiTextures.INDICATOR_WHITE;
            case RED -> AllGuiTextures.INDICATOR_RED;
            case YELLOW -> AllGuiTextures.INDICATOR_YELLOW;
            case GREEN -> AllGuiTextures.INDICATOR_GREEN;
            default -> AllGuiTextures.INDICATOR;
         }).render(graphics, this.getX(), this.getY());
      }
   }

   public static enum State {
      OFF,
      ON,
      RED,
      YELLOW,
      GREEN;
   }
}
