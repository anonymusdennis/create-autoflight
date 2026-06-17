package com.simibubi.create.foundation.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

public class ScreenOverlay extends CompositeWidget {
   public final int zOffset;

   public ScreenOverlay(int zOffset) {
      this.zOffset = zOffset;
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      graphics.pose().pushPose();
      graphics.pose().translate(0.0F, 0.0F, (float)this.zOffset);
      super.render(graphics, mouseX, mouseY, partialTicks);
      graphics.pose().popPose();
   }
}
