package net.createmod.catnip.config.ui;

import net.createmod.catnip.gui.UIRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.CommonComponents;

public class HintableTextFieldWidget extends EditBox {
   protected Font font;
   protected String hint;

   public HintableTextFieldWidget(Font font, int x, int y, int width, int height) {
      super(font, x, y, width, height, CommonComponents.EMPTY);
      this.font = font;
   }

   public void setHint(String hint) {
      this.hint = hint;
   }

   public void setHeight(int value) {
      this.height = value;
   }

   public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWidget(graphics, mouseX, mouseY, partialTicks);
      if (this.hint != null && !this.hint.isEmpty()) {
         if (this.getValue().isEmpty()) {
            graphics.drawString(
               this.font, this.hint, this.getX() + 5, this.getY() + (this.height - 8) / 2, UIRenderHelper.COLOR_TEXT.getFirst().scaleAlpha(0.75F).getRGB()
            );
         }
      }
   }

   public boolean mouseClicked(double x, double y, int button) {
      if (!this.isMouseOver(x, y)) {
         return false;
      } else if (button == 1) {
         this.setValue("");
         return true;
      } else {
         return super.mouseClicked(x, y, button);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      return Minecraft.getInstance().options.keyInventory.matches(keyCode, scanCode) ? true : super.keyPressed(keyCode, scanCode, modifiers);
   }
}
